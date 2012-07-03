package au.csiro.umran.test.readblue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Adapter;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

public class ReadBlueService extends Service {
	
	private static final long SYSTEM_MSG_REFRESH_DELAY = 500L;
	private static final int MAX_MSG_COUNT = 30;	
	
	private class InternReadBlueServiceBinder extends Binder implements ReadBlueServiceBinder {

		@Override
		public void registerEventHandler(ServiceEventHandler eventHandler) {
			ReadBlueService.this.eventHandler = eventHandler;
			if (eventHandler!=null) {
				eventHandler.onScanningStateChanged();
				eventHandler.onConnectableDevicesUpdated();
				eventHandler.onMessagesUpdated();
			} else {
				if (!anyDeviceConnected() && !serviceStarted) {
					stopService();
				}
			}
		}
		
		@Override
		public boolean isScanningEnabled() {
			return scanningEnabled && !anyDeviceConnected();
		}
		
		@Override
		public void startScanning() {
			ReadBlueService.this.startScanning();
		}
		
		@Override
		public void stopScanning() {
			ReadBlueService.this.stopScanning(true);
		}

		@Override
		public boolean isScanning() {
			return scanning;
		}
		
		@Override
		public void startCalibration() {
			for (ConnectableDevice device:connectableDevices) {
				device.startRecording(true);
			}
		}
		
		@Override
		public void startRecording() {
			for (ConnectableDevice device:connectableDevices) {
				device.startRecording(false);
			}
		}
		
		@Override
		public void stopRecording() {
			for (ConnectableDevice device:connectableDevices) {
				device.stopRecording();
			}
		}
		
		@Override
		public boolean isAnyConnected() {
			return anyDeviceConnected();
		}
		
		@Override
		public boolean isAnyRecording() {
			return anyDeviceRecording();
		}

		@Override
		public Object getWaitForScanningMutex() {
			return waitForScanningMutex;
		}

		@Override
		public List<ConnectableDevice> getConnectableDevices() {
			return connectableDevices;
		}

		@Override
		public void toggleDeviceConnection(ConnectableDevice device) {
			ReadBlueService.this.toggleDeviceConnection(device);
			
			boolean devicesConnected = anyDeviceConnected();
			if (devicesConnected && !serviceStarted) {
				startService();
			} else 
				if (!devicesConnected && serviceStarted) {
					stopService();
				}
		}

		@Override
		public void deviceConnectionChanged(ConnectableDevice device) {
			if (eventHandler!=null) {
				eventHandler.onConnectableDevicesUpdated();
			}
		}

		@Override
		public List<SystemMessage> getMessages() {
			return messages;
		}

		@Override
		public synchronized void addMessage(String msg) {
//			Log.v(Constants.TAG, "Message: "+msg);
			
			if (msg==null) {
				throw new RuntimeException("Null message received!");
			}
			
			synchronized (messages) {
				long time = System.currentTimeMillis();
				messages.add(new SystemMessage(time, msg));
				while (messages.size()>MAX_MSG_COUNT)
					messages.remove(0);
			}
			
		}
		
		@Override
		public void playSoundAlert() {
			mainLooperHandler.post(new Runnable() {
				@Override
				public void run() {
					ReadBlueService.this.playSoundAlert();
				}
			});
		}
		
		@Override
		public void vibrate() {
			mainLooperHandler.post(new Runnable() {
				@Override
				public void run() {
					ReadBlueService.this.vibrate();
				}
			});
		}
		
		@Override
		public void setMarker(String s) {
			for (ConnectableDevice device:connectableDevices) {
				if (device.isConnected())
					device.getConnection().setMarker(s);
			}
		}
	}
	
	private InternReadBlueServiceBinder binder = new InternReadBlueServiceBinder();
	
	private AudioManager audioManager;
	private Vibrator vibrator;
	
	private boolean scanningEnabled;
	private boolean scanning;
	private boolean serviceStarted;
	private List<ConnectableDevice> connectableDevices;
	private List<SystemMessage> messages;
	private ServiceEventHandler eventHandler;
	
	private Handler mainLooperHandler;
	
	private BluetoothAdapter bluetoothAdapter;
	
	private final Object waitForScanningMutex = new Object();

	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(Constants.TAG, "Service created");
		
		this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		this.audioManager.setStreamVolume(Constants.ALERT_STREAM_TYPE, audioManager.getStreamMaxVolume(Constants.ALERT_STREAM_TYPE), 0);
		
		this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); 
		
		this.scanningEnabled = true;
		this.scanning = false;
		this.connectableDevices = new ArrayList<ConnectableDevice>();
		this.messages = new ArrayList<SystemMessage>();
		this.mainLooperHandler = new Handler(this.getMainLooper());
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			this.scanningEnabled = false;
			binder.addMessage("Device does not support bluetooth");
		}
		
		CustomUncaughtExceptionHandler.setInterceptHandler(this, Thread.currentThread());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (this.scanning) {
			stopScanning(false);
		}
		
		for (ConnectableDevice device:connectableDevices) {
			device.disconnect();
		}
		connectableDevices.clear();
		
		Log.d(Constants.TAG, "Service destroyed");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		CustomUncaughtExceptionHandler.setInterceptHandler(this, Thread.currentThread());
	}
	
	private void startService() {
		Intent intent = new Intent(this, ReadBlueService.class);
		this.startService(intent);
		this.serviceStarted = true;
		Log.d(Constants.TAG, "Service started");
	}
	
	
	public void stopService() {
		this.stopSelf();
		this.serviceStarted = false;
		Log.d(Constants.TAG, "Service stopped");
		System.gc();
	}

	synchronized
	private void startScanning() {
		if (scanning)
			stopScanning(true);
		
		if (scanningEnabled!=bluetoothAdapter.isEnabled()) {
			scanningEnabled = bluetoothAdapter.isEnabled();
			if (eventHandler!=null) {
				eventHandler.onScanningStateChanged();
			}
		}
		
		if (!bluetoothAdapter.isEnabled()) {
			if (eventHandler!=null) {
				eventHandler.onAttemptEnableBluetooth();
			}
			return;
		}
		
		IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(deviceScanningFoundReceiver, foundFilter);

		IntentFilter startFinishFilter = new IntentFilter();
		startFinishFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		startFinishFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(deviceScanningStartFinishReceiver, startFinishFilter);

		//	disconnect from currently connected devices
		for (ConnectableDevice connectableDevice:connectableDevices) {
			connectableDevice.disconnect();
		}
		connectableDevices.clear();
		if (eventHandler!=null) {
			eventHandler.onConnectableDevicesUpdated();
		}
		
		bluetoothAdapter.startDiscovery();
		scanning = true;
		
		if (eventHandler!=null) {
			eventHandler.onScanningStateChanged();
		}
		
		startService();
	}
	
	synchronized
	private void stopScanning(boolean userInvoked)
	{
		if (scanning) {
			if (userInvoked) {
				binder.addMessage("Scanning stopped.");
			} else {
				binder.addMessage("Scanning finished.");
			}

			bluetoothAdapter.cancelDiscovery();
			unregisterReceiver(deviceScanningFoundReceiver);
			unregisterReceiver(deviceScanningStartFinishReceiver);
			
			scanning = false;

			if (eventHandler!=null) {
				eventHandler.onScanningStateChanged();
			}
			
			synchronized (waitForScanningMutex) {
				waitForScanningMutex.notifyAll();
			}
			
			if (!anyDeviceConnected())
				stopService();
		}
	}
	
	private boolean anyDeviceConnected() {
		for (ConnectableDevice device:connectableDevices) {
			if (device.isConnected()) {
				return true;
			}
		}
		return false;
	}
	
	private boolean anyDeviceRecording() {
		for (ConnectableDevice device:connectableDevices) {
			if (device.isRecording()) {
				return true;
			}
		}
		return false;
	}
	
	private void toggleDeviceConnection(ConnectableDevice connectableDevice) {
		synchronized (connectableDevice) {
			if (this.scanning) {
				this.stopScanning(true);
			}
			
			if (connectableDevice.isConnected()) {
				Log.d(Constants.TAG, "Device "+connectableDevice.getDevice().getName()+" is connected. Disconnecting.");
				connectableDevice.disconnect();
			} else {
				Log.d(Constants.TAG, "Device "+connectableDevice.getDevice().getName()+" is not connected. Connecting.");
				try {
					connectableDevice.connect(binder);
				} catch (IOException e) {
					Log.e(Constants.TAG, "Error while trying to establish connection to device", e);
					binder.addMessage("Error: "+e.getMessage());
				}
			}
		}
		
	}
	
	private void playSoundAlert() {
		Uri alert = RingtoneManager.getDefaultUri(Constants.ALERT_LONG_SOUND_TYPE);
		if (alert == null) {
			Log.e(Constants.TAG, "No default URI found for alert sound type "+Constants.ALERT_LONG_SOUND_TYPE);
			return;
		}

		try {
			MediaPlayer player = new MediaPlayer();
			player.setDataSource(this, alert);
			player.setAudioStreamType(Constants.ALERT_STREAM_TYPE);
			player.setLooping(false);
			player.prepare();
			player.start();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void vibrate() {
		this.vibrator.vibrate(1000L);
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
				
				ConnectableDevice connectableDevice = new ConnectableDevice(ReadBlueService.this, bluetoothAdapter, device);
				binder.addMessage("Device Found: " + connectableDevice.toString());

				if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
					connectableDevices.add(connectableDevice);
					if (eventHandler!=null) {
						eventHandler.onConnectableDevicesUpdated();
					}
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
				binder.addMessage("Scanning Started.");
			}
			
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				stopScanning(false);
			}
		}
	};
	
	
}
