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

public class Main_old extends Activity {
	
	private static final String TAG = "ReadBlue";
	private static final int REQUEST_ENABLE_BT = 1;
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int MAX_MSG_COUNT = 30;
	public static final URI URI_DATA_UPLOAD = URI.create("http://testing-umranium.appspot.com/receive");
	private static final int UPLOAD_BATCH_SIZE = 100;
	

	private BluetoothAdapter mBluetoothAdapter;

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
	private Uploader uploader;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.looper = this.getMainLooper();
		this.handler = new Handler(looper);
		this.uploader = new Uploader();
		this.uploader.start();
		
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
		
		this.uploader.quit();
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
				Main_old.this.displayMsg(msg);
			}
		});
	}
	
	private void enableUI(boolean enable) {
		btnStartScan.setEnabled(enable);
		btnStartScan.setEnabled(enable);
	}

	private void turnOnBluetooth() {

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			displayMsg("Device does not support bluetooth");
			enableUI(false);
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
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

		mBluetoothAdapter.startDiscovery();
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
			mBluetoothAdapter.cancelDiscovery();
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
	
	private static final byte[] MARKER = new byte[] {(byte)0x52,(byte)0x48};

	private class DeviceConnection extends Thread {
		
		private BluetoothDevice device;
		private BluetoothSocket socket;
		private boolean isOpen = false;
		private boolean isClosing;
		
		private Queue<byte[]> msgQueue = new LinkedList<byte[]>();  
		private ArrayList<byte[]> msgBatch = new ArrayList<byte[]>(1000);
		private byte[] data = new byte[1024];
		private int dataWriteLoc = 0;
		private boolean foundFirstMarker = false;

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
			
//			byte[] msg = new byte[] {
//					(byte)00, (byte)0x52, (byte)0x48, (byte)0xff, (byte)0x00, (byte)0x7b, (byte)0x6b, (byte)0x8c, (byte)0x39, (byte)0x31, (byte)0x00
//			};
//			
//			Log.i(TAG, "found="+findMarker(msg, 0, msg.length));
			
			try {
				this.socket.connect();
				isOpen = true;
				refreshConnectableDeviceAdapter();
				displayMsgFromSepThread("Connected to " + device.getName());

				try {
					InputStream in = this.socket.getInputStream();
					OutputStream out = this.socket.getOutputStream();

					while (!isClosing) {
						if (!foundFirstMarker) {
							int len = in.read(data, dataWriteLoc, data.length-dataWriteLoc);
							int newLen = dataWriteLoc + len;
							int foundLoc = findMarker(data, 0, newLen);
							
							//Log.d(TAG, "Check: "+bytesToString(data, newLen));
							//Log.d(TAG, "Found: "+foundLoc);
							
							foundFirstMarker = foundLoc>=0;
							
							if (foundFirstMarker) {
								//Log.d(TAG, "found first marker");
								
								//	shift all data back to index 0
								for (int i=foundLoc; i<newLen; ++i) {
									data[i-foundLoc] = data[i];
								}
								//	new write location
								dataWriteLoc = newLen-foundLoc;
							} else {
								if (newLen-MARKER.length+1>0) {
									//	shift all data from MARKER.length-1 bytes from the end of
									//		data, to the start of data
									for (int j=0; j<MARKER.length-1; ++j) {
										data[j] = data[newLen-MARKER.length+1+j];
									}
									dataWriteLoc = MARKER.length-1;
								} else {
									//	retain all data
									dataWriteLoc = newLen;
								}
							}
						} else {
							int len = in.read(data, dataWriteLoc, data.length-dataWriteLoc);
							int newLen = dataWriteLoc + len;
							//	the start is expected to be the marker
							int msgStart = 0;
							int nextLoc = findMarker(data, msgStart+MARKER.length, newLen);
							
							//Log.d(TAG, "Process: "+bytesToString(data, newLen));
							
							while (nextLoc>=0) {
								int msgLen = nextLoc-msgStart;
								
								msgFound(msgStart, msgLen);
								
								msgStart = nextLoc;
								nextLoc = findMarker(data, msgStart+MARKER.length, newLen);
							}
							
							if (msgStart>0) {
								//	shift msg from msgStart to zero, in case the next msg is really long!
								for (int i=msgStart; i<newLen; ++i) {
									data[i-msgStart] = data[i];
								}
								dataWriteLoc = newLen - msgStart;
							}
						}
						
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
		
		private int findMarker(byte[] bytes, int start, int len)
		{
			for (int i=start; i<len; ++i) {
//				Log.d(TAG, "i="+i+", bytes[i]="+bytes[i]+", MARKER[0]="+MARKER[0]+", len-i="+(len-i)+", MARKER.length="+MARKER.length);
				
				if (bytes[i]==MARKER[0] && len-i>=MARKER.length) {
//					Log.d(TAG, "check");
					
					boolean found = true;
					
					for (int j=1; j<MARKER.length; ++j)
						if (bytes[i+j]!=MARKER[j]) {
							found = false;
							break;
						}
					
//					Log.d(TAG, "found="+found);
					
					if (found) {
						return i;
					}
				}
			}
			
			return -1;
		}
		
		private void msgFound(int start, int len)
		{
			byte[] msg = new byte[len];
			for (int i=0; i<len; ++i)
				msg[i] = data[start+i];
//			msgBatch.add(msg);
//			
//			if (msgBatch.size()>=UPLOAD_BATCH_SIZE) {
//				byte[][] uploadBatch = new byte[UPLOAD_BATCH_SIZE][];
//				for (int i=UPLOAD_BATCH_SIZE-1; i>=0; --i) {
//					uploadBatch[i] = msgBatch.get(i);
//					msgBatch.remove(i);
//				}
//				uploader.enqueue(uploadBatch);
//				displayMsgFromSepThread("A batch of "+UPLOAD_BATCH_SIZE+" was sampled.");
//			}
			Log.d(TAG, "Msg: "+bytesToString(msg, len));
		}
		
	}
	
	private class Uploader extends Thread {
		
		private java.util.concurrent.ArrayBlockingQueue<byte[][]> queue = new ArrayBlockingQueue<byte[][]>(1000);
		
		private boolean isQuiting = false;
		
		public void quit() {
			isQuiting = true;
			this.interrupt();
		}
		
		@SuppressWarnings("unused")
		public void enqueue(byte[][] d) {
			this.queue.add(d);
			synchronized (this.queue) {
				this.queue.notify();
			}
		}
		
		@Override
		public void run() {
			while (!isQuiting) {
				try {
					synchronized (queue) {
						if (queue.isEmpty())
							queue.wait();
						
						byte[][] msg = queue.remove();
						
						HttpPost post = new HttpPost(URI_DATA_UPLOAD);
						
						for (int i=0; i<msg.length; ++i) {
							post.addHeader(Integer.toString(i), bytesToString(msg[i], msg[i].length));
						}
						
						DefaultHttpClient client = new DefaultHttpClient();
						HttpResponse response = client.execute(post);
						int code = 0;
						response.getStatusLine().getStatusCode();
						String responseText = "";
						
						if (response.getStatusLine()!=null) {
							code = response.getStatusLine().getStatusCode();
							responseText = response.getStatusLine().getReasonPhrase();
						}
						
						if (response.getEntity() instanceof BasicManagedEntity) {
							BasicManagedEntity managedEntity = (BasicManagedEntity)response.getEntity();
							long contentLength = managedEntity.getContentLength();
							if (contentLength>2048)
								contentLength = 2048;
							if (contentLength>0) {
								InputStream inputStream = managedEntity.getContent();
								byte[] content = new byte[(int)contentLength];
								inputStream.read(content);
								responseText = new String(content);
							}
						} else {
							responseText = response.getEntity().toString();
						}
						
						Log.i(TAG, "Post Result: "+code+": "+responseText);
						displayMsgFromSepThread("Upload Result Code: "+code);
					}
				} catch (NoSuchElementException ex) {
					//	ignore
					Log.d(TAG, "No Message Found!");
				} catch (Exception ex) {
					Log.e(TAG, "Uploader Error", ex);
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