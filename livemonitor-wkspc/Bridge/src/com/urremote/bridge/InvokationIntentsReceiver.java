package com.urremote.bridge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.urremote.bridge.common.Constants;
import com.urremote.bridge.service.ILiveMonitorBinder;
import com.urremote.bridge.service.LiveMonitorService;

public class InvokationIntentsReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Constants.TAG, "Received intent: "+intent.getAction()+":"+(intent.getExtras()==null?"[]":intent.getExtras().keySet()));
		
		final String intentStart = context.getString(R.string.INTENT_START);
		final String intentPause = context.getString(R.string.INTENT_PAUSE);
		final String intentStop = context.getString(R.string.INTENT_STOP);
		
		if (intentStart.equals(intent.getAction())) {
			start(context.getApplicationContext());
		} else
			if (intentPause.equals(intent.getAction())) {
				final String intentExtraMyTracksOff = context.getString(R.string.INTENT_EXTRA_MYTRACKS_OFF);
				boolean mytracksOff = intent.getBooleanExtra(intentExtraMyTracksOff, true);
				Log.d(Constants.TAG, "AppContext:"+(context.getApplicationContext()!=null?"present":"absent"));
				pause(context.getApplicationContext(), mytracksOff);
			} else
				if (intentStop.equals(intent.getAction())) {
					stop(context.getApplicationContext());
				}
	}
	
	private void start(Context context) {
		new InvokeServiceManager(context, startCallback);
	}
	
	private void pause(Context context, boolean mytracksOff) {
		if (mytracksOff)
			new InvokeServiceManager(context, pauseMyTracksStopCallback);
		else
			new InvokeServiceManager(context, pauseMyTracksRecordCallback);
	}
	
	private void stop(Context context) {
		new InvokeServiceManager(context, stopCallback);
	}
	
	private final static InvokeServiceCallback startCallback = new InvokeServiceCallback() {
		@Override
		public void onConnectionReady(ILiveMonitorBinder binder) {
			try {
				binder.startRecording();
			} catch (Exception e) {
				throw new RuntimeException("Error while attempting to start/resume recording", e);
			}
		}
	};
	
	private final static InvokeServiceCallback pauseMyTracksStopCallback = new InvokeServiceCallback() {
		@Override
		public void onConnectionReady(ILiveMonitorBinder binder) {
			try {
				binder.pauseRecording(true);
			} catch (Exception e) {
				throw new RuntimeException("Error while attempting to pause recording", e);
			}
		}
	};
	
	private final static InvokeServiceCallback pauseMyTracksRecordCallback = new InvokeServiceCallback() {
		@Override
		public void onConnectionReady(ILiveMonitorBinder binder) {
			try {
				binder.pauseRecording(false);
			} catch (Exception e) {
				throw new RuntimeException("Error while attempting to pause recording", e);
			}
		}
	};
	
	private final static InvokeServiceCallback stopCallback = new InvokeServiceCallback() {
		@Override
		public void onConnectionReady(ILiveMonitorBinder binder) {
			try {
				binder.stopRecording();
			} catch (Exception e) {
				throw new RuntimeException("Error while attempting to stop recording", e);
			}
		}
	};
	
	
	private static interface InvokeServiceCallback {
		void onConnectionReady(ILiveMonitorBinder binder);
	}
	
	private static class InvokeServiceManager {
		
		private Context context;
		private InvokeServiceCallback callback;
		
		public InvokeServiceManager(Context context, InvokeServiceCallback callback) {
			this.context = context;
			this.callback = callback;
			bind();
		}
		
		private void bind() {
	    	Intent intent = new Intent(context, LiveMonitorService.class);
	        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
		}
		
		private void unbind() {
			context.unbindService(connection);
		}
		
		
	    private ServiceConnection connection = new ServiceConnection() {

	        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
	        	ILiveMonitorBinder binder = (ILiveMonitorBinder)arg1;
	        	callback.onConnectionReady(binder);
	        	unbind();
	        }

	        public void onServiceDisconnected(ComponentName arg0) {
	        	
	        }

	    };
	}
	
	
}
