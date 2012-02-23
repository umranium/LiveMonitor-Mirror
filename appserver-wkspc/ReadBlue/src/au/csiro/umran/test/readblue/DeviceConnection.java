package au.csiro.umran.test.readblue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import au.csiro.umran.test.readblue.blueparser.LengthBasedParser;
import au.csiro.umran.test.readblue.blueparser.Parser;
import au.csiro.umran.test.readblue.blueparser.MarkerBasedParser;
import au.csiro.umran.test.readblue.blueparser.OnMessageListener;
import au.csiro.umran.test.readblue.filewriter.MessageToFileWriter;
import au.csiro.umran.test.readblue.runcheck.RunningNumberChecker;

public class DeviceConnection extends Thread {

	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final byte[] MARKER = new byte[] {(byte)0x52,(byte)0x48};
	private static final int MESSAGE_LENGTH = 9;
	private static final Object BLUETOOTH_SERVICE_OPEN_MUTEX = new Object();
	
	private ReadBlueServiceBinder binder;
	private ConnectableDevice device;
	private BluetoothSocket socket;
	private boolean socketIsOpen = false;
	private boolean connectionIsClosing;
	private File deviceFile;
	private MessageToFileWriter fileWriter;
	private boolean record = false;
	private Parser blueParser;
	
	private final Object waitForThreadToExit = new Object();
	

	private OnMessageListener onMessageListener = new OnMessageListener() {
		@Override
		synchronized public void onMessage(long timeStamp, byte[] message, int length) {
			binder.addMessage(ByteUtils.bytesToString(message, 0, length));
		}
	};

	public DeviceConnection(ReadBlueServiceBinder binder, ConnectableDevice device)
			throws IOException {
		super("DeviceConnection:"+device.getDevice().getName());
		this.binder = binder;
		this.device = device;
		this.socket = createSocket(device.getDevice());
		this.deviceFile = null;
		this.fileWriter = null;

		
		this.setPriority(Thread.MAX_PRIORITY);
		
		this.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				Log.e(Constants.TAG, ex.getMessage(), ex);
			}
		});
	}
	
	public ConnectableDevice getConnectableDevice() {
		return device;
	}

	public void close() {
		Log.d(Constants.TAG, "Closing connection to device: "+device.getDevice().getName());
		
		connectionIsClosing = true;

		if (blueParser!=null) {
			blueParser.quit();
		}
		
		Log.d(Constants.TAG, "Refresh Connectable Device Adapter");
		refreshConnectableDeviceAdapter();
		
		if (isAlive() && !Thread.currentThread().equals(DeviceConnection.this)) {
			Log.d(Constants.TAG, "Waiting for thread exit");
			synchronized (waitForThreadToExit) {
				try {
					waitForThreadToExit.wait();
				} catch (InterruptedException e) {
				}
			}
		}

		Log.d(Constants.TAG, "Thread exit.");
	}

	public String getConnectionState() {
		if (connectionIsClosing) {
			if (socketIsOpen) {
				return "disconnecting";
			} else {
				return "disconnected";
			}
		} else {
			if (socketIsOpen) {
				if (record)
					return "recording"+((blueParser!=null)?(":"+blueParser.getLastestReadTime()):"");
				else
					return "connected";
			} else {
				return "connecting";
			}
		}
	}
	
	private static BluetoothSocket createSocket(BluetoothDevice device) throws IOException {
		//return device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
		return device.createRfcommSocketToServiceRecord(SPP_UUID);
	}

	private void refreshConnectableDeviceAdapter() {
		binder.deviceConnectionChanged(device);
	}

	private void displayMsgFromSepThread(final String msg) {
		binder.addMessage(msg);
	}
	
	private void connectSocket() throws IOException {
		int serviceDisconveryRetries = 0;
		synchronized (BLUETOOTH_SERVICE_OPEN_MUTEX) {
			while (!connectionIsClosing) {
				try {
						if (this.device.getAdapter().isDiscovering()) {
							Log.d(Constants.TAG, "Cancelling discovery");
							this.device.getAdapter().cancelDiscovery();
						}
						
						Log.d(Constants.TAG, "Connecting to device");
						this.socket.connect();
						Log.d(Constants.TAG, "Device connected.");
					return;
				} catch (IOException ioe) {
					if (ioe.getMessage().equalsIgnoreCase("Service discovery failed") && serviceDisconveryRetries<1000) {
						++serviceDisconveryRetries;
						Thread.yield();
						continue;
					} else {
						throw ioe;
					}
				}
			}
		}
	}
	
	public void startRecording() {
		record = true;
	}
	
	public void stopRecording() {
		record = false;
		if (blueParser!=null) {
			this.interrupt();
			blueParser.quit();
		}
	}
	
	public boolean isRecording() {
		return record;
	}
	
	private void openWriter() throws IOException {
		this.deviceFile = FileUtils.getFileForDevice(device.getDevice().getName());
		FileUtils.ensureParentFolderExists(deviceFile);
		this.fileWriter = new MessageToFileWriter(onMessageListener,
				deviceFile);
	}
	
	private void closeWriter() {
		if (this.fileWriter != null) {
			Log.d(Constants.TAG, "Closing file writer");
			try {
				displayMsgFromSepThread("Data saved at " + deviceFile);
				this.fileWriter.close();
				this.fileWriter = null;
			} catch (IOException e) {
			}
		}
	}
	
	@Override
	public void run() {
		try {
			record = false;
			
			refreshConnectableDeviceAdapter();
			
			if (binder.isScanning()) {
				try {
					final Object waitForScanningMutex = binder.getWaitForScanningMutex(); 
					synchronized (waitForScanningMutex) {
						if (binder.isScanning()) {
							waitForScanningMutex.wait();
						}
					}
				} catch (Exception e) {
					Log.e(Constants.TAG, e.getMessage(), e);
				}
			}
			
			connectSocket();
			
			if (connectionIsClosing) {
				return;
			}
			
			socketIsOpen = true;
			refreshConnectableDeviceAdapter();
			displayMsgFromSepThread("Connected to "
					+ device.getDevice().getName());
			
			boolean wasRecording = false;
			blueParser = null;
			while (!connectionIsClosing) {
				boolean isRecording = record;
				
				if (isRecording) {
					if (!wasRecording) {
						openWriter();
						
						InputStream in = this.socket.getInputStream();
						OutputStream out = this.socket.getOutputStream();
						blueParser = new LengthBasedParser(MARKER, MESSAGE_LENGTH, this, in, 1024,
								20, true, new RunningNumberChecker(this.fileWriter, 2));
						
						refreshConnectableDeviceAdapter();
					}
					
					blueParser.process();
				} else {
					if (wasRecording) {
						closeWriter();
						blueParser = null;
						
						refreshConnectableDeviceAdapter();
					}
				}
				
				wasRecording = isRecording;
				Thread.yield();
			}
			
//			this.fileWriter = new MessageToFileWriter(onMessageListener,
//					deviceFile);
//
//			try {
//				InputStream in = this.socket.getInputStream();
//				OutputStream out = this.socket.getOutputStream();
//				BlueParser blueParser = new BlueParser(MARKER, MESSAGE_LENGTH, in, 1024,
//						20, true, new RunningNumberChecker(this.fileWriter, 2));
//
//				while (!connectionIsClosing) {
//					blueParser.process();
//					Thread.yield();
//				}
//
//			} catch (Exception e) {
//				displayMsgFromSepThread("Read Error:" + e.getMessage());
//				Log.e(Constants.TAG, "Error while reading from device", e);
//			}
		} catch (IOException e1) {
			displayMsgFromSepThread("Connection Error:"
					+ e1.getMessage());
			Log.e(Constants.TAG, "Error in the device connection", e1);
		} finally {
			Log.d(Constants.TAG, "Connection thread exit for device: "+device.getDevice().getName());
			closeWriter();
			if (socketIsOpen) {
				Log.d(Constants.TAG, "Closing socket");
				try {
					socketIsOpen = false;
					Log.d(Constants.TAG, "Display disconnected msg");
					displayMsgFromSepThread("Disconnected from "
							+ device.getDevice().getName());
					Log.d(Constants.TAG, "this.socket.close()");
					this.socket.close();
				} catch (IOException e) {
					Log.e(Constants.TAG, "Error while closing socket", e);
				}
				Log.d(Constants.TAG, "Socket closed");
			}
			Log.d(Constants.TAG, "device.disconnect()");
			device.disconnect();
			Log.d(Constants.TAG, "refreshConnectableDeviceAdapter()");
			refreshConnectableDeviceAdapter();
			synchronized (waitForThreadToExit) {
				waitForThreadToExit.notifyAll();
			}
			Log.d(Constants.TAG, "Waiting threads notified");
		}
	}

}
