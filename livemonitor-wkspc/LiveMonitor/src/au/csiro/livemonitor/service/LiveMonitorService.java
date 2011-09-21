package au.csiro.livemonitor.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import au.csiro.livemonitor.Main;
import au.csiro.livemonitor.R;
import au.csiro.livemonitor.common.Constants;
import au.csiro.livemonitor.common.DefSettings;
import au.csiro.livemonitor.mapmymaps.MapMyTracksInterfaceApi;
import au.csiro.livemonitor.service.thread.MonitoringThread;
import au.csiro.livemonitor.service.thread.UploaderThread;
import au.csiro.livemonitor.service.utils.ServiceForegroundUtil;

public class LiveMonitorService extends Service {
	
	private static final int MAX_SYSTEM_MESSAGES = 100;
    
	private static String NOTIFICATION_TITLE = "LiveMonitor";
	private static String NOTIFICATION_TEXT = "LiveMonitor Started";
	private static String NOTIFICATION_TICKER = "LiveMonitor Started";
	
	private class LiveMonitorBinder extends Binder implements ILiveMonitorBinder {
		
		@Override
		public boolean isStarted() {
			return LiveMonitorService.this.foregroundUtil.isForeground();
		}
		
		@Override
		public void stopService() {
			LiveMonitorService.this.stopService();
		}
		
		@Override
		public void setUiActive(boolean active) {
			LiveMonitorService.this.isUiActive = active;
		}

		@Override
		public boolean isRecording() {
			return LiveMonitorService.this.isRecording;
		}

		@Override
		public void startRecording() throws Exception {
			LiveMonitorService.this.startRecording();
		}

		@Override
		public void stopRecording() {
			LiveMonitorService.this.stopRecording();
		}

		@Override
		public void registerUpdateListener(UpdateListener handler) {
			LiveMonitorService.this.updateHandlers.add(handler);
		}
		
		@Override
		public void unregisterUpdateListener(UpdateListener handler) {
			LiveMonitorService.this.updateHandlers.remove(handler);
		}

		@Override
		public List<SystemMessage> getSystemMessages() {
			return systemMessages.getMessages();
		}

	}
	
	
	private InternalServiceMessageHandler serviceMsgHandler = new InternalServiceMessageHandler() {
		@Override
		public void onCriticalError(String msg) {
			if (isRecording) {
				stopRecording();
				onSystemMessage("Error: "+msg);
			}
		}

		@Override
		public void onSystemMessage(String msg) {
			systemMessages.addMessage(msg);
			
			updateHandlers.updateSystemMessages();
			if (foregroundUtil.isForeground()) {
				foregroundUtil.updateNotification(NOTIFICATION_TITLE, msg);
			}
		}

		@Override
		public void requestLaunchMyTracks() {
			updateHandlers.lauchMyTracks();
		}
		
	};
	
	private LiveMonitorBinder binder = new LiveMonitorBinder();
	private boolean isRecording = false;
	private UpdateListenerCollection updateHandlers = new UpdateListenerCollection();
	private ServiceForegroundUtil foregroundUtil;
	private SamplingQueue samplingQueue = new SamplingQueue(Constants.SAMPLING_QUEUE_SIZE);
	private MonitoringThread monitoringThread = null; 
	private UploaderThread uploaderThread = null;
	private MessageContainer systemMessages = new MessageContainer(MAX_SYSTEM_MESSAGES);
	private boolean isUiActive = false;

	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onBind()");
		return binder;
	}
	
	synchronized
	private void startRecording() throws Exception {
		if (isRecording) return;
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":startRecording()");
		
		serviceMsgHandler.onSystemMessage("Starting Recording...");
		try {
			monitoringThread.begin();
			uploaderThread.begin();
			isRecording = true;
			startService();
			serviceMsgHandler.onSystemMessage("Recording Started.");
		} catch (Exception e) {
			serviceMsgHandler.onSystemMessage("Error:"+e.getMessage());
			throw e;
		}
	}
	
	synchronized
	private void stopRecording() {
		if (!isRecording) return;
		
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":stopRecording()");
		
		serviceMsgHandler.onSystemMessage("Stopping Recording...");
		
		monitoringThread.quit();
		uploaderThread.quit();
		isRecording = false;
		
		serviceMsgHandler.onSystemMessage("Recording Stopped.");
		
		if (isUiActive)
			stopService();
	}
	
	private void startService() {
		Intent intent = new Intent(this, LiveMonitorService.class);		
		this.startService(intent);
	}
	
	private void stopService() {
		if (foregroundUtil.isForeground()) {
			foregroundUtil.cancelForeground();
		}
		
		this.stopSelf();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onCreate()");
		this.monitoringThread = new MonitoringThread(
				this, serviceMsgHandler, samplingQueue);
		this.uploaderThread = new UploaderThread(
				this, serviceMsgHandler, samplingQueue);
		this.foregroundUtil = new ServiceForegroundUtil(this, Main.class, Constants.FOREGROUND_NOTIFICATION_ID);
		if (this.foregroundUtil.isForeground()) {
			try {
				this.startRecording();
			} catch (Exception e) {
				Log.e(Constants.TAG, "Unable to restart recording", e);
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onDestroy()");
		if (this.foregroundUtil.isForeground())
			this.foregroundUtil.cancelForeground();
		this.monitoringThread.destroy();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onStart()");

		foregroundUtil.setToForeground(
			R.drawable.icon,
			NOTIFICATION_TICKER,
			NOTIFICATION_TITLE,
			NOTIFICATION_TEXT
		);
	}

}
