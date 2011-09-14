package au.csiro.livemonitor.service.thread;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.format.DateFormat;
import android.util.Log;
import au.csiro.livemonitor.common.*;
import au.csiro.livemonitor.mapmymaps.LocationPoint;
import au.csiro.livemonitor.mapmymaps.MapMyMapsException;
import au.csiro.livemonitor.mapmymaps.MapMyTracksInterfaceApi;
import au.csiro.livemonitor.mapmymaps.SensorData;
import au.csiro.livemonitor.service.Sample;
import au.csiro.livemonitor.service.SamplingQueue;
import au.csiro.livemonitor.service.InternalServiceMessageHandler;
import au.csiro.livemonitor.service.UpdateListenerCollection;

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.protobuf.InvalidProtocolBufferException;

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
		private List<LocationPoint> pointsToUpload = new ArrayList<LocationPoint>();
		private List<SensorData> sensorDataToUpload = new ArrayList<SensorData>();
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
			SharedPreferences state = context.getSharedPreferences(
					Constants.SHARE_PREF, Context.MODE_PRIVATE);
			this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
					DefSettings.getUsername(state),
					DefSettings.getPassword(state)
					);
			
			Sample sample = null;
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
						try {
							synchronized (stopSemaphore) {
								stopSemaphore.wait(30000L);	//	30s
							}
						} catch (InterruptedException ex) {
						}
					}
				}
				
				if (activityId!=null) {
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
					isRunning = true;
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
				if (sample!=null) {
					try {
						samplingQueue.returnEmptyInstance(sample);
					} catch (InterruptedException e) {
						Log.e(Constants.TAG, "Error", e);
					}
				}
			}
			
			if (isRunning) {
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
			
			while (samplingQueue.getFilledSize()>0) {
				try {
					samplingQueue.returnEmptyInstance(samplingQueue.takeFilledInstance());
				} catch (InterruptedException e) {
				}
			}
			
			//	post stop activity request
			if (activityId!=null) {
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
		private List<LocationPoint> pointsToUpload = new ArrayList<LocationPoint>();
		private List<SensorData> sensorDataToUpload = new ArrayList<SensorData>();
		
		public ActivityUploaderHelper(
				int helperIndex,
				ActivityUploader uploaderHelper,
				List<SensorData> sensorDataToUpload) {
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
			SharedPreferences state = context.getSharedPreferences(
					Constants.SHARE_PREF, Context.MODE_PRIVATE);
			this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
					DefSettings.getUsername(state),
					DefSettings.getPassword(state)
					);
			
			boolean previousWasIOException = false;
			
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
						
						LocationPoint lp = pointsToUpload.get(0);
						SensorData sd = sensorDataToUpload.isEmpty()?null:sensorDataToUpload.get(0);
						serviceMsgHandler.onSystemMessage(
								"Uploaded: "+timestampToDate(lp.timeStamp)+
								((sd==null)?"":
								": HR="+(sd.heartRate==null?"null":String.format("%02d", sd.heartRate)) +
								", CAD="+(sd.cadence==null?"null":String.format("%02d", sd.cadence)) +
								", POW="+(sd.power==null?"null":String.format("%02d", sd.power))
								));
						
						pointsToUpload.clear();
						sensorDataToUpload.clear();
						previousWasIOException = false;
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
				} catch (MapMyMapsException e) {
					Log.e(Constants.TAG, "Helper "+helperIndex+": MapMyTracks Service Error", e);
					serviceMsgHandler.onCriticalError("MapMyTracks:"+e.getMessage());
				}
			}
			
			mapMyTracksInterfaceApi.shutdown();
			Log.d(Constants.TAG, "Helper "+helperIndex+": Exiting");		
		}

	}
	
	private void addData(List<LocationPoint> pointsToUpload, List<SensorData> sensorDataToUpload, Sample sample)
	{
		LocationPoint locationPoint = new LocationPoint();
		
		locationPoint.timeStamp = sample.timeStamp;
		locationPoint.latitude = sample.latitude;
		locationPoint.longitude = sample.longitude;
		locationPoint.altitude = sample.altitude;
		
		pointsToUpload.add(locationPoint);
		
		if (sample.heartRate!=null || sample.cadence!=null || sample.power!=null) {
			SensorData sensorData = new SensorData();
			
			sensorData.timeStamp = sample.timeStamp;
			sensorData.cadence = sample.cadence;
			sensorData.heartRate = sample.heartRate;
			sensorData.power = sample.power;
			
			sensorDataToUpload.add(sensorData);
			
			Log.d(Constants.TAG, "HR = "+sample.heartRate);
		} else {
			//Log.d(Constants.TAG, "No Sensor Data Found");
		}
	}
	
	private void grabData(long waitDuration, List<LocationPoint> pointsToUpload, List<SensorData> sensorDataToUpload) throws InterruptedException
	{
		Sample sample = null;
		try {
			sample = samplingQueue.takeFilledInstance(waitDuration);
			addData(pointsToUpload, sensorDataToUpload, sample);
			samplingQueue.returnEmptyInstance(sample);
			sample = null;
			while (true) {
				sample = samplingQueue.takeFilledInstanceIfAvail();
				if (sample!=null) {
					addData(pointsToUpload, sensorDataToUpload, sample);
					samplingQueue.returnEmptyInstance(sample);
					sample = null;
				} else {
					break;
				}
			}
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
