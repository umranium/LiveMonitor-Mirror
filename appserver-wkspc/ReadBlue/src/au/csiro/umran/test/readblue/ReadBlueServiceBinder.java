package au.csiro.umran.test.readblue;

import java.util.List;

public interface ReadBlueServiceBinder {
	
	void registerEventHandler(ServiceEventHandler eventHandler);
	
	boolean isScanningEnabled();
	void startScanning();
	void stopScanning();
	boolean isScanning();
	Object getWaitForScanningMutex();
	
	void startRecording();
	void stopRecording();
	
	
	List<ConnectableDevice> getConnectableDevices();
	void toggleDeviceConnection(ConnectableDevice device);
	void deviceConnectionChanged(ConnectableDevice device);
	
	List<SystemMessage> getMessages();
	void addMessage(String msg);
	
	void playSoundAlert();

}
