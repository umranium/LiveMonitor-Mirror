package au.csiro.umran.test.readblue;

import java.util.List;

public interface ReadBlueServiceBinder {
	
	void registerEventHandler(ServiceEventHandler eventHandler);
	
	boolean isScanningEnabled();
	void startScanning();
	void stopScanning();
	boolean isScanning();
	Object getWaitForScanningMutex();
	
	void startCalibration();
	void startRecording();
	void stopRecording();
	
	boolean isAnyConnected();
	boolean isAnyRecording();
	
	List<ConnectableDevice> getConnectableDevices();
	void toggleDeviceConnection(ConnectableDevice device);
	void deviceConnectionChanged(ConnectableDevice device);
	
	List<SystemMessage> getMessages();
	void addMessage(String msg);
	
	void playSoundAlert();
	void vibrate();
	
	void setMarker(String s);
	
}
