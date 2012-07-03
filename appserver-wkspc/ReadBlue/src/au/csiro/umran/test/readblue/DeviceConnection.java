package au.csiro.umran.test.readblue;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import au.csiro.umran.test.readblue.blueparser.ParserThread;
import au.csiro.umran.test.readblue.filewriter.Writer;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

public class DeviceConnection {

	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final byte[] MARKER = new byte[] {(byte)0x52,(byte)0x48};
	private static final int MESSAGE_LENGTH = 9;
	private static final Object BLUETOOTH_SERVICE_OPEN_MUTEX = new Object();
	private static final TreeSet<String> IGNORABLE_ERRORS = new TreeSet<String>() {
		{
			this.add("Service discovery failed");
			this.add("Unable to start Service Discovery");
		}
	};
	
	private Context context;
	private ReadBlueServiceBinder binder;
	private ConnectableDevice device;
	private BluetoothSocket socket;
	private boolean socketIsOpen = false;
	private boolean connectionIsClosing;
	private File deviceFile;
	private boolean recording = false;
	private boolean calibration = false;
	private ParsedMsgQueue msgQueue;
	private ParserThread parserThread;
	private MessageReceiverThread messageReceiverThread;
	private Writer writer;
	private String marker;
	
	public DeviceConnection(Context context, ReadBlueServiceBinder binder, ConnectableDevice device) {
		this.context = context;
		this.binder = binder;
		this.device = device;
		this.socket = null;
		this.deviceFile = null;
		this.writer = null;
		this.msgQueue = new ParsedMsgQueue();
		this.marker = "";
	}
	
	@Override
	protected void finalize() throws Throwable {
		Log.v(Constants.TAG, "DeviceConnection Finalized");
		if (socket!=null && socketIsOpen) {
			Log.v(Constants.TAG, "Socket closed");
			socket.close();
		}
		super.finalize();
	}
	
	public ConnectableDevice getConnectableDevice() {
		return device;
	}
	
	public void setMarker(String marker) {
		this.marker = marker;
		if (messageReceiverThread!=null)
			messageReceiverThread.setMarker(marker);
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
					return ((parserThread!=null)?(parserThread.getParser().getLastestReadTime()+"-"+marker):"recording");
				else
					return "connected";
			} else {
				return "connecting";
			}
		}
	}
	
	public boolean isCalibrating() {
		return calibration;
	}
	
	private static BluetoothSocket createSocket(BluetoothDevice device) throws IOException {
		return device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
	}

	private void refreshConnectableDeviceAdapter() {
		binder.deviceConnectionChanged(device);
	}

	private void displayMsgFromSepThread(final String msg) {
		binder.addMessage(msg);
	}
	
	public void connect() {
		connect(false,false);
	}
	
	public void connect(final boolean startRecording, final boolean doCalibration) {
		Thread th = new Thread("AsyncConnectThread") {
			public void run() {
				CustomUncaughtExceptionHandler.setInterceptHandler(context, this);
				
				connectionIsClosing = false;
				
				try {
					asyncConnect(startRecording,doCalibration);
				} catch (IOException e) {
					Log.e(Constants.TAG, "Error while trying to establish connection to device", e);
					binder.addMessage("Error: "+e.getMessage());
				}
			};
		};
		
		th.start();
	}
	
	private void asyncConnect(boolean startRecording, boolean doCalibration) throws IOException {
		int serviceDisconveryRetries = 0;
		synchronized (BLUETOOTH_SERVICE_OPEN_MUTEX) {
			while (!connectionIsClosing) {
				try {
					if (this.device.getAdapter().isDiscovering()) {
						Log.d(Constants.TAG, "Cancelling discovery");
						this.device.getAdapter().cancelDiscovery();
					}
					
					Log.d(Constants.TAG, "Connecting to device");
					if (this.socket==null) {
						this.socket = createSocket(device.getDevice());
					}
					
					this.socket.connect();
					
					if (this.socket.getInputStream()==null) {
						this.socket.close();
						this.socket = null;
						continue;
					}
					
					socketIsOpen = true;
					Log.d(Constants.TAG, "Device connected.");
					
					refreshConnectableDeviceAdapter();
					
					if (startRecording) {
						startRecording(doCalibration);
					}
					
					return;
				} catch (IOException ioe) {
					if (IGNORABLE_ERRORS.contains(ioe.getMessage()) && serviceDisconveryRetries<1000) {
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
			if (this.socket!=null) {
				this.socket.close();
				this.socket = null;
				socketIsOpen = false;
			}
		} catch (IOException e) {
			Log.e(Constants.TAG, "Error while closing socket to device: "+device.getDevice().getName(), e);
		}

		Log.d(Constants.TAG, "Refresh Connectable Device Adapter");
		refreshConnectableDeviceAdapter();

	}
	
	public void startRecording(boolean doCalibration) {
		try {
			while (msgQueue.peekFilledInstance()!=null) {
				try {
					ParsedMsg msg = msgQueue.takeFilledInstance();
					msgQueue.returnEmptyInstance(msg);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			msgQueue.assertAllAvailable();
			openWriter(doCalibration);
			this.messageReceiverThread = new MessageReceiverThread(context, this, msgQueue, binder, writer, doCalibration, marker);
			this.parserThread = new ParserThread(context, "ParserThread:"+device.getDevice().getName(), MARKER, 
					binder, this, socket.getInputStream(), msgQueue);
			this.calibration = doCalibration;
			this.recording = true;
			refreshConnectableDeviceAdapter();
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
		calibration = false;
		if (this.parserThread!=null) {
			this.parserThread.quit();
			this.parserThread = null;
		}
		if (this.messageReceiverThread!=null) {
			this.messageReceiverThread.quit();
			this.messageReceiverThread = null;
		}
		closeWriter();
		refreshConnectableDeviceAdapter();
	}
	
	public boolean isRecording() {
		return recording;
	}
	
	private void openWriter(boolean doCalibration) throws IOException {
		this.deviceFile = FileUtils.getFileForDevice(doCalibration, device.getDevice().getName());
		FileUtils.ensureParentFolderExists(deviceFile);
		this.writer = new Writer(this, deviceFile);
	}
	
	private void closeWriter() {
		if (this.writer != null) {
			Log.d(Constants.TAG, "Closing file writer");
			if (deviceFile.exists())
				displayMsgFromSepThread("Data saved at " + deviceFile);
			this.writer.close();
			this.writer = null;
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
