package au.urremote.bridge.service.thread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.Log;
import au.urremote.bridge.common.Constants;
import au.urremote.bridge.common.DefSettings;
import au.urremote.bridge.mapmymaps.MapMyTracksInterfaceApi;
import au.urremote.bridge.mapmymaps.MapMyMapsException;
import au.urremote.bridge.service.InternalServiceMessageHandler;
import au.urremote.bridge.service.Sample;
import au.urremote.bridge.service.SamplingQueue;
import au.urremote.bridge.service.utils.CustomThreadUncaughtExceptionHandler;

import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;

public class UploaderThread {
	
	private static final int NUM_HELPER_THREADS = 20;
	private static final long INITIAL_DATA_WAIT = 60*60*1000L;
	private static final long CONSEQUENT_DATA_WAIT = 20;
	
	private InternalServiceMessageHandler serviceMsgHandler;
	private SamplingQueue samplingQueue;
	private Context context;
	private ActivityUploader activityUploader;
	
	public UploaderThread(
			Context context,
			InternalServiceMessageHandler criticalErrorHandler,
			SamplingQueue samplingQueue
			) {
		this.context = context;
		this.serviceMsgHandler = criticalErrorHandler;
		this.samplingQueue = samplingQueue;
	}
	
	public void begin() throws Exception {
		this.activityUploader = new ActivityUploader();
		this.activityUploader.start();
	}
	
	public void quit() {
		if (this.activityUploader!=null) {
			this.activityUploader.quit();
			this.activityUploader = null;
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
		
		private MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		private List<Location> pointsToUpload = new ArrayList<Location>();
		private List<SensorDataSet> sensorDataToUpload = new ArrayList<SensorDataSet>();
		private Long activityId = null;
		public boolean isRunning = false;
		private ActivityUploaderHelper[] helpers = null;
		private Object stopSemaphore = new Object();
		
		public ActivityUploader() {
			super("ActivityUploader");
		}
		
		public void quit() {
			isRunning = false;
			synchronized (stopSemaphore) {
				stopSemaphore.notifyAll();
			}
		}
		
		@Override
		public void run() {
			CustomThreadUncaughtExceptionHandler.setInterceptHandler(Thread.currentThread());
			
			SharedPreferences state = context.getSharedPreferences(
					Constants.SHARE_PREF, Context.MODE_PRIVATE);
			this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
					DefSettings.getUsername(state),
					DefSettings.getPassword(state)
					);
			
			isRunning = true;
//			Sample sample = null;
			try {
				grabData(INITIAL_DATA_WAIT, pointsToUpload, sensorDataToUpload);
				Log.d(Constants.TAG, "UploaderThread: filled sample received");
				
				serviceMsgHandler.onSystemMessage("Attempting to start MapMyTracks Activity");
				activityId = null;
				while (isRunning && activityId==null) {
					try {
						activityId = 
							mapMyTracksInterfaceApi.startActivity(
								DefSettings.getActivityTitle(state),
								DefSettings.getTags(state),
								DefSettings.isPublic(state),
								DefSettings.getActivityType(state),
								pointsToUpload
							);
					} catch (java.net.UnknownHostException e) {
						//	ignore, sometimes happens because of poor connection
						//	try again after a short time
						//Thread.sleep(30000L);	don't use sleep, instead
						//						allow for the user to stop, ie. use the stop semaphore
						Log.d(Constants.TAG, "Error while starting MapMyTracks Activity", e);
						serviceMsgHandler.onSystemMessage("DNS Error while trying to start MapMyTracks Activity, retrying shortly");
						try {
							synchronized (stopSemaphore) {
								stopSemaphore.wait(30000L);	//	30s
							}
						} catch (InterruptedException ex) {
						}
					} catch (java.net.SocketException e) {
						//	ignore, sometimes happens because of poor connection
						//	try again after a short time
						//Thread.sleep(30000L);	don't use sleep, instead
						//						allow for the user to stop, ie. use the stop semaphore
						Log.d(Constants.TAG, "Error while starting MapMyTracks Activity", e);
						serviceMsgHandler.onSystemMessage("Network Socket Error while trying to start MapMyTracks Activity, retrying shortly");
						try {
							synchronized (stopSemaphore) {
								stopSemaphore.wait(30000L);	//	30s
							}
						} catch (InterruptedException ex) {
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
				
			} catch (InterruptedException e) {
				//ignore
			} catch (IOException e) {
				Log.e(Constants.TAG, "Error Connecting To Server", e);
				serviceMsgHandler.onCriticalError("Error Connecting To Server");
			} catch (MapMyMapsException e) {
				Log.e(Constants.TAG, "MapMyTracks Service Error", e);
				serviceMsgHandler.onCriticalError("MapMyTracks:"+e.getMessage());
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
				Log.d(Constants.TAG, "UploaderThread: waiting for stop signal");
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
				Log.d(Constants.TAG, "UploaderThread: posting activity stop");
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
		
		private CharSequence timestampToDate(long timestamp)
		{
			return DateFormat.format("kk:mm:ss", timestamp);
		}
		
		
		@Override
		public void run() {
			CustomThreadUncaughtExceptionHandler.setInterceptHandler(Thread.currentThread());
			
			SharedPreferences state = context.getSharedPreferences(
					Constants.SHARE_PREF, Context.MODE_PRIVATE);
			this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
					DefSettings.getUsername(state),
					DefSettings.getPassword(state)
					);
			
			boolean previousWasIOException = false;
			
			StringBuilder msgBuilder = new StringBuilder();
			
			while (uploaderHelper.isRunning) {
				try {
					if (pointsToUpload.isEmpty()) {
						grabData(CONSEQUENT_DATA_WAIT, pointsToUpload, sensorDataToUpload);
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
						previousWasIOException = false;
					} catch (MapMyMapsException e) {
						Log.e(Constants.TAG, "Helper "+helperIndex+": MapMyTracks Service Error", e);
						serviceMsgHandler.onSystemMessage("MapMyTracks-API:"+e.getMessage());
					} catch (IOException e) {
						if (!previousWasIOException) {
							Log.e(Constants.TAG, "Helper "+helperIndex+": Error Connecting To Server", e);
							serviceMsgHandler.onSystemMessage("Error Connecting To Server");
							previousWasIOException = true;
						}
						
						try {
							synchronized (uploaderHelper.stopSemaphore) {
								uploaderHelper.stopSemaphore.wait(1000L);	//	1s
							}
						} catch (InterruptedException ex) {
						}
					}
				} catch (InterruptedException e) {
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
