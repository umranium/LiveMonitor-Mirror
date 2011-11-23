package com.urremote.invoker.client;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.urremote.invoker.shared.InvalidDeviceStateException;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("devices")
public interface DeviceService extends RemoteService {
	
	void setChannelDeviceFilter(String channelToken, String device);
	
	List<String> getDeviceList() throws Exception;
	
	void deleteDevice(String device) throws InvalidDeviceStateException;
	
	Long getDeviceLastUpdate(String device) throws InvalidDeviceStateException;
	
	boolean isDeviceRecordingState(String device) throws InvalidDeviceStateException;
	
	void startDeviceRecording(String device) throws InvalidDeviceStateException;
	
	void stopDeviceRecording(String device) throws InvalidDeviceStateException;
	
}
