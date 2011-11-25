package com.urremote.bridge;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.urremote.bridge.c2dm.C2dmDeviceRegistrationMessage;
import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.HtmlPostUtil;
import com.urremote.bridge.common.HtmlPostUtil.PostResultListener;
import com.urremote.bridge.common.PrimaryAccountUtil;
import com.urremote.bridge.mapmymaps.ReusableBasicNameValuePair;
import com.urremote.bridge.service.ILiveMonitorBinder;
import com.urremote.bridge.service.LiveMonitorService;

public class C2dmReceiver extends BroadcastReceiver {
	
	private static final String INTENT_C2DM_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";
	private static final String INTENT_C2DM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
	private static final String INTENT_REGISTER_C2DM = "com.google.android.c2dm.intent.REGISTER";
	private static final String INTENT_UNREGISTER_C2DM = "com.google.android.c2dm.intent.UNREGISTER";
	
	private static final String EXTRA_C2DM_REG_REQUEST_PENDING_INTENT = "app"; 
	private static final String EXTRA_C2DM_REG_REQUEST_SENDER_ACCOUNT = "sender";
	
	private static final String EXTRA_C2DM_REG_REPLY_REG_ID = "registration_id"; 
	private static final String EXTRA_C2DM_REG_REPLY_ERROR = "error"; 
	private static final String EXTRA_C2DM_REG_REPLY_UNREGISTERED = "unregistered"; 

	public static final String ERROR_CODE_SERVICE_NOT_AVAIL = "SERVICE_NOT_AVAILABLE";
	public static final String ERROR_CODE_ACCOUNT_MISSING = "ACCOUNT_MISSING";
	public static final String ERROR_CODE_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
	public static final String ERROR_CODE_TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";
	public static final String ERROR_CODE_INVALID_SENDER = "INVALID_SENDER";
	public static final String ERROR_CODE_PHONE_REGISTRATION_ERROR	 = "PHONE_REGISTRATION_ERROR";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Constants.ENABLE_C2DM) {
			if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
				Log.d(Constants.TAG, "Received Boot Completed");
				register(context);
			} else
			if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
				Log.d(Constants.TAG, "Received Shutdown");
				unregister(context);
			} else
			if (INTENT_C2DM_REGISTRATION.equals(intent.getAction())) {
				Log.d(Constants.TAG, "Received C2DM Registration Info");
				
				String registrationId = intent.getStringExtra(EXTRA_C2DM_REG_REPLY_REG_ID);
				String error = intent.getStringExtra(EXTRA_C2DM_REG_REPLY_ERROR);
				String unregistered = intent.getStringExtra(EXTRA_C2DM_REG_REPLY_UNREGISTERED);
				
				Log.i(Constants.TAG, "C2DM: registrationId = " + registrationId + ", error = " + error);
				
				if (error!=null) {
					handleRegistrationError(context, error);
				} if (unregistered!=null) {
			        SharedPreferences preferences = context.getSharedPreferences(Constants.SHARE_PREF, Context.MODE_PRIVATE);
			        Editor editor = preferences.edit();
			        editor.remove(Constants.KEY_CD2M_ID);
			        editor.commit();
			        
					sendUnregisterServer(context);
				} else {
			        SharedPreferences preferences = context.getSharedPreferences(Constants.SHARE_PREF, Context.MODE_PRIVATE);
			        Editor editor = preferences.edit();
			        editor.putString(Constants.KEY_CD2M_ID, registrationId);
			        editor.commit();
			        
					sendRegisterServer(context, registrationId);
				}
				
			} else
			if (INTENT_C2DM_RECEIVE.equals(intent.getAction())) {
				Log.d(Constants.TAG, "Received C2DM Message");
				
				Bundle bundle = intent.getExtras();
				handleMessage(context, bundle);
			}
		}
	}
	
	public static void register(Context context) {
		if (Constants.ENABLE_C2DM) {
			Log.d(Constants.TAG, "Sending C2DM Registration Request");
			Intent intent = new Intent(INTENT_REGISTER_C2DM);
			intent.putExtra(EXTRA_C2DM_REG_REQUEST_PENDING_INTENT,
					PendingIntent.getBroadcast(context, 0, new Intent(), 0));
			intent.putExtra(EXTRA_C2DM_REG_REQUEST_SENDER_ACCOUNT,
					Constants.EMAIL_C2DM_ACCOUNT);
			context.startService(intent);
		}
	}

	public static void unregister(Context context) {
		if (Constants.ENABLE_C2DM) {
			Log.d(Constants.TAG, "Sending C2DM Unregistration Request");
			Intent intent = new Intent(INTENT_UNREGISTER_C2DM);
			intent.putExtra(EXTRA_C2DM_REG_REQUEST_PENDING_INTENT,
					PendingIntent.getBroadcast(context, 0, new Intent(), 0));
			context.startService(intent);
		}
	}
	
	private static void sendRegisterServer(final Context context, final String registrationId) {
		NameValuePair account = new BasicNameValuePair(Constants.C2DM_MSG_PARAM_ACCOUNT, 
				PrimaryAccountUtil.getPrimaryAccount(context));
		NameValuePair type = new BasicNameValuePair(Constants.C2DM_MSG_PARAM_TYPE, 
				C2dmDeviceRegistrationMessage.MessageType.RegisterDevice.toString());
		NameValuePair deviceId = new BasicNameValuePair(Constants.C2DM_MSG_PARAM_DEVICE_ID, 
				registrationId);
		
		HtmlPostUtil.asyncPost(new PostResultListener() {
			@Override
			public void OnResult(int statusCode, String result) {
				if (statusCode==200)
					Log.i(Constants.TAG, "device successfully registered with server ("+statusCode+"): "+result);
				else
					Log.i(Constants.TAG, "Error registering with server ("+statusCode+"): "+result);
			}
			
			@Override
			public void OnError(Throwable e) {
				Log.e(Constants.TAG, "Failure while registering device with server", e);
//				if (e instanceof org.apache.http.conn.HttpHostConnectException) {
//					try {
//						new DelayedInvoke(context, 1000L, C2dmReceiver.class, null, 
//								"sendRegisterServer", new Class<?>[]{Context.class,String.class},
//								context, registrationId);
//						Log.i(Constants.TAG, "Retrying registering to server shortly");
//					} catch (Exception e1) {
//						Log.e(Constants.TAG, "Error while delayed invoking register to server", e1);
//					}
//				}
			}
		}, 	Constants.URI_C2DM_SERVER_REG, Arrays.asList(account, type, deviceId));
	}
	
	private static void sendUnregisterServer(final Context context) {
		NameValuePair account = new BasicNameValuePair(Constants.C2DM_MSG_PARAM_ACCOUNT, 
				PrimaryAccountUtil.getPrimaryAccount(context));
		NameValuePair type = new BasicNameValuePair(Constants.C2DM_MSG_PARAM_TYPE, 
				C2dmDeviceRegistrationMessage.MessageType.UnregisterDevice.toString());
		
		HtmlPostUtil.asyncPost(new PostResultListener() {
			@Override
			public void OnResult(int statusCode, String result) {
				if (statusCode==200)
					Log.i(Constants.TAG, "device successfully unregistered from server ("+statusCode+"): "+result);
				else
					Log.i(Constants.TAG, "Error unregistering from server ("+statusCode+"): "+result);
			}
			
			@Override
			public void OnError(Throwable e) {
				Log.e(Constants.TAG, "Failure while registering device with server", e);
			}
			
		}, 	Constants.URI_C2DM_SERVER_REG, Arrays.asList(account, type));
	}
	
	private static void handleRegistrationError(Context context, String error) {
		String msg = "Error registering device for c2dm:\n"+ error;
		Log.e(Constants.TAG, msg);
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
	
	private static void handleMessage(Context context, Bundle message) {
		Log.i(Constants.TAG, "Received C2DM Message: ");
		for (String key:message.keySet()) {
			Log.i(Constants.TAG, "C2DM: "+key+"="+message.getString(key));
		}
		
		String messageType = message.getString(Constants.C2DM_MSG_PARAM_TYPE);
		
		if (Constants.C2DM_MSG_TYPE_START_RECORDING.equals(messageType)) {
			// start recording
			new InvokeService(context.getApplicationContext(), new InvokeServiceCallback() {
				@Override
				public void onConnectionReady(ILiveMonitorBinder binder) {
					if (!binder.isRecording()) {
						try {
							binder.startRecording();
							Log.i(Constants.TAG, "Recording started (C2DM invokation)");
						} catch (Exception e) {
							Log.e(Constants.TAG, "Error while attempting to start recording (C2DM invokation)", e);
						}
					}
				}
			});
		} else
			if (Constants.C2DM_MSG_TYPE_STOP_RECORDING.equals(messageType)) {
				new InvokeService(context.getApplicationContext(), new InvokeServiceCallback() {
					@Override
					public void onConnectionReady(ILiveMonitorBinder binder) {
						if (binder.isRecording()) {
							try {
								binder.stopRecording();
								Log.i(Constants.TAG, "Recording stopped (C2DM invokation)");
							} catch (Exception e) {
								Log.e(Constants.TAG, "Error while attempting to start recording (C2DM invokation)", e);
							}
						}
					}
				});
			}
	}
	
	private static interface InvokeServiceCallback {
		void onConnectionReady(ILiveMonitorBinder binder);
	}
	
	private static class InvokeService {
		
		private Context context;
		private InvokeServiceCallback callback;
		
		public InvokeService(Context context, InvokeServiceCallback callback) {
			this.context = context;
			this.callback = callback;
			
	    	Intent intent = new Intent(context, LiveMonitorService.class);
	        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
		}
		
	    private ServiceConnection connection = new ServiceConnection() {

	        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
	        	ILiveMonitorBinder binder = (ILiveMonitorBinder)arg1;
	        	
	        	callback.onConnectionReady(binder);
	        	
	        	context.unbindService(this);
	        }

	        public void onServiceDisconnected(ComponentName arg0) {
	        }

	    };
	}
	
	
	private static class DelayedInvoke implements Runnable {
		
		private Object _this;
		private Method method;
		private Object[] args;
		
		public DelayedInvoke(Context context, long delay, Class<?> _class, Object _this, String methodName, Class<?>[] paramTypes, Object ... args) throws SecurityException, NoSuchMethodException {
			this._this = _this;
			this.method = _class.getMethod(methodName, paramTypes);
			this.args = args;
			
			Context appContext = context.getApplicationContext();
			Handler mainLooperHandler = new Handler(appContext.getMainLooper());
			mainLooperHandler.postDelayed(this, delay);
		}
		
		@Override
		public void run() {
			try {
				
				method.invoke(_this, args);
				
			} catch (Exception e) {
				Log.e(Constants.TAG, "Exception while trying to run method "+method, e);
			}
		}
		
	}
	
	
}
