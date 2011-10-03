package au.urremote.bridge.service.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import au.urremote.bridge.common.Constants;
import au.urremote.bridge.service.InternalServiceMessageHandler;
import au.urremote.bridge.service.Sample;
import au.urremote.bridge.service.SamplingQueue;
import au.urremote.bridge.service.filters.median.FilterOut;
import au.urremote.bridge.service.filters.median.MedianFilter;

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.services.ITrackRecordingService;

public class MonitoringThread {
	
	private InternalServiceMessageHandler serviceMsgHandler; 
	private Context context;
	private ITrackRecordingService mytracks;
	private Timer timer;
	private MonitorTimerTask monitorTimerTask;
	private SamplingQueue samplingQueue;
	private boolean requestedTrackRecording = false;
	private LocationManager locationManager;
	private boolean locationListenerRegistered = false;
	private boolean gpsProviderFound = false;
	private Location prevRecordedLocation = null;
	private Location latestLocation = null;
	
	private LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (isBetterLocation(location, latestLocation)) {
				latestLocation = location;
			}
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {

		}
	};
	
	public MonitoringThread(
			Context context,
			InternalServiceMessageHandler serviceMsgHandler,
			SamplingQueue samplingQueue)
	{
		this.context = context;
		this.serviceMsgHandler = serviceMsgHandler;
		this.samplingQueue = samplingQueue;
		this.timer = new Timer("monitoring-timer");
		
		this.locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		this.locationListenerRegistered = false;
		
		if (hasGPS())
			this.gpsProviderFound = true;
		
		//registerLocationListener();
	}
	
	public void registerLocationListener() {
		if (this.locationListenerRegistered)
			return;
		this.locationListenerRegistered = true;
		
		this.locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				0,
				0,
				locationListener
				);
		
//		this.locationManager.requestLocationUpdates(
//			LocationManager.NETWORK_PROVIDER,
//			0,
//			0,
//			locationListener
//		);
		
	}
	
	public void unregisterLocationListener() {
		if (!this.locationListenerRegistered)
			return;
		this.locationListenerRegistered = false;
		
		this.locationManager.removeUpdates(locationListener);
	} 
	
	public boolean isLocationListenerRegistered() {
		return this.locationListenerRegistered;
	}
	
	public void destroy() {
		timer.cancel();
		unregisterLocationListener();
	}
	
	public void begin() throws Exception {
		serviceMsgHandler.onSystemMessage("Connecting to MyTracks...");
		registerLocationListener();
		connectToMyTracks();
		
    	if (monitorTimerTask!=null) {
    		monitorTimerTask.cancel();
    	}
    	prevRecordedLocation = null;
    	monitorTimerTask = new MonitorTimerTask();
		timer.schedule(monitorTimerTask, Constants.MONITORING_INTERVAL, Constants.MONITORING_INTERVAL);
	}
	
	public void quit() {
		if (mytracks!=null && requestedTrackRecording) {
			try {
				if (mytracks.isRecording())
					mytracks.endCurrentTrack();
			} catch (RemoteException e) {
				Log.e(Constants.TAG, "Error", e);
			}
		}
		if (monitorTimerTask!=null) {
			monitorTimerTask.cancel();
			timer.purge();
			monitorTimerTask = null;
		}
		context.unbindService(connection);
	}
		
	private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        	ITrackRecordingService mytracksService = ITrackRecordingService.Stub.asInterface(arg1);
        	
        	serviceMsgHandler.onSystemMessage("Connected to MyTracks");
        	try {
				if (mytracksService.isRecording()) {
					serviceMsgHandler.onSystemMessage("MyTracks is already Recording.");
				} else {
					Log.d(Constants.TAG, "Launching MyTracks To Start Recording");
//					Toast.makeText(context, 
//							"Please start MyTracks Recording.", 
//							Toast.LENGTH_LONG).show();
//					serviceMsgHandler.requestLaunchMyTracks();
					requestedTrackRecording = true;
					mytracksService.startNewTrack();
					serviceMsgHandler.onSystemMessage("Requesting MyTracks to Start Recording.");
				}
			} catch (Exception e) {
				Log.e(Constants.TAG, "Error", e);
			}
        	
        	mytracks = mytracksService;
        }

        public void onServiceDisconnected(ComponentName arg0) {
        	mytracks = null;
        	serviceMsgHandler.onCriticalError("Disconnected from MyTracks.");
        }

    };
	
	/**
	 * Attempts to connect to the MyTracks service
	 * 
	 * @return
	 * 	whether the connection was successful or not
	 * @throws Exception 
	 */
	private void connectToMyTracks() throws Exception {
		this.requestedTrackRecording = false;
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(
				Constants.MY_TRACKS_PACKAGE,
				Constants.MY_TRACKS_SERVICE_CLASS));
		if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
			throw new Exception("Unable to find MyTracks service. MyTracks may not be installed.");
		} else {
			Log.i(Constants.TAG, "Connecting to MyTracks service");
		}
	}
	
	/**
	 * Never invoke this directly, always schedule through the timer
	 */
	private class MonitorTimerTask extends TimerTask {
		
		private boolean firstMyTracksRecording = true;
		private boolean wasMyTracksRecording = false;
		private boolean firstLocation = true;
		private int previousState = -1;
		private int locationIndex = 0;
		private MedianFilter medianFilter;
		
		private ArrayList<Sensor.SensorDataSet> bufferdSensorData = new ArrayList<Sensor.SensorDataSet>(10);
		
		@Override
		public void run() {
//			if (!gpsProviderFound) {
//				if (!hasGPS()) {
//					serviceMsgHandler.showToast("Please enable GPS", Toast.LENGTH_SHORT);
//					serviceMsgHandler.vibrate();
//					if (!serviceMsgHandler.isUiActive()) {
//						serviceMsgHandler.makeErrorSound();
//					}
//				} else {
//					gpsProviderFound = true;
//				}
//			}
			this.medianFilter = new MedianFilter(5, new FilterOut() {
				@Override
				public void receive(Location location) {
					Sample sample = null;
					try {
						sample = samplingQueue.takeEmptyInstance();
						sample.location = location;
						if (!bufferdSensorData.isEmpty()) {
							sample.sensorData.clear();
							sample.sensorData.addAll(bufferdSensorData);
							bufferdSensorData.clear();
						}
						samplingQueue.returnFilledInstance(sample);
						sample = null;
						Log.d(Constants.TAG, "MonitoringThread: filled sample sent");
					} catch (Exception e) {
						Log.e(Constants.TAG, "Error", e);
					} finally {
						if (sample!=null) {
							try {
								samplingQueue.returnEmptyInstance(sample);
							} catch (InterruptedException e) {
							}
						}
					}
				}
			});
			
			Sample sample = null;
			try {
				if (mytracks!=null) {
					if (mytracks.isRecording()) {
						int state = mytracks.getSensorState();
						Sensor.SensorState sensorState = Sensor.SensorState.valueOf(state);
						if (sensorState==null)
							sensorState = Sensor.SensorState.NONE;
						
						if (previousState!=state) {
							Log.d(Constants.TAG, "sensorState="+sensorState);
							serviceMsgHandler.onSystemMessage("MyTrack Sensor State changed to '"+sensorState+"'");
							previousState = state;
						}
						
						if (sensorState!=null  && sensorState.equals(SensorState.CONNECTED)) {
							byte[] sensorData = mytracks.getSensorData();
							if (sensorData!=null) {
								Sensor.SensorDataSet sensorDataSet = Sensor.SensorDataSet.parseFrom(sensorData);
								bufferdSensorData.add(sensorDataSet);
							}
						}
					}
					
					
					Location location = latestLocation;
					//Log.d(Constants.TAG, "mytracks.isRecording()="+mytracks.isRecording()+" location="+((location!=null)?"available":"not available"));
					
					if (firstMyTracksRecording && mytracks.isRecording()) {
						serviceMsgHandler.onSystemMessage("MyTracks is Recording.");
						firstMyTracksRecording = false;
						wasMyTracksRecording = true;
					}
					
					if (firstLocation && location!=null) {
						serviceMsgHandler.onSystemMessage("Started Receiving Location Info.");
						firstLocation = false;
					}
					
					if (wasMyTracksRecording && !mytracks.isRecording()) {
						serviceMsgHandler.onSystemMessage("MyTracks stopped Recording.");
						firstMyTracksRecording = true;
						wasMyTracksRecording = false;
					}
					
//					if (location!=null) {
//						++locationIndex;
//					}
					
					if (mytracks.isRecording() && location!=null) {
						Log.d(Constants.TAG, "MonitoringThread: location sent to median filter");
						medianFilter.add(location);
						
						//if ((locationIndex % 3)==0)
//						if (prevRecordedLocation==null || prevRecordedLocation.distanceTo(location)>Constants.MINIMUM_RECORDING_DISTANCE)
//						{							
//							locationIndex = 0;
//							prevRecordedLocation = location;
//						}
					}
					
				}
			} catch (Exception e) {
				Log.e(Constants.TAG, "Error", e);
			} finally {
				if (sample!=null) {
					try {
						samplingQueue.returnEmptyInstance(sample);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
	}
	
	protected boolean hasGPS()
	{
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//		List<String> providers = locationManager.getAllProviders();
//		for (String provider:providers) {
//			if (provider.contains(LocationManager.GPS_PROVIDER)) {
//				Log.d(Constants.TAG, "GPS found");
//				return true;
//			}
//		}
//		return false;
	}
	
	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > Constants.LOCATION_MAX_UPDATE_INTERVAL;
		boolean isSignificantlyOlder = timeDelta < -Constants.LOCATION_MAX_UPDATE_INTERVAL;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}	
}
