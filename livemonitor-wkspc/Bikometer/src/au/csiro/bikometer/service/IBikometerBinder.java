package au.csiro.bikometer.service;

import android.os.IBinder;

public interface IBikometerBinder extends IBinder {
	
	public void registerUpdateListener(UpdateListener handler);
	public void unregisterUpdateListener(UpdateListener handler);
	
}
