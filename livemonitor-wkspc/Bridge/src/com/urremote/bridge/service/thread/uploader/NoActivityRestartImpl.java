package com.urremote.bridge.service.thread.uploader;

import java.io.IOException;
import java.security.acl.LastOwnerException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.urremote.bridge.LatencyTestThread;
import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.CustomUncaughtExceptionHandler;
import com.urremote.bridge.common.DefSettings;
import com.urremote.bridge.mapmymaps.ActivityDetails;
import com.urremote.bridge.mapmymaps.ActivityType;
import com.urremote.bridge.mapmymaps.MapMyMapsException;
import com.urremote.bridge.mapmymaps.MapMyTracksInterfaceApi;
import com.urremote.bridge.mapmymaps.UnparsableReplyException;
import com.urremote.bridge.service.InternalServiceMessageHandler;
import com.urremote.bridge.service.Sample;
import com.urremote.bridge.service.SamplingQueue;
import com.urremote.bridge.service.thread.UploaderThread;

public class NoActivityRestartImpl implements UploaderThread {
	
	private static final int NUM_HELPER_THREADS = 5;
	private static final long DATA_WAIT = 30*1000L;
	private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm:ss"); 
	
	private InternalServiceMessageHandler serviceMsgHandler;
	private SamplingQueue samplingQueue;
	private Context context;
	private ActivityUploader activityUploader;
	
	public NoActivityRestartImpl(
			Context context,
			InternalServiceMessageHandler criticalErrorHandler,
			SamplingQueue samplingQueue
			) {
		this.context = context;
		this.serviceMsgHandler = criticalErrorHandler;
		this.samplingQueue = samplingQueue;
	}
	
	/* (non-Javadoc)
	 * @see com.urremote.bridge.service.thread.UploaderThread#begin()
	 */
	@Override
	public void begin() throws Exception {
		this.activityUploader = new ActivityUploader();
		this.activityUploader.start();
	}
	
	/* (non-Javadoc)
	 * @see com.urremote.bridge.service.thread.UploaderThread#quit()
	 */
	@Override
	public void quit() {
		if (this.activityUploader!=null) {
			this.activityUploader.quit();
			this.activityUploader = null;
		}
	}
	
	private CharSequence timestampToDate(long timestamp)
	{
		return TIMESTAMP_FORMAT.format(new Date(timestamp));
	}	
	
	/**
	 * The master thread for uploading an activity.
	 * 
	 *  This master thread is responsible for:
	 *  	1) Starting the activity
	 *  	2) Creating the helper threads to update the activity
	 *  	3) Interrupting the helper threads when the activity is done,
	 *  			hence closing their connections.
	 *  	4) Since it's connection will still be alive, it
	 *  			can post the 'stop activity' request.
	 */
	class ActivityUploader extends Thread {
		
		private MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		private List<Location> pointsToUpload = new ArrayList<Location>();
		private List<SensorDataSet> sensorDataToUpload = new ArrayList<SensorDataSet>();
		private Long activityId = null;
		public boolean isRunning = false;
		private ActivityUploaderHelper[] helpers = null;
		private Object stopSemaphore = new Object();
		
		
		public ActivityUploader() {
			super("ActivityMaster");
		}
		
		public void quit() {
			Log.d(Constants.TAG, "Uploader thread quiting");
			isRunning = false;
			synchronized (stopSemaphore) {
				stopSemaphore.notifyAll();
			}
		}
		
		public ActivityDetails queryLatestActivity() throws IOException, MapMyMapsException {
			SharedPreferences state = context.getSharedPreferences(
					Constants.SHARE_PREF, Context.MODE_PRIVATE);
			List<ActivityDetails> activities = mapMyTracksInterfaceApi.getActivities(DefSettings.getUsername(state));
			ActivityDetails latestActivity = null;
			for (ActivityDetails details:activities) {
//				Log.d(Constants.TAG,"Activity: "+details.id+":"+details.timestamp+": "+details.name);
				if (latestActivity==null || latestActivity.timestamp<details.timestamp) {
					latestActivity = details;
				}
			}
			return latestActivity;
		}
		
		@Override
		public void run() {
			CustomUncaughtExceptionHandler.setInterceptHandler(context, Thread.currentThread());
			
			SharedPreferences state = context.getSharedPreferences(
					Constants.SHARE_PREF, Context.MODE_PRIVATE);
			this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
					-1,
					DefSettings.getUsername(state),
					DefSettings.getPassword(state)
					);
			
			isRunning = true;
			try {
				while (isRunning && pointsToUpload.isEmpty()) {
					try {
						grabData(DATA_WAIT, pointsToUpload, sensorDataToUpload);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				Log.d(Constants.TAG, "Uploader: filled sample received");
				
				serviceMsgHandler.onSystemMessage("Attempting to start MapMyTracks Activity");
				activityId = null;
				Class<?> prevExceptionClass = null;
				long latestStartAttemptTime = 0;
				while (isRunning && activityId==null) {
					try {
						if (latestStartAttemptTime>0) {
							Log.d(Constants.TAG, "Query latest activity from server");
							ActivityDetails latestActivity = queryLatestActivity();
							if (latestActivity!=null) {
								Log.d(Constants.TAG, "Latest activity was at: "+latestActivity.timestamp+", latest attempt was at: "+latestStartAttemptTime);
								if (latestActivity.timestamp>=(latestStartAttemptTime/1000L)*1000L) {
									Log.d(Constants.TAG, "Continuing with activity");
									activityId = latestActivity.id;
								} else {
									Log.d(Constants.TAG, "Activity is too old");
									latestStartAttemptTime = 0;
								}
							} else {
								Log.d(Constants.TAG, "No activities returned");
							}
						} else {
							latestStartAttemptTime = mapMyTracksInterfaceApi.getServerTime();
							Log.d(Constants.TAG, "Attempting to start new activity, time="+latestStartAttemptTime);
							activityId = 
									mapMyTracksInterfaceApi.startActivity(
										DefSettings.getActivityTitle(state),
										DefSettings.compileTags(state),
										DefSettings.isPublic(state),
										DefSettings.getActivityType(state),
										pointsToUpload
								);
						}
					} catch (Exception e) {
						Log.d(Constants.TAG, "Error while starting MapMyTracks Activity", e);
						if (prevExceptionClass==null || !prevExceptionClass.equals(e.getClass())) {
							serviceMsgHandler.onSystemMessage("Error starting activity: "+e.getMessage()+", retrying shortly");
							prevExceptionClass = e.getClass();
						}
						
						try {
							synchronized (stopSemaphore) {
								stopSemaphore.wait(Constants.INTERVAL_RETRY_UPLOAD);	//	60s
							}
						} catch (InterruptedException ex) {
							//	ignore
						}
					}
				}
				
				if (isRunning && activityId!=null) {
					serviceMsgHandler.onSystemMessage("MapMyTracks Activity Started");
					
					pointsToUpload.clear();
					
					helpers = new ActivityUploaderHelper[NUM_HELPER_THREADS];
					for (int i=0; i<NUM_HELPER_THREADS; ++i) {
						if (!sensorDataToUpload.isEmpty()) {
							helpers[i] = new ActivityUploaderHelper(i, this, sensorDataToUpload);
							sensorDataToUpload.clear();
						} else {
							helpers[i] = new ActivityUploaderHelper(i, this, null);
						}
					}
					for (int i=0; i<NUM_HELPER_THREADS; ++i) {
						helpers[i].start();
					}
				}
				
			} finally {
//				if (sample!=null) {
//					try {
//						samplingQueue.returnEmptyInstance(sample);
//					} catch (InterruptedException e) {
//						Log.e(Constants.TAG, "Error", e);
//					}
//				}
			}
			
			if (isRunning) {
				Log.d(Constants.TAG, "Uploader: waiting for stop signal");
				try {
					synchronized (stopSemaphore) {
						stopSemaphore.wait();
					}
				} catch (InterruptedException e) {
				}
				//	interrupt helper functions,
				//		isRunning should have been set to false by now
				for (int i=0; i<NUM_HELPER_THREADS; ++i) {
					helpers[i].interrupt();
				}
			}
			
			while (samplingQueue.getPendingFilledInstances()>0) {
				try {
					samplingQueue.returnEmptyInstance(samplingQueue.takeFilledInstance());
				} catch (InterruptedException e) {
				}
			}
			
			//	post stop activity request
			if (activityId!=null) {
				Log.d(Constants.TAG, "Uploader: posting activity stop");
				try {
					mapMyTracksInterfaceApi.stopActivity();
					serviceMsgHandler.onSystemMessage("MapMyTracks Activity Stopped");
				} catch (IOException e) {
				} catch (MapMyMapsException e) {
				}
			}
			
			Log.d(Constants.TAG, "Uploader Thread stopped");
		}
		
	}
	
	private Class<?> prevExceptionClass = null;
	private final Object prevExceptionMutex = new Object();
	
	class ActivityUploaderHelper extends Thread {
		
		private int helperIndex;
		private ActivityUploader uploaderHelper;
		private MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		private List<Location> pointsToUpload = new ArrayList<Location>();
		private List<SensorDataSet> sensorDataToUpload = new ArrayList<SensorDataSet>();
		
		public ActivityUploaderHelper(
				int helperIndex,
				ActivityUploader uploaderHelper,
				List<SensorDataSet> sensorDataToUpload) {
			this.helperIndex = helperIndex;
			this.uploaderHelper = uploaderHelper;
			if (sensorDataToUpload!=null) {
				this.sensorDataToUpload.addAll(sensorDataToUpload);
			}
		}
		
		@Override
		public void run() {
			CustomUncaughtExceptionHandler.setInterceptHandler(context, Thread.currentThread());
			
			SharedPreferences state = context.getSharedPreferences(
					Constants.SHARE_PREF, Context.MODE_PRIVATE);
			this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
					helperIndex,
					DefSettings.getUsername(state),
					DefSettings.getPassword(state)
					);
			
			StringBuilder msgBuilder = new StringBuilder();
			
			while (uploaderHelper.isRunning) {
				try {
					if (pointsToUpload.isEmpty()) {
						grabData(DATA_WAIT, pointsToUpload, sensorDataToUpload);
					}
					
					try {
						mapMyTracksInterfaceApi.updateActivity(
								uploaderHelper.activityId,
								pointsToUpload,
								sensorDataToUpload
								);
						
						Location lp = pointsToUpload.get(pointsToUpload.size()-1);
						
						Log.d(Constants.TAG, "Helper Index: "+helperIndex+": Uploading "+lp.getTime());
						
						msgBuilder.append("Uploaded: "+timestampToDate(lp.getTime()));
						if (!sensorDataToUpload.isEmpty()) {
							SensorDataSet sd = sensorDataToUpload.get(0); 
							msgBuilder.append(": HR=").append(!sd.hasHeartRate()?"null":String.format("%02d", sd.getHeartRate().getValue()))
									.append(", CAD=").append(!sd.hasCadence()?"null":String.format("%02d", sd.getCadence().getValue()))
									.append(", POW=").append(!sd.hasPower()?"null":String.format("%02d", sd.getPower().getValue()));
							for (int l=sensorDataToUpload.size(), i=1; i<l; ++i) {
								sd = sensorDataToUpload.get(i);
								msgBuilder.append("\n\tHR=").append(!sd.hasHeartRate()?"null":String.format("%02d", sd.getHeartRate().getValue()))
										.append(", CAD=").append(!sd.hasCadence()?"null":String.format("%02d", sd.getCadence().getValue()))
										.append(", POW=").append(!sd.hasPower()?"null":String.format("%02d", sd.getPower().getValue()));
							}
						}
						serviceMsgHandler.onSystemMessage(msgBuilder.toString());
						msgBuilder.setLength(0);
						
						pointsToUpload.clear();
						sensorDataToUpload.clear();
						
						prevExceptionClass = null;
					} catch (Exception e) {
						synchronized (prevExceptionMutex) {
							if (prevExceptionClass==null || !prevExceptionClass.equals(e.getClass())) {
								Log.e(Constants.TAG, "Error Updating Server", e);
								serviceMsgHandler.onSystemMessage("Error Updating Server: "+e.getMessage()+"\nRetrying shortly");
								prevExceptionClass = e.getClass();
							}
						}
						
						try {
							synchronized (uploaderHelper.stopSemaphore) {
								uploaderHelper.stopSemaphore.wait(Constants.INTERVAL_RETRY_UPLOAD);	//	60s
							}
						} catch (InterruptedException ex) {
						}
					}
				} catch (Exception e) {
					Log.d(Constants.TAG, "Helper interrupted while fetching data", e);
					// ignored
				}
			}
			
			mapMyTracksInterfaceApi.shutdown();
			Log.d(Constants.TAG, "Helper "+helperIndex+": Exiting");		
		}

	}
	
	private void addData(List<Location> pointsToUpload, List<SensorDataSet> sensorDataToUpload, Sample sample)
	{
		pointsToUpload.add(sample.location);
		sensorDataToUpload.addAll(sample.sensorData);
		sample.sensorData.clear();
	}
	
	private void grabData(long waitDuration, List<Location> pointsToUpload, List<SensorDataSet> sensorDataToUpload) throws InterruptedException
	{
		Sample sample = null;
		try {
			sample = samplingQueue.takeFilledInstance(waitDuration);
			addData(pointsToUpload, sensorDataToUpload, sample);
			samplingQueue.returnEmptyInstance(sample);
			sample = null;
//			while (true) {
//				sample = samplingQueue.takeFilledInstanceIfAvail();
//				if (sample!=null) {
//					addData(pointsToUpload, sensorDataToUpload, sample);
//					samplingQueue.returnEmptyInstance(sample);
//					sample = null;
//				} else {
//					break;
//				}
//			}
		} finally {
			if (sample!=null) {
				try {
					samplingQueue.returnEmptyInstance(sample);
				} catch (InterruptedException e) {
					Log.e(Constants.TAG, "Error", e);
				}
			}
		}
	}
	
	
}
