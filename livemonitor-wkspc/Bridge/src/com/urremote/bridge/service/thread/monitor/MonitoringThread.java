package com.urremote.bridge.service.thread.monitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.google.android.apps.mytracks.content.Sensor.SensorData;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
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
	
	private static class DataMeans {
		int numSamples;
		double sumValues;
		
		public void reset() {
			numSamples = 0;
			sumValues = 0;
		}
		
		public boolean isEmpty() {
			return numSamples==0;
		}
		
		public void include(double value) {
			++numSamples;
			sumValues += value;
		}
		
		public double calcMean() {
			return sumValues / numSamples;
		}
	}
	
//	private static class LocMeans {
//		int numSamples;
//		long sumTime;
//		double sumLon;
//		double sumLat;
//		double sumAlt;
//		double sumAcc;
//		
//		public void reset() {
//			numSamples = 0;
//			sumTime = 0;
//			sumLon = 0;
//			sumLat = 0;
//			sumAlt = 0;
//			sumAcc = 0;
//		}
//		
//		public boolean isEmpty() {
//			return numSamples==0;
//		}
//		
//		public void include(Location location) {
//			++numSamples;
//			
//			long time = location.getTime();
//			if (time==0)
//				time = System.currentTimeMillis();
//			sumTime += time;
//			sumLon += location.getLongitude();
//			sumLat += location.getLatitude();
//			sumAlt += location.getAltitude();
//			sumAcc += location.getAccuracy();
//		}
//		
//		public Location calcMeanLoc() {
//			Location location = new Location(LocationManager.GPS_PROVIDER);
//			
//			location.setTime(sumTime / numSamples);
//			location.setLongitude(sumLon / numSamples);
//			location.setLatitude(sumLat / numSamples);
//			location.setAltitude(sumAlt / numSamples);
//			location.setAccuracy((float)(sumAcc / numSamples));
//			
//			return location;
//		}
//	}
	
	private class SensorDataSetMethodMap {
		String sensorDataName;
		Method hasSensorMethod;
		Method getSensorMethod;
		Method bldrSetSensorMethod;
		
		public SensorDataSetMethodMap(String sensorDataName, Method hasSensorMethod,
				Method getSensorMethod, Method bldrSetSensorMethod) {
			super();
			this.sensorDataName = sensorDataName;
			this.hasSensorMethod = hasSensorMethod;
			this.getSensorMethod = getSensorMethod;
			this.bldrSetSensorMethod = bldrSetSensorMethod;
		}
		
	}
	
	private class SensorDataSetMeans {
		
		List<SensorDataSetMethodMap> dataSetMethodMaps = new ArrayList<MonitoringThread.SensorDataSetMethodMap>();
		private Map<String,DataMeans> sensorDataMeans = new HashMap<String,DataMeans>();
		
		public SensorDataSetMeans() {
			try {
				Class<?> cl = Sensor.SensorDataSet.class;
				Class<?> bldrCl = Sensor.SensorDataSet.Builder.class;
				for (Method m:cl.getMethods()) {
					if (m.getName().matches("has[A-Z].*") && m.getReturnType().equals(Boolean.TYPE)) {
						String sensorDataName = m.getName().substring("has".length());
						Log.d(Constants.TAG, "SensorDataSet: checking methods for "+sensorDataName);
						Method hasSensorMethod = m;
						Method getSensorMethod = cl.getMethod("get"+sensorDataName);
						Method setSensorMethod = bldrCl.getMethod("set"+sensorDataName, SensorData.class);
						dataSetMethodMaps.add(new SensorDataSetMethodMap(
								sensorDataName, 
								hasSensorMethod, 
								getSensorMethod,
								setSensorMethod));
					}
				}
				
				for (SensorDataSetMethodMap methodMap:dataSetMethodMaps) {
					sensorDataMeans.put(methodMap.sensorDataName, new DataMeans());
				}
			} catch (SecurityException e) {
				Log.e(Constants.TAG, "Error while initializing sensor data set means", e);
			} catch (NoSuchMethodException e) {
				Log.e(Constants.TAG, "Error while initializing sensor data set means", e);
			}
		}
		
		public void reset() {
			for (DataMeans dm:sensorDataMeans.values()) {
				dm.reset();
			}
		}
		
		public boolean isEmpty() {
			boolean empty = true;
			for (DataMeans dm:sensorDataMeans.values()) {
				if (!dm.isEmpty()) {
					empty = false;
					break;
				}
			}
			return empty;
		}
		
		public void include(SensorDataSet sds) {
			try {
				for (SensorDataSetMethodMap methodMap:dataSetMethodMaps) {
					Boolean has = (Boolean)methodMap.hasSensorMethod.invoke(sds);
					if (has) {
						Sensor.SensorData data = (Sensor.SensorData)methodMap.getSensorMethod.invoke(sds);
						if (data.hasValue()) {
							sensorDataMeans.get(methodMap.sensorDataName).include(data.getValue());
						}
					}
				}
			} catch (SecurityException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			} catch (IllegalArgumentException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			} catch (IllegalAccessException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			} catch (InvocationTargetException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			}
		}
		
		public SensorDataSet calcMeanSensorDataSet() {
			SensorDataSet.Builder sdsBldr = SensorDataSet.newBuilder();
			
			try {
				for (SensorDataSetMethodMap methodMap:dataSetMethodMaps) {
					DataMeans means = sensorDataMeans.get(methodMap.sensorDataName);
					if (!means.isEmpty()) {
						double mean = means.calcMean();
						SensorData.Builder sdBldr = SensorData.newBuilder();
						sdBldr.setValue((int)mean);
						methodMap.bldrSetSensorMethod.invoke(sdsBldr, sdBldr.build());
					}
				}
			} catch (SecurityException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			} catch (IllegalArgumentException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			} catch (IllegalAccessException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			} catch (InvocationTargetException e) {
				Log.e(Constants.TAG, "Error while including data set in data set means", e);
			}			
			
			return sdsBldr.build();
		}
		
	}
	

	/**
	 * Never invoke this directly, always schedule through the timer
	 */
	private class MonitorTimerTask extends TimerTask {
		
		private boolean firstLocation = true;
		private int locationIndex = 0;
		
//		private LocMeans locMeans = new LocMeans();
		private List<Sensor.SensorDataSet> sensorDataSetList = new ArrayList<Sensor.SensorDataSet>();
		private SensorDataSetMeans sensorMeans = new SensorDataSetMeans();
		
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
					
//					locMeans.include(location);
					if (myTracksConnection.isRecording()) {
						sensorDataSetList.clear();
						myTracksConnection.retrieveSensorData(sample.sensorData);
						for (Sensor.SensorDataSet sensorDataSet:sensorDataSetList) {
							sensorMeans.include(sensorDataSet);
						}
					}
					
					if ((locationIndex % Constants.SAMPLING_INTERVAL) == 0) {
						sample = samplingQueue.takeEmptyInstanceIfAvail(); // first try and get an empty instance
						if (sample==null)
							sample = samplingQueue.takeFilledInstance(); // if not available, take a filled one (which would be the oldest one)
						sample.location = new Location(location); //locMeans.calcMeanLoc();
						//locMeans.reset();
						sample.sensorData.clear();
						if (!sensorMeans.isEmpty()) {
							sample.sensorData.add(sensorMeans.calcMeanSensorDataSet());
							sensorMeans.reset();
						}
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
