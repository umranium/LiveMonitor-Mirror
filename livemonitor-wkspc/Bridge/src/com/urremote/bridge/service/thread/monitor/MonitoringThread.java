package com.urremote.bridge.service.thread.monitor;

import java.util.ArrayList;
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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.CustomUncaughtExceptionHandler;
import com.urremote.bridge.service.InternalServiceMessageHandler;
import com.urremote.bridge.service.Sample;
import com.urremote.bridge.service.SamplingQueue;
import com.urremote.bridge.service.thread.monitor.exceptions.MyTracksNotInstalledException;
import com.urremote.bridge.service.thread.monitor.exceptions.MyTracksPermissionsNotGrantedException;

public class MonitoringThread {
	
	private InternalServiceMessageHandler serviceMsgHandler; 
	private Context context;
	private Timer timer;
	private MonitorTimerTask monitorTimerTask;
	private SamplingQueue samplingQueue;
	private LocationManager locationManager;
	private boolean locationListenerRegistered = false;
	private Location latestLocation = null;
	private MyTracksConnection myTracksConnection;
	
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
			InternalServiceMessageHandler criticalErrorHandler,
			SamplingQueue samplingQueue)
	{
		this.context = context;
		this.serviceMsgHandler = criticalErrorHandler;
		this.samplingQueue = samplingQueue;
		this.timer = new Timer("monitoring-timer");
		
		this.locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		this.locationListenerRegistered = false;
		
		this.myTracksConnection = new MyTracksConnection(serviceMsgHandler, context);
//		registerLocationListener();
	}
	
	public void registerLocationListener() {
		if (this.locationListenerRegistered)
			return;
		this.locationListenerRegistered = true;
		this.latestLocation = null;
		this.locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				0,
				0,
				locationListener
				);
		
		if (Constants.IS_TESTING) {
			//	can't get a GPS fix in-doors!
			this.locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER,
				0,
				0,
				locationListener
			);
		}
		
		Log.d(Constants.TAG, "Location Listener Registered");
	}
	
	public void unregisterLocationListener() {
		if (!this.locationListenerRegistered)
			return;
		this.locationListenerRegistered = false;
		
		this.locationManager.removeUpdates(locationListener);
		
		Log.d(Constants.TAG, "Location Listener Unregistered");
	}
	
	public boolean isLocationListenerRegistered() {
		return this.locationListenerRegistered;
	}
	
	public void destroy() {
		timer.cancel();
		unregisterLocationListener();
	}
	
	public void begin() throws MyTracksNotInstalledException, MyTracksPermissionsNotGrantedException {
		myTracksConnection.setShouldBeRecording(true);
		if (!myTracksConnection.isConnected())
			myTracksConnection.connectToMyTracks();

		registerLocationListener();
		
		startTimerTask();
	}
	
	public void quit(boolean turnOffFromMyTracks) {
		stopTimerTask();
		unregisterLocationListener();
		
		if (turnOffFromMyTracks) {
			myTracksConnection.setShouldBeRecording(false);
			if (myTracksConnection.isConnected())
				myTracksConnection.disconnectFromMyTracks();
		}
	}
	
	private void startTimerTask() {
    	if (monitorTimerTask!=null) {
    		monitorTimerTask.cancel();
    	}
    	monitorTimerTask = new MonitorTimerTask();
		timer.schedule(monitorTimerTask, Constants.MONITORING_INTERVAL, Constants.MONITORING_INTERVAL);
	}
	
	private void stopTimerTask() {
		if (monitorTimerTask!=null) {
			monitorTimerTask.cancel();
			timer.purge();
			monitorTimerTask = null;
		}
	}
	
	/**
	 * Never invoke this directly, always schedule through the timer
	 */
	private class MonitorTimerTask extends TimerTask {
		
		private boolean firstLocation = true;
		private int locationIndex = 0;
		
		@Override
		public void run() {
			CustomUncaughtExceptionHandler.setInterceptHandler(context, Thread.currentThread());
			
			Sample sample = null;
			try {
				Location location = latestLocation;
				
				myTracksConnection.updateState();
				
				if (firstLocation && location!=null) {
					serviceMsgHandler.onSystemMessage("Started Receiving Location Info.");
					firstLocation = false;
				}
				
				if (location!=null && myTracksConnection.isRecording()) {
					++locationIndex;
					
					if ((locationIndex % Constants.SAMPLING_INTERVAL) == 0) {
						sample = samplingQueue.takeEmptyInstanceIfAvail(); // first try and get an empty instance
						if (sample==null)
							sample = samplingQueue.takeFilledInstance(); // if not available, take a filled one (which would be the oldest one)
						sample.location = new Location(location);
						sample.sensorData.clear();
						if (myTracksConnection.isRecording())
							myTracksConnection.retrieveSensorData(sample.sensorData);
						samplingQueue.returnFilledInstance(sample);
						sample = null;
						
						Log.d(Constants.TAG, "In sampling queue: empty="+samplingQueue.getPendingEmptyInstances()+", filled="+samplingQueue.getPendingFilledInstances());
						Log.d(Constants.TAG, "OldMonitoringThread: filled sample sent");
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
