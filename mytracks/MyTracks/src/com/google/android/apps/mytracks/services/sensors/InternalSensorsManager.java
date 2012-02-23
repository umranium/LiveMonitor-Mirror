package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.Constants;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class InternalSensorsManager {
  
  private static final int[] REQ_SENSORS = new int[] {
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_MAGNETIC_FIELD,
//    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_GRAVITY
  };
  
  private Context context;
  private SensorManager sensorManager;
  private Map<Integer,InternalSensorEventListener> sensorEventListeners;
  
  public InternalSensorsManager(Context context) {
    this.context = context;
    this.sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    this.sensorEventListeners = new HashMap<Integer,InternalSensorEventListener>(100);  
  }
  
  public void init() {
    for (int sensor:REQ_SENSORS) {
      if (this.sensorManager.getDefaultSensor(sensor)!=null) {
        InternalSensorEventListener sensorEventListener = new InternalSensorEventListener(sensor, true);
        this.sensorManager.registerListener(
            sensorEventListener, 
            this.sensorManager.getDefaultSensor(sensor),
            SensorManager.SENSOR_DELAY_FASTEST
            );
        sensorEventListeners.put(sensor, sensorEventListener);
      }
    }
    
  }
  
  public void done() {
    for (InternalSensorEventListener listener:sensorEventListeners.values()) {
      sensorEventListeners.remove(listener);
    }
  }
  
  public com.google.android.apps.mytracks.content.Sensor.SensorDataSet read() {
    com.google.android.apps.mytracks.content.Sensor.SensorDataSet.Builder builder = 
        com.google.android.apps.mytracks.content.Sensor.SensorDataSet.newBuilder();
    
    if (sensorEventListeners.containsKey(Sensor.TYPE_ACCELEROMETER)) {
      InternalSensorEventListener eventListener = sensorEventListeners.get(Sensor.TYPE_ACCELEROMETER);
      
      if (eventListener.hasValues()) {
        synchronized (eventListener.mutex) {
          builder.setAccel(extractTriple(eventListener));
        }
      }
    }
    
    if (sensorEventListeners.containsKey(Sensor.TYPE_MAGNETIC_FIELD)) {
      InternalSensorEventListener eventListener = sensorEventListeners.get(Sensor.TYPE_MAGNETIC_FIELD);
      
      if (eventListener.hasValues()) {
        synchronized (eventListener.mutex) {
          builder.setMagneticField(extractTriple(eventListener));
        }
      }
    }
    
    if (sensorEventListeners.containsKey(Sensor.TYPE_GYROSCOPE)) {
      InternalSensorEventListener eventListener = sensorEventListeners.get(Sensor.TYPE_GYROSCOPE);
      
      if (eventListener.hasValues()) {
        synchronized (eventListener.mutex) {
          builder.setGyro(extractTriple(eventListener));
        }
      }
    }
    
    if (sensorEventListeners.containsKey(Sensor.TYPE_GRAVITY)) {
      InternalSensorEventListener eventListener = sensorEventListeners.get(Sensor.TYPE_GRAVITY);
      
      if (eventListener.hasValues()) {
        synchronized (eventListener.mutex) {
          builder.setGravity(extractTriple(eventListener));
        }
      }
    }
    
    return builder.build();
    
  }
  
  private
  com.google.android.apps.mytracks.content.Sensor.SensorDataTriple
  extractTriple(InternalSensorEventListener eventListener) {
    long timestamp = eventListener.getTimestamp();
    float[] values = eventListener.getValues();
    
    com.google.android.apps.mytracks.content.Sensor.SensorDataTriple.Builder tripleBuilder = 
        com.google.android.apps.mytracks.content.Sensor.SensorDataTriple.newBuilder();
    tripleBuilder.setState(com.google.android.apps.mytracks.content.Sensor.SensorState.CONNECTED);
    tripleBuilder.setTimestamp(timestamp);
    tripleBuilder.setX(values[0]);
    tripleBuilder.setY(values[1]);
    tripleBuilder.setZ(values[2]);
    
    eventListener.reset();
    
    return tripleBuilder.build();
  }
  
  private static String getSensorName(int sensor) {
    Class cl = Sensor.class;
    try {
      for (Field f:cl.getDeclaredFields()) {
        if ((f.getModifiers() & Modifier.PUBLIC)!=0 &&
            (f.getModifiers() & Modifier.STATIC)!=0 &&
            (f.getModifiers() & Modifier.FINAL)!=0 &&
            f.getType().equals(Integer.TYPE)
            ){
          if (f.getInt(null)==sensor) {
            return f.getName();
          }
        }
      }
      return "UNKNOWN";
    } catch (IllegalArgumentException e) {
      Log.e(Constants.TAG, "Error finding sensor name using reflection", e);
      return "UNKNOWN";
    } catch (IllegalAccessException e) {
      Log.e(Constants.TAG, "Error finding sensor name using reflection", e);
      return "UNKNOWN";
    }
    
  }
  
  private class InternalSensorEventListener implements SensorEventListener {
    
    public final Object mutex = new Object();
    
    private int sensorType;
    private String sensorName;
    private boolean averageOut;
    
    int numOfValues = 0;
    long latestTimestamp;
    float[] latestValues;
    float[] sumValues = new float[3];
    float[] averageValues = new float[3];
    
    public InternalSensorEventListener(int sensorType, boolean averageOut) {
      this.sensorType = sensorType;
      this.sensorName = getSensorName(sensorType);
      this.averageOut = averageOut;
    }
    
    public boolean hasValues() {
      return numOfValues > 1;
    }
    
    public long getTimestamp() {
      return latestTimestamp;
    }
    
    public float[] getValues() {
      if (averageOut) {
        for (int i=0; i<3; ++i) {
          averageValues[i] = sumValues[i] / numOfValues;
        }
        return averageValues;
      } else {
        return latestValues;
      }
    }
    
    public void reset() {
      if (averageOut) {
        numOfValues = 0;
        for (int i=0; i<3; ++i) {
          sumValues[i] = 0;
        }
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
      synchronized (mutex) {
        ++numOfValues;
        
        if (averageOut) {
          for (int i=0; i<3; ++i)
            sumValues[i] += event.values[i];
        } else {
          latestValues = event.values;
        }
        
        latestTimestamp = event.timestamp;
        
//        Log.d("Sensor:"+sensorName, event.timestamp+":"+Arrays.toString(event.values));
      }
    }
    
  }

  
}
