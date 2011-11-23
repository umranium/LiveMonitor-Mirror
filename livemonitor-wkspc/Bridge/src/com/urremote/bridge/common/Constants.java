package com.urremote.bridge.common;

import java.net.URI;

import android.net.Uri;

public class Constants {
	
	public static final String TAG = "livemonitor";
	
	public static final boolean IS_TESTING = false;
	
	public static final long MONITORING_INTERVAL = 1000L;
	
	public static final int MINIMUM_RECORDING_DISTANCE = 5;
	
	public static final String MY_TRACKS_PACKAGE = "com.google.android.maps.mytracks";
	public static final String MY_TRACKS_SERVICE_CLASS = "com.google.android.apps.mytracks.services.TrackRecordingService";
	public static final String MY_TRACKS_UI_CLASS = "com.google.android.apps.mytracks.MyTracks";
	
	/**
	 * The maximum duration before taking a location provided by a
	 * source with lesser accuracy
	 */
	public static final long LOCATION_MAX_UPDATE_INTERVAL = 30*1000L;
	
	public static final int SAMPLING_QUEUE_SIZE = 6*60*60; // at 1Hz, this means the queue can store for up to 5 hrs

	public static final int FOREGROUND_NOTIFICATION_ID = 1;
	
	public static final URI URI_DATA_UPLOAD = URI.create("http://www.mapmytracks.com/api/");
	
	public static final String SHARE_PREF			= "MainActivityPrefs";
	public static final String KEY_ACCOUNT_SETTINGS_AVAILABLE = "accountSettingsAvailable";
	public static final String KEY_ACTIVITY_SETTINGS_AVAILABLE = "activitySettingsAvailable";
	public static final String KEY_USERNAME			= "username";
	public static final String KEY_PASSWORD			= "password";
	public static final String KEY_ACTIVITY_TITLE	= "activity_title";
	public static final String KEY_IS_PUBLIC		= "is_public";
	public static final String KEY_ACTIVITY_TYPE	= "activity_type";
	public static final String KEY_TAGS				= "tags";

	public static final String SHOW_ACTIVITY_SETTINGS_TOO = "SHOW_ACTIVITY_SETTINGS_TOO";
	
	// MYTRACKS 
	public static final Uri URI_MYTRACKS_MARKET = Uri.parse("market://search?q="+MY_TRACKS_PACKAGE);
	
	//	MAPMYTRACKS
	public static final Uri URI_MAPMYTRACKS_REGISTER = Uri.parse("http://www.mapmytracks.com/sign-up");
	
	// C2DM
	public static final String EMAIL_C2DM_ACCOUNT = "umranium.mytracks@gmail.com";
	// C2DM - Server Comm.
//	public static final URI URI_C2DM_SERVER_REG = URI.create("http://192.168.1.10:8888/registerphone");
//	public static final URI URI_C2DM_SERVER_UPDATE = URI.create("http://192.168.1.10:8888/phoneinput");
	public static final URI URI_C2DM_SERVER_REG = URI.create("http://remote-invoker.appspot.com/registerphone");
	public static final URI URI_C2DM_SERVER_UPDATE = URI.create("http://remote-invoker.appspot.com/phoneinput");
	
	public static final String C2DM_MSG_PARAM_ACCOUNT = "account";
	public static final String C2DM_MSG_PARAM_DEVICE_ID = "deviceId";
	public static final String C2DM_MSG_PARAM_TYPE = "type";
	public static final String C2DM_MSG_PARAM_DATA = "data";
	
	public static final String C2DM_MSG_TYPE_STATE_UPDATE = "StateUpdate";
	public static final String C2DM_MSG_TYPE_START_RECORDING = "StartRecording";
	public static final String C2DM_MSG_TYPE_STOP_RECORDING = "StopRecording";
	
	//	the amount of time between c2dm server updates
	public static final long C2DM_UPDATE_SERVER_INTERVAL = 60*1000L; // 1 min
	
	public static final String KEY_CD2M_ID = "C2DM_ID";
	public static final String KEY_CD2M_ID_TIMESTAMP = "C2DM_ID_TIMESTAMP";
	
}