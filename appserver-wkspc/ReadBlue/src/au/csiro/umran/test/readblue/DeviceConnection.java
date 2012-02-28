package au.csiro.umran.test.readblue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import au.csiro.umran.test.readblue.blueparser.ParsedMsg;
import au.csiro.umran.test.readblue.blueparser.ParserThread;
import au.csiro.umran.test.readblue.filewriter.WriterThread;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

public class DeviceConnection {

	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final byte[] MARKER = new byte[] {(byte)0x52,(byte)0x48};
	private static final int MESSAGE_LENGTH = 9;
	private static final Object BLUETOOTH_SERVICE_OPEN_MUTEX = new Object();
	
	private static final int BUFFERED_MSG_COUNT = 500*60*5; // 5min @ 500Hz
	private static final int BUFFER_MSG_LENGTH = 32;
	
	private ReadBlueServiceBinder binder;
	private ConnectableDevice device;
	private BluetoothSocket socket;
	private boolean socketIsOpen = false;
	private boolean connectionIsClosing;
	private File deviceFile;
	private boolean recording = false;
	private ParserThread parserThread;
	private MessageReceiverThread messageReceiverThread;
	private TwoWayBlockingQueue<ParsedMsg> msgQueue;
	private WriterThread writerThread;
	
	public DeviceConnection(ReadBlueServiceBinder binder, ConnectableDevice device)
			throws IOException {
		this.binder = binder;
		this.device = device;
		this.socket = createSocket(device.getDevice());
		this.deviceFile = null;
		this.writerThread = null;
	}
	
	public ConnectableDevice getConnectableDevice() {
		return device;
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
				if (recording)
					return "recording"+((parserThread!=null)?(":"+parserThread.getParser().getLastestReadTime()):"");
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
	
	public void connect() throws IOException {
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
					socketIsOpen = true;
					Log.d(Constants.TAG, "Device connected.");
					
					return;
				} catch (IOException ioe) {
					if (ioe.getMessage().equalsIgnoreCase("Service discovery failed") && serviceDisconveryRetries<1000) {
						++serviceDisconveryRetries;
						try {
							Thread.sleep(1000L);
						} catch (InterruptedException e) {
							// ignore
						}
						continue;
					} else {
						throw ioe;
					}
				}
			}
		}
	}
	
	public void close() {
		Log.d(Constants.TAG, "Closing connection to device: "+device.getDevice().getName());
		
		connectionIsClosing = true;
		
		stopRecording();
		try {
			this.socket.close();
			socketIsOpen = false;
		} catch (IOException e) {
			Log.e(Constants.TAG, "Error while closing socket to device: "+device.getDevice().getName(), e);
		}

		Log.d(Constants.TAG, "Refresh Connectable Device Adapter");
		refreshConnectableDeviceAdapter();

	}

	public void startRecording() {
		try {
			msgQueue = new TwoWayBlockingQueue<ParsedMsg>(BUFFERED_MSG_COUNT) {
				@Override
				protected ParsedMsg getNewInstance() {
					return new ParsedMsg(BUFFER_MSG_LENGTH);
				}
			};
			openWriter();
			this.messageReceiverThread = new MessageReceiverThread(msgQueue, binder, writerThread);
			this.parserThread = new ParserThread("ParserThread:"+device.getDevice().getName(), MARKER, this, socket.getInputStream(), msgQueue);
			recording = true;
		} catch (IOException e) {
			Log.e(Constants.TAG, "Error while initializing recording: "+device.getDevice().getName(), e);
			this.close();
		} finally {
			if (!recording) {
				closeWriter();
			}
		}
	}
	
	public void stopRecording() {
		recording = false;
		if (this.parserThread!=null) {
			this.parserThread.quit();
			this.parserThread = null;
		}
		if (this.messageReceiverThread!=null) {
			this.messageReceiverThread.quit();
			this.messageReceiverThread = null;
		}
		closeWriter();
		msgQueue = null;
	}
	
	public boolean isRecording() {
		return recording;
	}
	
	private void openWriter() throws IOException {
		this.deviceFile = FileUtils.getFileForDevice(device.getDevice().getName());
		FileUtils.ensureParentFolderExists(deviceFile);
		this.writerThread = new WriterThread(this, deviceFile);
	}
	
	private void closeWriter() {
		if (this.writerThread != null) {
			Log.d(Constants.TAG, "Closing file writer");
			if (deviceFile.exists())
				displayMsgFromSepThread("Data saved at " + deviceFile);
			this.writerThread.quit();
			this.writerThread = null;
		}
	}
	
	/*
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
	*/
	
	 
	
}
