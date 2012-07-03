package com.urremote.test.sample;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MainActivity extends Activity {
	
	public static final String TAG = "TestSample";
	
	private Handler mainLooperHandler;
	private SensorManager sensorManager;
	
	private SensorEventListener sensorEventListener = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			Log.i(TAG, ""+event.timestamp);
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			
		}
		
	};
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
//        this.mainLooperHandler = new Handler(this.getMainLooper());
        this.sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	this.sensorManager.registerListener(sensorEventListener, 
    			this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
    			SensorManager.SENSOR_DELAY_FASTEST);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	this.sensorManager.unregisterListener(sensorEventListener);
    }
}