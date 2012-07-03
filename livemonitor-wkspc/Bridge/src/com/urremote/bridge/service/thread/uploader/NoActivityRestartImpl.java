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
import com.urremote.bridge.common.PhoneInfo;
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
	private PhoneInfo phoneInfo;
	
	public NoActivityRestartImpl(
			Context context,
			InternalServiceMessageHandler criticalErrorHandler,
			SamplingQueue samplingQueue
			) {
		this.context = context;
		this.serviceMsgHandler = criticalErrorHandler;
		this.samplingQueue = samplingQueue;
		this.phoneInfo = new PhoneInfo(context);
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
	
	
	class ConnectionFaultController {
		final Object prevExceptionMutex = new Object();
		final Object stopSemaphore = new Object();
		final Object attemptConnectionSemaphore = new Object();
		
		ActivityUploader activityUploader;
		Class<?> prevExceptionClass = null;
		long lastConnectionAttemptTime = 0;
		
		public ConnectionFaultController(ActivityUploader activityUploader) {
			this.activityUploader = activityUploader;
		}
		
		/**
		 * Returns whether or not an attempt should be made to connect (after a connection error occurs)
		 */
		public boolean attemptConnection(int helperIndex) {
			boolean logResult = false;
			try {
				if (prevExceptionClass==null ||
						System.currentTimeMillis()-lastConnectionAttemptTime>Constants.INTERVAL_RETRY_UPLOAD) {
					synchronized (attemptConnectionSemaphore) {
						long now = System.currentTimeMillis();
//						Log.d(Constants.TAG, "Update Helper "+helperIndex+
//								": attemptConnection: (prevExceptionClass==null)="+
//								(prevExceptionClass==null)+
//								" (now-lastConnectionAttemptTime>Constants.INTERVAL_RETRY_UPLOAD)="+
//								(now-lastConnectionAttemptTime>Constants.INTERVAL_RETRY_UPLOAD));
						if (prevExceptionClass==null ||
								now-lastConnectionAttemptTime>Constants.INTERVAL_RETRY_UPLOAD) {
							lastConnectionAttemptTime = now;
							return logResult=true;
						} else {
							return logResult=false;
						}
					}
				} else {
					return logResult=false;
				}
			} finally {
//				Log.d(Constants.TAG, "Helper Index: "+helperIndex+": attemptConnection = "+logResult);
			}
		}
		
		public void clearPrevException(int helperIndex) {
			synchronized (prevExceptionMutex) {
				if (prevExceptionClass!=null) {
					prevExceptionClass = null;
					
//					Log.d(Constants.TAG, "Helper Index: "+helperIndex+
//							": cleared exception");
				}
			}
		}
		
		public void displayPrevException(String msg, Exception e) {
			synchronized (prevExceptionMutex) {
				if (prevExceptionClass==null || !prevExceptionClass.equals(e.getClass())) {
					Log.e(Constants.TAG, msg, e);
					serviceMsgHandler.onSystemMessage(msg+": "+e.getMessage()+"\nRetrying shortly");
					prevExceptionClass = e.getClass();
				}
			}
		}
		
		public void doMainStop() {
			try {
				synchronized (stopSemaphore) {
					stopSemaphore.wait();
				}
			} catch (InterruptedException ex) {
				//	ignore
			}
		}
		
		public void doUpdateStop(int updaterIndex) {
			try {
				long startTime = System.currentTimeMillis();
				
				//	wait until either uploading stops, or the exception is cleared, or for the required duration 
				while (activityUploader.isRunning &&
						System.currentTimeMillis()-startTime<Constants.INTERVAL_RETRY_UPLOAD &&
						prevExceptionClass!=null) {
					synchronized (stopSemaphore) {
						stopSemaphore.wait(1000L);
					}
					
//					Log.d(Constants.TAG, "Helper Index: "+updaterIndex+
//							": woke up to check status. Time-ready?="+
//							(!(System.currentTimeMillis()-startTime<Constants.INTERVAL_RETRY_UPLOAD))+
//							", prev-Ex-cleared?="+(!(prevExceptionClass!=null))
//							);
				}
			} catch (InterruptedException ex) {
				//	ignore
			}
		}
		
		public void notifyStop() {
			synchronized (stopSemaphore) {
				stopSemaphore.notifyAll();
			}
		}
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
		
		private String uniqueToken;
		private MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		private List<Location> pointsToUpload = new ArrayList<Location>();
		private List<SensorDataSet> sensorDataToUpload = new ArrayList<SensorDataSet>();
		private Long activityId = null;
		public boolean isRunning = false;
		private ActivityUploaderHelper[] helpers = null;
		private ConnectionFaultController controller;
		
		public ActivityUploader() {
			super("ActivityMaster");
			
			uniqueToken = phoneInfo.getIMEI()+":"+Long.toHexString(System.currentTimeMillis());
			controller = new ConnectionFaultController(this);
		}
		
		public void quit() {
			Log.d(Constants.TAG, "Uploader thread quiting");
			isRunning = false;
			controller.notifyStop();
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
			
			String activityTitle = DefSettings.compileActivityTitle(state);
			String activityTags = DefSettings.compileTags(state);
			boolean isPublic = DefSettings.isPublic(state);
			ActivityType activityType = DefSettings.getActivityType(state);
			
			isRunning = true;
			try {
				while (isRunning && pointsToUpload.isEmpty()) {
					try {
						grabData(DATA_WAIT, pointsToUpload, sensorDataToUpload, true);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				Log.d(Constants.TAG, "Uploader: filled sample received");
				
				serviceMsgHandler.onSystemMessage("Attempting to start MapMyTracks Activity");
				activityId = null;
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
										activityTitle,
										activityTags,
										isPublic,
										activityType,
										pointsToUpload,
										sensorDataToUpload,
										uniqueToken
								);
						}
						controller.clearPrevException(-1);
					} catch (Exception e) {
						controller.displayPrevException("Error while starting MapMyTracks Activity", e);
						controller.doUpdateStop(-1);
					}
				}
				
				if (isRunning && activityId!=null) {
					serviceMsgHandler.onSystemMessage("MapMyTracks Activity Started");
					
					pointsToUpload.clear();
					sensorDataToUpload.clear();
					
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
			
			Log.d(Constants.TAG, "Uploader: waiting for stop signal");
			while (isRunning) {
				controller.doMainStop();
			}
			
			if (helpers!=null) {
				//	interrupt helper functions,
				//		isRunning should have been set to false by now
				for (int i=0; i<NUM_HELPER_THREADS; ++i) {
					if (helpers[i]!=null)
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
	
	class ActivityUploaderHelper extends Thread {
		
		private int helperIndex;
		private ActivityUploader uploader;
		private MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		private List<Location> pointsToUpload = new ArrayList<Location>();
		private List<SensorDataSet> sensorDataToUpload = new ArrayList<SensorDataSet>();
		
		public ActivityUploaderHelper(
				int helperIndex,
				ActivityUploader uploader,
				List<SensorDataSet> sensorDataToUpload) {
			this.helperIndex = helperIndex;
			this.uploader = uploader;
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
			
			while (uploader.isRunning) {
				try {
					if (pointsToUpload.isEmpty()) {
						grabData(DATA_WAIT, pointsToUpload, sensorDataToUpload, true);
					}
					while (pointsToUpload.size()<Constants.DATAPOINTS_UPLOADED_PER_BATCH) {
						if (!grabData(DATA_WAIT, pointsToUpload, sensorDataToUpload, false))
							break;
					}
					
					try {
						Log.d(Constants.TAG, "Helper Index: "+helperIndex+": Attempting activity update.");
						mapMyTracksInterfaceApi.updateActivity(
								uploader.activityId,
								pointsToUpload,
								sensorDataToUpload
								);
						
						{
							boolean first = true;
							for (Location lp:pointsToUpload) {
								Log.d(Constants.TAG, "Helper Index: "+helperIndex+": Uploaded "+lp.getTime());
								if (first)
									first = false;
								else
									msgBuilder.append("\n");
								msgBuilder.append("Uploaded: "+timestampToDate(lp.getTime()));
							}
						}
						
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
						
						uploader.controller.clearPrevException(helperIndex);
					} catch (Exception e) {
						uploader.controller.displayPrevException("Error Updating Server", e);
						while (uploader.isRunning && !uploader.controller.attemptConnection(helperIndex)) {
							uploader.controller.doUpdateStop(helperIndex);
						}
					}
				} catch (Exception e) {
					Log.d(Constants.TAG, "Helper "+helperIndex+" interrupted while fetching data", e);
					// ignored
				}
			}
			
			mapMyTracksInterfaceApi.shutdown();
			Log.d(Constants.TAG, "Helper Index "+helperIndex+": Exiting");		
		}

	}
	
	private void addData(List<Location> pointsToUpload, List<SensorDataSet> sensorDataToUpload, Sample sample)
	{
		pointsToUpload.add(sample.location);
		sensorDataToUpload.addAll(sample.sensorData);
		sample.sensorData.clear();
	}
	
	private boolean grabData(long waitDuration, List<Location> pointsToUpload, List<SensorDataSet> sensorDataToUpload, boolean wait) throws InterruptedException
	{
		Sample sample = null;
		try {
			if (wait)
				sample = samplingQueue.takeFilledInstance(waitDuration);
			else
				sample = samplingQueue.takeFilledInstanceIfAvail();
			boolean gotSample = sample!=null;
			if (gotSample) {
				addData(pointsToUpload, sensorDataToUpload, sample);
				samplingQueue.returnEmptyInstance(sample);
			}
			sample = null;
			return gotSample;
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
