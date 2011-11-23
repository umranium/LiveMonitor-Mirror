package com.urremote.invoker.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface DeviceServiceAsync {
	
	void setChannelDeviceFilter(String channelToken, String device, AsyncCallback<Void> callback);
	
	void getDeviceList(AsyncCallback<List<String>> callback);
	
	void deleteDevice(String device, AsyncCallback<Void> callback);
	
	void getDeviceLastUpdate(String device, AsyncCallback<Long> callback);
	
	void isDeviceRecordingState(String device, AsyncCallback<Boolean> callback);
	
	void startDeviceRecording(String device, AsyncCallback<Void> callback);
	
	void stopDeviceRecording(String device, AsyncCallback<Void> callback);
	
}
