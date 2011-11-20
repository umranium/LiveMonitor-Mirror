package com.urremote.bridge;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.urremote.bridge.common.Constants;

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
		if (INTENT_C2DM_REGISTRATION.equals(intent.getAction())) {
			Log.d(Constants.TAG, "Received C2DM Registration Info");
			
			String registrationId = intent.getStringExtra(EXTRA_C2DM_REG_REPLY_REG_ID);
			String error = intent.getStringExtra(EXTRA_C2DM_REG_REPLY_ERROR);
			String unregistered = intent.getStringExtra(EXTRA_C2DM_REG_REPLY_UNREGISTERED);
			
			Log.i(Constants.TAG, "C2DM: registrationId = " + registrationId + ", error = " + error);
			
			if (error!=null) {
				handleRegistrationError(error);
			} if (unregistered!=null) {
				handleSuccessfulUnregistration();
			} else {
				sendRegIdToServer(registrationId);
			}
			
		} else
		if (INTENT_C2DM_RECEIVE.equals(intent.getAction())) {
			Log.d(Constants.TAG, "Received C2DM Message");
			
			Bundle bundle = intent.getExtras();
			handleMessage(bundle);
		}
	}
	
	public static void register(Context context) {
		Log.d(Constants.TAG, "Sending C2DM Registration Request");
		Intent intent = new Intent(INTENT_REGISTER_C2DM);
		intent.putExtra(EXTRA_C2DM_REG_REQUEST_PENDING_INTENT,
				PendingIntent.getBroadcast(context, 0, new Intent(), 0));
		intent.putExtra(EXTRA_C2DM_REG_REQUEST_SENDER_ACCOUNT,
				Constants.EMAIL_C2DM_ACCOUNT);
		context.startService(intent);
	}

	public static void unregister(Context context) {
		Log.d(Constants.TAG, "Sending C2DM Unregistration Request");
		Intent intent = new Intent(INTENT_UNREGISTER_C2DM);
		intent.putExtra(EXTRA_C2DM_REG_REQUEST_PENDING_INTENT,
				PendingIntent.getBroadcast(context, 0, new Intent(), 0));
		context.startService(intent);
	}
	
	public void sendRegIdToServer(String registrationId) {
	}
	
	public void handleRegistrationError(String error) {
	}
	
	public void handleSuccessfulUnregistration() {
	}
	
	public void handleMessage(Bundle message) {
	}
	
}
