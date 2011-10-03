package au.urremote.bridge.service;

import java.util.List;

import android.os.IBinder;

public interface ILiveMonitorBinder extends IBinder {
	
	public boolean isStarted();
	public void stopService();
	
	public void setUiActive(boolean visible);

	public boolean isRecording();
	public void startRecording() throws Exception;
	public void stopRecording();
	
	public void registerUpdateListener(UpdateListener handler);
	public void unregisterUpdateListener(UpdateListener handler);
	
	public List<SystemMessage> getSystemMessages();
	
}
