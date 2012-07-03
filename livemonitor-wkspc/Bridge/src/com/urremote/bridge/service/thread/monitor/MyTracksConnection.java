package com.urremote.bridge.service.thread.monitor;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.urremote.bridge.R;
import com.urremote.bridge.common.Constants;
import com.urremote.bridge.service.InternalServiceMessageHandler;
import com.urremote.bridge.service.thread.monitor.exceptions.MyTracksException;
import com.urremote.bridge.service.thread.monitor.exceptions.MyTracksNotInstalledException;
import com.urremote.bridge.service.thread.monitor.exceptions.MyTracksPermissionsNotGrantedException;

public class MyTracksConnection {
	
	private static final String MYTRACKS_WRITE_PERMISSION = "com.google.android.apps.mytracks.WRITE_TRACK_DATA"; 
	private static final String MYTRACKS_READ_PERMISSION = "com.google.android.apps.mytracks.READ_TRACK_DATA"; 
	
	private InternalServiceMessageHandler serviceMsgHandler; 
	private Context context;
	
	private ITrackRecordingService mytracks;
	private MyTracksProviderUtils myTracksProviderUtils;
	private boolean requestedTrackRecording;
	
	private final Object stateMutex = new Object();
	private boolean firstMyTracksRecording;
	private boolean wasMyTracksRecording;
	private int previousSensorState;
	private boolean isRecording;
	private boolean shouldBeRecording;
	
	private ArrayList<Sensor.SensorDataSet> bufferdSensorData = new ArrayList<Sensor.SensorDataSet>(10);
	
	public MyTracksConnection(InternalServiceMessageHandler serviceMsgHandler,
			Context context) {
		super();
		this.serviceMsgHandler = serviceMsgHandler;
		this.context = context;
		
		this.requestedTrackRecording = false;
		this.shouldBeRecording = true;
		
		resetMyTracksState();
	}

	private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        	ITrackRecordingService mytracksService = ITrackRecordingService.Stub.asInterface(arg1);
        	
        	serviceMsgHandler.onSystemMessage("Connected to MyTracks");
        	try {
				if (mytracksService.isRecording()) {
					serviceMsgHandler.onSystemMessage("MyTracks is already Recording.");
				} else {
//					Log.d(Constants.TAG, "Launching MyTracks To Start Recording");
//					Toast.makeText(context, 
//							"Please start MyTracks Recording.", 
//							Toast.LENGTH_LONG).show();
//					serviceMsgHandler.requestLaunchMyTracks();
					if (shouldBeRecording) {
						mytracksService.startNewTrack();
						requestedTrackRecording = true;
						serviceMsgHandler.onSystemMessage("Requesting MyTracks to Start New Track.");
					}
				}
			} catch (Exception e) {
				Log.e(Constants.TAG, "Error", e);
			}
        	
        	synchronized (stateMutex) {
        		mytracks = mytracksService;
        		myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
        	}
        }

        public void onServiceDisconnected(ComponentName arg0) {
        	serviceMsgHandler.onSystemMessage("Disconnected from MyTracks. MyTracks service probably crashed. Reconnecting briefly");
    		resetMyTracksState();
    		try {
    			connectToMyTracks();
    		} catch (MyTracksException exception) {
    			throw new RuntimeException("Error while trying to reconnect to MyTracks", exception);
    		}
        }

    };

    public void connectToMyTracks() throws MyTracksNotInstalledException, MyTracksPermissionsNotGrantedException {
    	if (!isMyTracksInstalled(context)) {
    		throw new MyTracksNotInstalledException(context);
    	}
    	if (!hasMyTracksPermission(context)) {
    		throw new MyTracksPermissionsNotGrantedException(context);
    	}
    	serviceMsgHandler.onSystemMessage("Connecting to MyTracks");
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(
				Constants.MY_TRACKS_PACKAGE,
				Constants.MY_TRACKS_SERVICE_CLASS));
		if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
			throw new RuntimeException("Unable to find MyTracks service. MyTracks may not be installed.");
		} else {
			Log.i(Constants.TAG, "Connecting to MyTracks service");
		}
	}
	
	public void disconnectFromMyTracks() {
		synchronized (stateMutex) {
			if (mytracks!=null && requestedTrackRecording) {
				try {
					if (mytracks.isRecording()) {
						mytracks.endCurrentTrack();
						serviceMsgHandler.onSystemMessage("Requesting MyTracks to End Current Track.");
					}
				} catch (RemoteException e) {
					Log.e(Constants.TAG, "Error while stopping MyTracks Recording", e);
				}
			}
			
			context.unbindService(connection);
			resetMyTracksState();
	    	serviceMsgHandler.onSystemMessage("Disconnected from MyTracks.");
		}
	}
	
	public boolean isConnected() {
		return mytracks!=null;
	}
	
	private void resetMyTracksState() {
    	synchronized (stateMutex) {
			mytracks = null;
			myTracksProviderUtils = null;
			firstMyTracksRecording = true;
			wasMyTracksRecording = false;
			previousSensorState = -1;
			isRecording = false;
    	}
	}
    
	public void updateState() {
		synchronized (stateMutex) {
			try {
				if (mytracks!=null) {
					isRecording = mytracks.isRecording();
					
					if (isRecording) {
						int state = mytracks.getSensorState();
						Sensor.SensorState sensorState = Sensor.SensorState.valueOf(state);
						if (sensorState==null)
							sensorState = Sensor.SensorState.NONE;
						
						if (previousSensorState!=state) {
							Log.d(Constants.TAG, "sensorState="+sensorState);
							serviceMsgHandler.onSystemMessage("MyTrack Sensor State changed to '"+sensorState+"'");
							previousSensorState = state;
						}
						
						if (sensorState!=null  && sensorState.equals(SensorState.CONNECTED)) {
							byte[] sensorData = mytracks.getSensorData();
							if (sensorData!=null) {
								Sensor.SensorDataSet sensorDataSet = Sensor.SensorDataSet.parseFrom(sensorData);
								bufferdSensorData.add(sensorDataSet);
							}
						}
					} else {
						if (shouldBeRecording) {
							serviceMsgHandler.onSystemMessage(
									"MyTracks is not recording. " +
									"Please enable sharing in MyTracks. " +
									"Go to Menu > More > Settings > Sharing > Allow access");
							mytracks.startNewTrack();
							requestedTrackRecording = true;
						}
					}
					
					if (firstMyTracksRecording && isRecording) {
						serviceMsgHandler.onSystemMessage("MyTracks is Recording.");
						firstMyTracksRecording = false;
						wasMyTracksRecording = true;
					}
					
					if (wasMyTracksRecording && !isRecording) {
						serviceMsgHandler.onSystemMessage("MyTracks stopped Recording.");
						firstMyTracksRecording = true;
						wasMyTracksRecording = false;
						serviceMsgHandler.onSystemMessage("Restarting MyTracks Recording.");
						mytracks.startNewTrack();
						requestedTrackRecording = true;
					}
				} else {
					isRecording = false;
				}
			} catch (RemoteException e) {
				Log.d(Constants.TAG, "Error while retrieving MyTracks data", e);
			} catch (InvalidProtocolBufferException e) {
				Log.d(Constants.TAG, "Error while retrieving MyTracks data", e);
			}
		}
	}
	
	public boolean isRecording() {
		return isRecording;
	}
	
	public boolean hasSensorData() {
		boolean has;
		synchronized (stateMutex) {
			has = !bufferdSensorData.isEmpty();
		}
		return has;
	}
	
	public void retrieveSensorData(List<Sensor.SensorDataSet> output) {
		synchronized (stateMutex) {
			output.addAll(bufferdSensorData);
			bufferdSensorData.clear();
		}
	}
	
	public boolean getShouldBeRecording() {
		return shouldBeRecording;
	}
	
	public void setShouldBeRecording(boolean shouldBeRecording) {
		if (this.shouldBeRecording!=shouldBeRecording) {
			this.shouldBeRecording = shouldBeRecording;
			
			if (mytracks!=null) {
				if (this.shouldBeRecording) {
					try {
						if (!mytracks.isRecording()) {
							mytracks.startNewTrack();
							serviceMsgHandler.onSystemMessage("Requesting MyTracks to Start New Track.");
						}
					} catch (RemoteException e) {
						Log.d(Constants.TAG, "Error while retrieving MyTracks data", e);
					}
				} else {
					try {
						if (mytracks.isRecording()) {
							mytracks.endCurrentTrack();
							serviceMsgHandler.onSystemMessage("Requesting MyTracks to End Current Track.");
						}
					} catch (RemoteException e) {
						Log.d(Constants.TAG, "Error while retrieving MyTracks data", e);
					}
				}
			}
		}
	}
	
	public static boolean hasMyTracksPermission(Context c) {
		return c.checkPermission(MYTRACKS_READ_PERMISSION, Process.myPid(), Process.myUid())==PackageManager.PERMISSION_GRANTED && 
				c.checkPermission(MYTRACKS_WRITE_PERMISSION, Process.myPid(), Process.myUid())==PackageManager.PERMISSION_GRANTED;
	}
	
//	public static boolean isMyTracksInstalled(Context c) {
//		try {
//			c.getPackageManager().getPackageInfo( Constants.MY_TRACKS_PACKAGE, 0 );
//			return true;
//		} catch (NameNotFoundException e) {
//			return false;
//		}
//	}
	
	public static boolean isMyTracksInstalled(Context c) {
		List<PackageInfo> packs = c.getPackageManager().getInstalledPackages(0);
		for (PackageInfo pack:packs) {
			if (pack.packageName.equals(Constants.MY_TRACKS_PACKAGE)) {
				return true;
			}
		}
		return false;
	}

}
