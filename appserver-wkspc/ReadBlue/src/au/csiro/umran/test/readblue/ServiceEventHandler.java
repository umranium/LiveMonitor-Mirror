package au.csiro.umran.test.readblue;

public interface ServiceEventHandler {
	
	void onAttemptEnableBluetooth();
	void onScanningStateChanged();
	void onConnectableDevicesUpdated();
	void onMessagesUpdated();

}
