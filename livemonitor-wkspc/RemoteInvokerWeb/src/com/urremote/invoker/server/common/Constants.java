package com.urremote.invoker.server.common;

public class Constants {
	
	public static final boolean AUTHENTICATE_USER = false;

	public static final String C2DM_ACC_EMAIL = "umranium.mytracks@gmail.com";
	public static final String C2DM_ACC_PASSWORD = "?password123";
	public static final long DURATION_UPDATE_C2DM_TOKEN = 10*60*1000L;	// 10min
	
	//	Phone Comm.
	public static final String C2DM_MSG_PARAM_ACCOUNT = "account";
	public static final String C2DM_MSG_PARAM_DEVICE_ID = "deviceId";
	public static final String C2DM_MSG_PARAM_TYPE = "type";
	public static final String C2DM_MSG_PARAM_DATA = "data";
	public static final String C2DM_MSG_PARAM_LAST_UPDATE = "lastUpdate";
	
	public static final String C2DM_MSG_TYPE_STATE_UPDATE = "StateUpdate";
	public static final String C2DM_MSG_TYPE_START_RECORDING = "StartRecording";
	public static final String C2DM_MSG_TYPE_STOP_RECORDING = "StopRecording";
	
	public static final String C2DM_RECORDING_COLLAPSE_KEY = "recording";
	
}
