package au.csiro.umran.test.readblue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import au.csiro.umran.test.readblue.blueparser.BlueParser;
import au.csiro.umran.test.readblue.blueparser.OnMessageListener;
import au.csiro.umran.test.readblue.filewriter.MessageToFileWriter;
import au.csiro.umran.test.readblue.runcheck.RunningNumberChecker;

public class DeviceConnection extends Thread {

	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final byte[] MARKER = new byte[] {(byte)0x52,(byte)0x48};
	
	private ReadBlueServiceBinder binder;
	private ConnectableDevice device;
	private BluetoothSocket socket;
	private boolean socketIsOpen = false;
	private boolean connectionIsClosing;
	private File deviceFile;
	private MessageToFileWriter fileWriter;
	
	private final Object waitForThreadToExit = new Object();

	private OnMessageListener onMessageListener = new OnMessageListener() {
		@Override
		synchronized public void onMessage(byte[] message, int length) {
			binder.addMessage(ByteUtils.bytesToString(message, 0, length));
		}
	};

	public DeviceConnection(ReadBlueServiceBinder binder, ConnectableDevice device)
			throws IOException {
		this.binder = binder;
		this.device = device;
		this.socket = getSocket();
		this.deviceFile = FileUtils.getFileForDevice(device.getDevice().getName());
		this.fileWriter = null;

		FileUtils.ensureParentFolderExists(deviceFile);
		
		this.setPriority(Thread.MAX_PRIORITY);
		
		this.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				Log.e(Constants.TAG, ex.getMessage(), ex);
			}
		});
	}

	public void close() {
		Log.d(Constants.TAG, "Closing connection to device: "+device.getDevice().getName());
		
		connectionIsClosing = true;
		refreshConnectableDeviceAdapter();
		if (isAlive()) {
			synchronized (waitForThreadToExit) {
				try {
					waitForThreadToExit.wait();
				} catch (InterruptedException e) {
				}
			}
		}
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
				return "connected";
			} else {
				return "connecting";
			}
		}
	}
	
	private BluetoothSocket getSocket() throws IOException {
		return device.getDevice().createRfcommSocketToServiceRecord(SPP_UUID);
	}

	private void refreshConnectableDeviceAdapter() {
		binder.deviceConnectionChanged(device);
	}

	private void displayMsgFromSepThread(final String msg) {
		binder.addMessage(msg);
	}
	

	@Override
	public void run() {
		try {
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
			
			
			this.socket.connect();
			socketIsOpen = true;
			refreshConnectableDeviceAdapter();
			displayMsgFromSepThread("Connected to "
					+ device.getDevice().getName());
			
			this.fileWriter = new MessageToFileWriter(onMessageListener,
					deviceFile);

			try {
				InputStream in = this.socket.getInputStream();
				OutputStream out = this.socket.getOutputStream();
				BlueParser blueParser = new BlueParser(MARKER, in, 1024,
						20, true, new RunningNumberChecker(this.fileWriter, 2));

				while (!connectionIsClosing) {
					blueParser.process();
				}

			} catch (Exception e) {
				displayMsgFromSepThread("Read Error:" + e.getMessage());
				Log.e(Constants.TAG, "Error while reading from device", e);
			}
		} catch (IOException e1) {
			displayMsgFromSepThread("Error Openning Connection:"
					+ e1.getMessage());
			Log.e(Constants.TAG, "Error while connecting to device", e1);
		} finally {
			Log.d(Constants.TAG, "Connection thread exit for device: "+device.getDevice().getName());
			if (this.fileWriter != null) {
				try {
					displayMsgFromSepThread("Data saved at " + deviceFile);
					this.fileWriter.close();
				} catch (IOException e) {
				}
			}
			try {
				socketIsOpen = false;
				displayMsgFromSepThread("Disconnected from "
						+ device.getDevice().getName());
				this.socket.close();
				device.disconnect();
				refreshConnectableDeviceAdapter();
			} catch (IOException e) {
			}
			synchronized (waitForThreadToExit) {
				waitForThreadToExit.notifyAll();
			}
		}
	}

}
