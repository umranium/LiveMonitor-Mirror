package com.urremote.bridge.service;

public interface InternalServiceMessageHandler {

	void onSystemMessage(String msg);
	void onCriticalError(String msg);
	void requestLaunchMyTracks();
	
	boolean isUiActive();
	void vibrate();
	void makeErrorSound();
	void showToast(String msg, int length);
	
}
