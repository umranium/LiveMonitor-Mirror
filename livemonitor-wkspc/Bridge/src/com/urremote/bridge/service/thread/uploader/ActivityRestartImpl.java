package com.urremote.bridge.service.thread.uploader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.Log;

import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.CustomThreadUncaughtExceptionHandler;
import com.urremote.bridge.common.DefSettings;
import com.urremote.bridge.mapmymaps.MapMyMapsException;
import com.urremote.bridge.mapmymaps.MapMyTracksInterfaceApi;
import com.urremote.bridge.service.InternalServiceMessageHandler;
import com.urremote.bridge.service.Sample;
import com.urremote.bridge.service.SamplingQueue;
import com.urremote.bridge.service.thread.UploaderThread;

public class ActivityRestartImpl implements UploaderThread {
	
	private static final int NUM_HELPER_THREADS = 20;
	private static final long INITIAL_DATA_WAIT = 60*60*1000L;
	private static final long CONSEQUENT_DATA_WAIT = 20;
	
	private InternalServiceMessageHandler serviceMsgHandler;
	private SamplingQueue samplingQueue;
	private Context context;
	private ActivityMaster activityUploader;
	
	public ActivityRestartImpl(
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
		this.activityUploader = new ActivityMaster();
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
	class ActivityMaster extends Thread {
		
		SharedPreferences preferences  = context.getSharedPreferences(
				Constants.SHARE_PREF, Context.MODE_PRIVATE);
		MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		List<Location> pointsToUpload = new ArrayList<Location>();
		List<SensorDataSet> sensorDataToUpload = new ArrayList<SensorDataSet>();
		Long activityId = null;
		boolean isRunning = false;
		SlaveGroupState slaveGroupState = null;
		UploadSlave[] currentSlaves = null;
		int numberOfRunninngSlaves = 0;
		Object stopSemaphore = new Object();
		
		public ActivityMaster() {
			super("ActivityMaster");
		}
		
		public void quit() {
			Log.d(Constants.TAG, "Uploader thread quiting");
			isRunning = false;
			synchronized (stopSemaphore) {
				stopSemaphore.notifyAll();
			}
			this.interrupt();
			if (currentSlaves!=null) {
				for (UploadSlave slave:currentSlaves) {
					slave.mapMyTracksInterfaceApi.shutdown();
					slave.interrupt();
				}
			}
			Log.d(Constants.TAG, "Waiting for uploader thread to finish");
			while (this.isAlive()) {
				Thread.yield();
			}
		}
		
		private void startActivity() {
			activityId = null;
			while (isRunning && activityId==null) {
				try {
					activityId = 
						mapMyTracksInterfaceApi.startActivity(
							DefSettings.getActivityTitle(preferences),
							DefSettings.getTags(preferences),
							DefSettings.isPublic(preferences),
							DefSettings.getActivityType(preferences),
							pointsToUpload
						);
				} catch (Exception e) {
					//	ignore, sometimes happens because of poor connection
					//	try again after a short time
					//  don't use sleep, instead allow for the user to stop, ie. use the stop semaphore
					delay("Error while starting MapMyTracks Activity", e, Constants.INTERVAL_RETRY_UPLOAD);
				}
			}
		}
		
		private void spawnHelpers() {
			pointsToUpload.clear();
			
			slaveGroupState = new SlaveGroupState(this);
			currentSlaves = new UploadSlave[NUM_HELPER_THREADS];
			for (int i=0; i<NUM_HELPER_THREADS; ++i) {
				if (!sensorDataToUpload.isEmpty()) {
					currentSlaves[i] = new UploadSlave(i, this, slaveGroupState, sensorDataToUpload);
					sensorDataToUpload.clear();
				} else {
					currentSlaves[i] = new UploadSlave(i, this, slaveGroupState, null);
				}
			}
			for (int i=0; i<NUM_HELPER_THREADS; ++i) {
				currentSlaves[i].start();
			}
			while (numberOfRunninngSlaves==0) {
				Thread.yield();
			}
		}
		
		private void delay(String msg, Throwable e, long time) {
			if (msg!=null) {
				StringBuilder sysMsg = new StringBuilder();
				sysMsg.append(msg);
				if (e!=null) {
					Log.d(Constants.TAG, msg, e);
					sysMsg.append("\nError: "+e.getMessage());
				}
				else {
					Log.d(Constants.TAG, msg);
					sysMsg.append("\n"+msg);
				}
				sysMsg.append("\nRetrying in "+(time/1000)+"s");
				serviceMsgHandler.onSystemMessage(sysMsg.toString());
			}
			try {
				synchronized (stopSemaphore) {
					stopSemaphore.wait(time);
				}
			} catch (InterruptedException ex) {
				//	ignore
			}
		}
		
		@Override
		public void run() {
			CustomThreadUncaughtExceptionHandler.setInterceptHandler(Thread.currentThread());
			
			this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
					-1,
					DefSettings.getUsername(preferences),
					DefSettings.getPassword(preferences)
					);
			
			isRunning = true;
			
			try {
				while (isRunning) {
					try {
						if (pointsToUpload.isEmpty()) {
							grabData(INITIAL_DATA_WAIT, pointsToUpload, sensorDataToUpload);
						}
					} catch (InterruptedException e) {
						continue;
					}
					
					startActivity();
					
					if (!isRunning) break;
					if (activityId==null) {
						delay("Unable to start activity", null, Constants.INTERVAL_RETRY_UPLOAD);
						continue;
					}
					
					serviceMsgHandler.onSystemMessage("MapMyTracks Activity Started");
					
					spawnHelpers();
					
					//	wait for all currentSlaves to return
					while (numberOfRunninngSlaves>0) {
						Thread.yield();
					}
					
					if (isRunning) {
						pointsToUpload.addAll(slaveGroupState.pointsUploadedSinceException);
						sensorDataToUpload.addAll(slaveGroupState.sensorDataUploadedSinceException);
					}
				}
			} finally {
				while (samplingQueue.getPendingFilledInstances()>0) {
					try {
						samplingQueue.returnEmptyInstance(samplingQueue.takeFilledInstance());
					} catch (InterruptedException e) {
					}
				}

				//	post stop activity request
				if (activityId!=null) {
					Log.d(Constants.TAG, "NoActivityRestartImpl: posting activity stop");
					try {
						mapMyTracksInterfaceApi.stopActivity();
						serviceMsgHandler.onSystemMessage("MapMyTracks Activity Stopped");
					} catch (IOException e) {
					} catch (MapMyMapsException e) {
					}
				}
			}
			
			Log.d(Constants.TAG, "Uploader Thread stopped");
		}
		
	}
	
	class SlaveGroupState {
		ActivityMaster uploadMaster;
		
		Class<?> prevExceptionClass = null;
		
		boolean recoveringFromError = false;
		long timeLastErrorThrown = 0;
		
		List<Location> pointsUploadedSinceException = new ArrayList<Location>();
		List<SensorDataSet> sensorDataUploadedSinceException = new ArrayList<SensorDataSet>();
		
		boolean activityWasStopped = false;
		
		public SlaveGroupState(ActivityMaster uploadMaster) {
			this.uploadMaster = uploadMaster;
		}
		
		void handleException(Throwable e) {
			boolean displayError = false;
			
			synchronized (this) {
				if (prevExceptionClass==null || !e.getClass().equals(prevExceptionClass)) {
					prevExceptionClass = e.getClass();
					recoveringFromError = false;
					displayError = true;
				}
			}
			
			if (displayError)
				uploadMaster.delay("Error updating server", e, Constants.INTERVAL_RETRY_UPLOAD);
			else
				uploadMaster.delay(null, null, Constants.INTERVAL_RETRY_UPLOAD);
		}
		
		void onSuccessUpdate() {
			synchronized (this) {
				if (prevExceptionClass!=null) {
					recoveringFromError = true;
					timeLastErrorThrown = System.currentTimeMillis();
					prevExceptionClass = null;
				}
			}
		}
		
		void checkRecovery(MapMyTracksInterfaceApi mapMyTracksInterfaceApi) throws IOException, MapMyMapsException {
			long time = System.currentTimeMillis();
			if (time-timeLastErrorThrown>Constants.DURATION_WAIT_FOR_STABLE_CONNECTION) {
				synchronized (this) {
					if (recoveringFromError) {
						if (mapMyTracksInterfaceApi.isActivityRecording(uploadMaster.activityId)) {
							pointsUploadedSinceException.clear();
							sensorDataUploadedSinceException.clear();
						} else {
							activityWasStopped = true;
						}
						recoveringFromError = false;
					}
				}
			}
		}
	}
	
	class UploadSlave extends Thread {
		SharedPreferences preferences  = context.getSharedPreferences(
				Constants.SHARE_PREF, Context.MODE_PRIVATE);;
		int helperIndex;
		ActivityMaster uploadMaster;
		SlaveGroupState slaveGroupState;
		MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		List<Location> pointsToUpload = new ArrayList<Location>();
		List<SensorDataSet> sensorDataToUpload = new ArrayList<SensorDataSet>();
		
		public UploadSlave(
				int helperIndex,
				ActivityMaster uploadMaster,
				SlaveGroupState slaveGroupState,
				List<SensorDataSet> sensorDataToUpload) {
			this.helperIndex = helperIndex;
			this.uploadMaster = uploadMaster;
			this.slaveGroupState = slaveGroupState;
			if (sensorDataToUpload!=null) {
				this.sensorDataToUpload.addAll(sensorDataToUpload);
			}
		}
		
		private CharSequence timestampToDate(long timestamp)
		{
			return DateFormat.format("kk:mm:ss", timestamp);
		}
		
		private boolean canRun() {
			return uploadMaster.isRunning && !slaveGroupState.activityWasStopped;
		}
		
		
		@Override
		public void run() {
			CustomThreadUncaughtExceptionHandler.setInterceptHandler(Thread.currentThread());
			
			++uploadMaster.numberOfRunninngSlaves;
			try {
				this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
						helperIndex,
						DefSettings.getUsername(preferences),
						DefSettings.getPassword(preferences)
						);
				
				StringBuilder msgBuilder = new StringBuilder();
				
				while (canRun()) {
					if (pointsToUpload.isEmpty()) {
						try {
							grabData(CONSEQUENT_DATA_WAIT, pointsToUpload, sensorDataToUpload);
						} catch (InterruptedException e) {
							continue;
						}
					}
						
					try {
						if (slaveGroupState.recoveringFromError) {
							slaveGroupState.checkRecovery(mapMyTracksInterfaceApi);
							continue; // check recovery might have changed state of shouldStop()
						}
						
						mapMyTracksInterfaceApi.updateActivity(
								uploadMaster.activityId,
								pointsToUpload,
								sensorDataToUpload
								);
						
						if (slaveGroupState.recoveringFromError) {
							slaveGroupState.pointsUploadedSinceException.addAll(pointsToUpload);
							slaveGroupState.sensorDataUploadedSinceException.addAll(sensorDataToUpload);
						}
						
						Location lp = pointsToUpload.get(pointsToUpload.size()-1);
						
						Log.d(Constants.TAG, "Helper Index: "+helperIndex+": Uploaded "+lp.getTime());
						
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
						
						if (slaveGroupState.prevExceptionClass!=null) {
							slaveGroupState.onSuccessUpdate();
						}
					} catch (Exception e) {
						slaveGroupState.handleException(e);
					}
				}
				
			} finally {
				if (mapMyTracksInterfaceApi!=null) {
					mapMyTracksInterfaceApi.shutdown();
				}
				if (!pointsToUpload.isEmpty()) {
					uploadMaster.pointsToUpload.addAll(pointsToUpload);
				}
				if (!sensorDataToUpload.isEmpty()) {
					uploadMaster.sensorDataToUpload.addAll(sensorDataToUpload);
				}
				Log.d(Constants.TAG, "Helper "+helperIndex+": Exiting, isRunning="+uploadMaster.isRunning+", activity stopped="+slaveGroupState.activityWasStopped);
				--uploadMaster.numberOfRunninngSlaves;
			}

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
