package com.urremote.bridge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.urremote.bridge.C2dmReceiver;
import com.urremote.bridge.Main;
import com.urremote.bridge.c2dm.C2dmDeviceRegistrationMessage;
import com.urremote.bridge.c2dm.C2dmDeviceUpdateMessage;
import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.CustomUncaughtExceptionHandler;
import com.urremote.bridge.common.HtmlPostUtil;
import com.urremote.bridge.common.PrimaryAccountUtil;
import com.urremote.bridge.common.HtmlPostUtil.PostResultListener;
import com.urremote.bridge.service.thread.MonitoringThread;
import com.urremote.bridge.service.thread.UploaderThread;
import com.urremote.bridge.service.thread.uploader.ActivityRestartImpl;
import com.urremote.bridge.service.thread.uploader.NoActivityRestartImpl;
import com.urremote.bridge.service.utils.ServiceForegroundUtil;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import com.urremote.bridge.R;

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
		public int getPendingUploadCount() {
			return samplingQueue.getPendingFilledInstances();
		}
		
		@Override
		public void stopService() {
			LiveMonitorService.this.stopService();
		}
		
		@Override
		public void setUiActive(boolean active) {
			LiveMonitorService.this.isUiActive = active;
			if (active || isRecording) {
				if (!monitoringThread.isLocationListenerRegistered())
					monitoringThread.registerLocationListener();
			} else {
				if (monitoringThread.isLocationListenerRegistered())
					monitoringThread.unregisterLocationListener();
			}
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
		
		private Object systemMessageMutex = new Object();
		
		@Override
		public void onCriticalError(String msg) {
			if (isRecording) {
				stopRecording();
				onSystemMessage("Critical Error: "+msg);
				CustomUncaughtExceptionHandler.reportCrash(
						LiveMonitorService.this,
						Thread.currentThread(),
						new RuntimeException("Critical Error: "+msg));
			}
		}
		
		@Override
		public void onSystemMessage(String msg) {
			synchronized (systemMessageMutex) {
				String lines[] = msg.replace("\t", "        ").split("\n");
				for (String line:lines) {
					systemMessages.addMessage(line);
				}
				
				updateHandlers.updateSystemMessages();
//				if (foregroundUtil.isForeground()) {
//					foregroundUtil.updateNotification(NOTIFICATION_TITLE, lines[lines.length-1]);
//				}
			}
		}

		@Override
		public void requestLaunchMyTracks() {
			updateHandlers.lauchMyTracks();
		}
		
		@Override
		public boolean isUiActive() {
			return isUiActive;
		};
		
		@Override
		public void vibrate() {
			Log.d(Constants.TAG, "vibrating");
			vibrator.vibrate(500L);
		}
		
		@Override
		public void makeErrorSound()
		{
			if (errorBeepPlayer.isPlaying())
				return;
			
			Log.d(Constants.TAG, "playing error sound");
			errorBeepPlayer.start();
		}
		
		@Override
		public void showToast(final String msg, final int length) {
			
			mainThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					Log.d(Constants.TAG, "showing toast");
					Toast toast = Toast.makeText(LiveMonitorService.this, msg, length);
					toast.show();
				}
			});
			
		}

	};
	
	private class ServerUpdateTimerTask extends TimerTask {
		@Override
		public void run() {
			updateC2dmRecordingState();
		}
	};
	
	private class C2dmServerEventBasedUpdater implements UpdateListener {
		
		@Override
		public void updateSystemMessages() {
			// TODO: Update C2dm server system messages
		}
		
		@Override
		public void onSystemStop() {
			Log.d(Constants.TAG, "C2dmServerEventBasedUpdater::onSystemStop");
			updateC2dmRecordingState();
		}
		
		@Override
		public void onSystemStart() {
			Log.d(Constants.TAG, "C2dmServerEventBasedUpdater::onSystemStart");
			updateC2dmRecordingState();
		}
		
		@Override
		public boolean lauchMyTracks() {
			return false;
		}
		
	};
	
	private Handler mainThreadHandler;
	private LiveMonitorBinder binder = new LiveMonitorBinder();
	private UpdateListenerCollection updateHandlers = new UpdateListenerCollection();
	private SamplingQueue samplingQueue = new SamplingQueue(Constants.SAMPLING_QUEUE_SIZE);
	private MessageContainer systemMessages = new MessageContainer(MAX_SYSTEM_MESSAGES);
	private SystemEventLogger systemEventLogger;
	private boolean isRecording = false;
	private ServiceForegroundUtil foregroundUtil;
	private MonitoringThread monitoringThread = null; 
	private UploaderThread uploaderThread = null;
	private boolean isUiActive = false;
	private Vibrator vibrator;
	private MediaPlayer errorBeepPlayer;
	
	// for C2DM
	private Timer serverUpdateTimer;
	private ServerUpdateTimerTask serverUpdateTimerTask;
	private C2dmServerEventBasedUpdater eventBasedUpdater;
	
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
			updateHandlers.onSystemStart();
			serviceMsgHandler.onSystemMessage("***** Recording Started. *****");
			startService();
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
		
		serviceMsgHandler.onSystemMessage("***** Recording Stopped. *****");
		
		updateHandlers.onSystemStop();
		
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
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onCreate() xxxx");
		
		CustomUncaughtExceptionHandler.setInterceptHandler(this, Thread.currentThread());
		
		this.systemEventLogger = new SystemEventLogger(binder);
		this.updateHandlers.add(systemEventLogger);
		
		this.mainThreadHandler = new Handler(Looper.getMainLooper());
		this.vibrator = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE); 
		this.errorBeepPlayer = MediaPlayer.create(this, R.raw.error);
		
		this.monitoringThread = new MonitoringThread(
				this, serviceMsgHandler, samplingQueue);
		this.uploaderThread = new NoActivityRestartImpl(
				this, serviceMsgHandler, samplingQueue);

		this.foregroundUtil = new ServiceForegroundUtil(this, Main.class, Constants.FOREGROUND_NOTIFICATION_ID);
		if (this.foregroundUtil.isForeground()) {
			try {
				this.startRecording();
			} catch (Exception e) {
				Log.e(Constants.TAG, "Unable to restart recording", e);
			}
		}
		
		if (Constants.ENABLE_C2DM) {
			Log.i(Constants.TAG, "C2DM IS enabled in this version");
			if (Build.VERSION.SDK_INT>=8) {
				C2dmReceiver.register(this);
				
				this.eventBasedUpdater = new C2dmServerEventBasedUpdater();
				this.updateHandlers.add(this.eventBasedUpdater);
				
				this.serverUpdateTimer = new Timer("Server-Update-Timer");
				this.serverUpdateTimerTask = new ServerUpdateTimerTask();
			this.serverUpdateTimer.schedule(serverUpdateTimerTask, 1000L, Constants.C2DM_UPDATE_SERVER_INTERVAL);
			} else {
				Log.i(Constants.TAG, "Android version doesn't support C2DM, version 2.2 or above required.");
			}
		} else {
			Log.i(Constants.TAG, "C2DM is NOT enabled in this version");
		}
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onDestroy()");
		
		if (this.foregroundUtil.isForeground())
			this.foregroundUtil.cancelForeground();
		this.monitoringThread.destroy();
		
		if (this.serverUpdateTimer!=null) {
			this.serverUpdateTimer.cancel();
			this.serverUpdateTimer.purge();
		}
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
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		serviceMsgHandler.onSystemMessage("Low Memory Detected");
	}
	
	private void updateC2dmRecordingState() {
		final boolean isRecording = binder.isRecording();
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		Set<C2dmDeviceUpdateMessage.MessageType> msgTypes = new HashSet<C2dmDeviceUpdateMessage.MessageType>();
		
		nameValuePairs.add(new BasicNameValuePair(
				Constants.C2DM_MSG_PARAM_ACCOUNT, 
				PrimaryAccountUtil.getPrimaryAccount(LiveMonitorService.this)));
		
		msgTypes.add(C2dmDeviceUpdateMessage.MessageType.RecordingStateUpdate);
		nameValuePairs.add(new BasicNameValuePair(
				C2dmDeviceUpdateMessage.MessageType.RecordingStateUpdate.toString(), 
				Boolean.toString(isRecording)));
		
		nameValuePairs.add(new BasicNameValuePair(
				Constants.C2DM_MSG_PARAM_TYPE,
				C2dmDeviceUpdateMessage.encode(msgTypes)
				));
		
		HtmlPostUtil.asyncPost(new PostResultListener() {
			@Override
			public void OnResult(int statusCode, String result) {
				if (statusCode==200)
					Log.i(Constants.TAG, "device successfully updated server ("+statusCode+") (isRecording="+isRecording+"). Result="+result);
				else
					Log.i(Constants.TAG, "Error updating server ("+statusCode+"). Result="+result);
			}
			
			@Override
			public void OnError(Throwable e) {
				if (e instanceof java.net.UnknownHostException)
					Log.e(Constants.TAG, "Failure while updating server. Internet connection problem.");
				else
					Log.e(Constants.TAG, "Failure while updating server", e);
			}
		}, 	Constants.URI_C2DM_SERVER_UPDATE, nameValuePairs);
	}

}
;