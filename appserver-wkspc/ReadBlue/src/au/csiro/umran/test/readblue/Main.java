package au.csiro.umran.test.readblue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.BasicManagedEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import au.csiro.blueparser.BlueParser;
import au.csiro.blueparser.OnMessageListener;

public class Main extends Activity {
	
	private static final String TAG = "ReadBlue";
	private static final int REQUEST_ENABLE_BT = 1;
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int MAX_MSG_COUNT = 30;	
	private static final byte[] MARKER = new byte[] {(byte)0x52,(byte)0x48};


	private BluetoothAdapter bluetoothAdapter;

	private boolean scanningReceiversRegistered = false;

	private Button btnStartScan;
	private Button btnStopScan;
	private ListView lstMessages;
	private ArrayAdapter<String> lstMessagesAdapter;
	private ListView lstConnectableDevices;
	private ArrayAdapter<ConnectableDevice> lstConnectableDevicesAdapter;

	private DeviceConnection currentConnection = null;

	private Looper looper;
	private Handler handler;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.looper = this.getMainLooper();
		this.handler = new Handler(looper);
		
		btnStartScan = (Button) findViewById(R.id.btn_start_scan);
		btnStartScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startScanning();
			}
		});

		btnStopScan = (Button) findViewById(R.id.btn_stop_scan);
		btnStopScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopScanning(true);
			}
		});
		btnStopScan.setEnabled(false);

		lstMessages = (ListView) findViewById(R.id.lst_messages);
		lstMessagesAdapter = new ArrayAdapter<String>(this, R.layout.msg_item);
		lstMessages.setAdapter(lstMessagesAdapter);

		lstConnectableDevices = (ListView) findViewById(R.id.lst_connectable_devices);
		lstConnectableDevicesAdapter = new ArrayAdapter<ConnectableDevice>(
				this, R.layout.device_item);
		lstConnectableDevices.setAdapter(lstConnectableDevicesAdapter);
		lstConnectableDevices.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View vew, int pos,
					long id) {
				ConnectableDevice connectableDevice = lstConnectableDevicesAdapter
						.getItem(pos);
				connectTo(connectableDevice.device);
			}
		});
		
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();

		turnOnBluetooth();
	}

	@Override
	protected void onStop() {
		super.onStop();

		stopScanning(true);
		
		if (currentConnection!=null) {
			currentConnection.close();
			currentConnection = null;
		}
	}

	private void displayMsg(String msg) {
		int count = lstMessagesAdapter.getCount(); 
		for (int i=MAX_MSG_COUNT; i<count; ++i)
			lstMessagesAdapter.remove(lstMessagesAdapter.getItem(0));
		lstMessagesAdapter.add(msg);
	}

	private void displayMsgFromSepThread(final String msg)
	{
		handler.post(new Runnable() {
			@Override
			public void run() {
				Main.this.displayMsg(msg);
			}
		});
	}
	
	private void enableUI(boolean enable) {
		btnStartScan.setEnabled(enable);
		btnStartScan.setEnabled(enable);
	}

	private void turnOnBluetooth() {

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			displayMsg("Device does not support bluetooth");
			enableUI(false);
			return;
		}

		if (!bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

	}

	private void startScanning() {
		if (currentConnection!=null) {
			connectTo(null);
		}
		
		IntentFilter foundFilter = new IntentFilter(
				BluetoothDevice.ACTION_FOUND);
		registerReceiver(deviceScanningFoundReceiver, foundFilter);
		
		IntentFilter startFinishFilter = new IntentFilter();
		startFinishFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		startFinishFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(deviceScanningStartFinishReceiver, startFinishFilter);

		scanningReceiversRegistered = true;

		bluetoothAdapter.startDiscovery();
		btnStartScan.setEnabled(false);
		btnStopScan.setEnabled(true);
	}

	private void stopScanning(boolean userInvoked) {
		if (scanningReceiversRegistered) {
			if (userInvoked) {
				displayMsg("Scanning stopped.");
			} else {
				displayMsg("Scanning finished.");
			}
			bluetoothAdapter.cancelDiscovery();
			unregisterReceiver(deviceScanningFoundReceiver);
			unregisterReceiver(deviceScanningStartFinishReceiver);
			scanningReceiversRegistered = false;
		}
		
		btnStartScan.setEnabled(true);
		btnStopScan.setEnabled(false);
	}

	private void connectTo(BluetoothDevice device) {
		
		BluetoothDevice prevDevice = null;
		
		if (currentConnection != null) {
			prevDevice = currentConnection.device;
			currentConnection.close();
			currentConnection = null;
		}

		if (device!=null && (prevDevice==null || !prevDevice.getAddress().equals(device.getAddress()))) {
			try {
				currentConnection = new DeviceConnection(device);
			} catch (IOException e) {
				displayMsg("Unable to establish socket to " + device.getName());
				Log.e(TAG, "Error establishing socket", e);
			}
		}

		lstConnectableDevicesAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ENABLE_BT: {
			switch (resultCode) {
			case RESULT_OK: {
				break;
			}
			case RESULT_CANCELED: {
				displayMsg("Bluetooth disabled");
				enableUI(false);
				break;
			}
			}
			break;
		}
		}
	}

	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver deviceScanningFoundReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				ConnectableDevice connectableDevice = new ConnectableDevice(
						device);
				displayMsg("Device Found: " + connectableDevice.toString());

				if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
					lstConnectableDevicesAdapter.add(connectableDevice);
				}
			}
		}
	};

	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver deviceScanningStartFinishReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				displayMsg("Scanning Started.");

				lstConnectableDevicesAdapter.clear();
			}
			
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				stopScanning(false);
			}
		}
	};

	private class ConnectableDevice {
		private BluetoothDevice device;

		public ConnectableDevice(BluetoothDevice device) {
			this.device = device;
		}

		@Override
		public String toString() {
			String connectionState = "";
			if (currentConnection!=null &&
					currentConnection.device.getAddress().equals(device.getAddress())) {
				connectionState = " ["+currentConnection.getConnectionState()+"]";
			}
			return String.format("%-20s (%20s)", device.getName(),
					device.getAddress())+connectionState;
		}
	}
	
	private OnMessageListener onMessageListener = new OnMessageListener() {
		
		@Override
		public void onMessage(byte[] message, int length) {
			String msgStr = bytesToString(message, length);
			displayMsgFromSepThread(msgStr);
			Log.d("BLUE-DATA", msgStr);
		}
		
	};
	

	private class DeviceConnection extends Thread {
		
		private BluetoothDevice device;
		private BluetoothSocket socket;
		private boolean isOpen = false;
		private boolean isClosing;
		
		public DeviceConnection(BluetoothDevice device) throws IOException {
			this.device = device;
			this.socket = getSocket();

			stopScanning(true);
			this.start();
		}

		public void close() {
			isClosing = true;
			if (!isOpen) {
				try {
					socket.close();
				} catch (IOException e) {
					Log.e(TAG,
							"Error while trying to abort openning connection",
							e);
				}
			}
			lstConnectableDevicesAdapter.notifyDataSetChanged();
		}
		
		public String getConnectionState() {
			if (isClosing) {
				return "disconnected";
			} else
			if (isOpen) {
				return "connected";
			} else {
				return "connecting";
			}
		}

		private BluetoothSocket getSocket() throws IOException {
			return device.createRfcommSocketToServiceRecord(SPP_UUID);
		}
		
		private void refreshConnectableDeviceAdapter()
		{
			handler.post(new Runnable() {
				@Override
				public void run() {
					lstConnectableDevicesAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void run() {
			
			try {
				this.socket.connect();
				isOpen = true;
				refreshConnectableDeviceAdapter();
				displayMsgFromSepThread("Connected to " + device.getName());

				try {
					InputStream in = this.socket.getInputStream();
					OutputStream out = this.socket.getOutputStream();
					BlueParser blueParser = new BlueParser(MARKER, in, 1024, 20, true, onMessageListener);

					while (!isClosing) {
						blueParser.process();
					}

				} catch (Exception e) {
					displayMsgFromSepThread("Read Error:" + e.getMessage());
					Log.e(TAG, "Error while reading from device", e);
				}
			} catch (IOException e1) {
				displayMsgFromSepThread("Error Openning Connection:" + e1.getMessage());
				Log.e(TAG, "Error while connecting to device", e1);
			} finally {
				try {
					displayMsgFromSepThread("Disconnected from " + device.getName());
					this.socket.close();
				} catch (IOException e) {
				}
			}
			
		}
		
		
	}
	
	private static String bytesToString(byte[] bytes, int len)
	{
		StringBuilder builder = new StringBuilder();
		
		for (int i=0; i<len; ++i) {
			if (i>0)
				builder.append(",");
			int val = bytes[i] & 0xFF;
			String hex = Integer.toHexString(val);
			if (hex.length()<2)
				builder.append("0"+hex);
			else
				builder.append(hex);
		}
		
		return builder.toString();
	}
	

}