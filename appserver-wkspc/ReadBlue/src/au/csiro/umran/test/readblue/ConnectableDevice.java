package au.csiro.umran.test.readblue;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

public class ConnectableDevice {
	
	private Context context;
	private BluetoothAdapter adapter;
	private BluetoothDevice device;
	private DeviceConnection connection;

	public ConnectableDevice(Context context, BluetoothAdapter adapter, BluetoothDevice device) {
		this.context = context;
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
	
	public void connect(ReadBlueServiceBinder binder) throws IOException {
		if (connection!=null) {
			Log.d(Constants.TAG, "Device "+device.getName()+" has a preexisting connection. Disconnecting first.");
			disconnect();
		}
		
		Log.d(Constants.TAG, "Establishing a DeviceConnection.");
		connection = new DeviceConnection(context, binder, this);
		connection.connect();
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
	
	public boolean isRecording() {
		return connection!=null && connection.isRecording();
	}
	
	public DeviceConnection getConnection() {
		return connection;
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