package au.csiro.bikometer.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import au.csiro.bikometer.common.Constants;

public class BikometerService extends Service {
	
	private class LiveMonitorBinder extends Binder implements IBikometerBinder {
		@Override
		public void registerUpdateListener(UpdateListener handler) {
			BikometerService.this.statusUpdateHandlers.add(handler);
		}
		
		@Override
		public void unregisterUpdateListener(UpdateListener handler) {
			BikometerService.this.statusUpdateHandlers.remove(handler);
		}
	}
	
	private LiveMonitorBinder binder = new LiveMonitorBinder();
	private UpdateListenerCollection statusUpdateHandlers = new UpdateListenerCollection();

	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(Constants.DEBUG_TAG, this.getClass().getSimpleName()+":onBind()");
		return binder;
	}
	
	private void startService() {
		Intent intent = new Intent(this, BikometerService.class);		
		this.startService(intent);
	}
	
	private void stopService() {
		this.stopSelf();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(Constants.DEBUG_TAG, this.getClass().getSimpleName()+":onCreate()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(Constants.DEBUG_TAG, this.getClass().getSimpleName()+":onDestroy()");
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(Constants.DEBUG_TAG, this.getClass().getSimpleName()+":onStart()");
	}
	
	

}
