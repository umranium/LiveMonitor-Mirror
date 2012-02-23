package au.csiro.umran.test.readblue;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class ConnectableDevice {
	
	private BluetoothAdapter adapter;
	private BluetoothDevice device;
	private DeviceConnection connection;

	public ConnectableDevice(BluetoothAdapter adapter, BluetoothDevice device) {
		this.adapter = adapter;
		this.device = device;
		this.connection = null;
	}
	
	public BluetoothAdapter getAdapter() {
		return adapter;
	}
	
	public BluetoothDevice getDevice() {
		return device;
	}
	
	public void establishConnection(ReadBlueServiceBinder binder) throws IOException {
		if (connection!=null) {
			Log.d(Constants.TAG, "Device "+device.getName()+" has a preexisting connection. Disconnecting first.");
			disconnect();
		}
		
		Log.d(Constants.TAG, "Establishing a DeviceConnection.");
		connection = new DeviceConnection(binder, this);
		connection.start();
		Log.d(Constants.TAG, "DeviceConnection established.");
	}
	
	public void disconnect() {
		if (connection!=null) {
			Log.d(Constants.TAG, "DeviceConnection closing.");
			DeviceConnection con = connection; 
			connection = null;
			con.close();
		}
	}
	
	public boolean isConnected() {
		return connection!=null;
	}
	
	public String getConnectionState() {
		return connection==null?"not connected":connection.getConnectionState();
	}
	
	public void startRecording() {
		if (connection!=null)
			connection.startRecording();
	}
	
	public void stopRecording() {
		if (connection!=null)
			connection.stopRecording();
	}
	
	@Override
	public String toString() {
		String connectionState = "";
		if (isConnected()) {
			connectionState = " ["+getConnectionState()+"]";
		}
		return String.format("%-15s (%18s)", device.getName(),
				device.getAddress())+connectionState;
	}
}