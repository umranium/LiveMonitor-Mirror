package com.urremote.bridge.service;

import java.util.List;

import android.os.IBinder;

public interface ILiveMonitorBinder extends IBinder {
	
	public boolean isStarted();
	public void stopService();
	
	public void setUiActive(boolean visible);
	
	/**
	 * @return The number of records that are pending upload.
	 */
	public int getPendingUploadCount();

	public boolean isRecording();
	public boolean isRecordingPaused();
	public void startRecording() throws Exception;
	public void pauseRecording(boolean stopMyTracks) throws Exception;
	public void stopRecording();
	
	public void registerUpdateListener(UpdateListener handler);
	public void unregisterUpdateListener(UpdateListener handler);
	
	public List<SystemMessage> getSystemMessages();
	
}
