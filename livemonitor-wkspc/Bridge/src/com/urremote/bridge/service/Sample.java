package com.urremote.bridge.service;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

import com.google.android.apps.mytracks.content.Sensor;
import com.urremote.bridge.common.Constants;


public class Sample {
	
	public Location location;
	public final List<Sensor.SensorDataSet> sensorData = new ArrayList<Sensor.SensorDataSet>(Constants.SAMPLING_INTERVAL);
	
}
