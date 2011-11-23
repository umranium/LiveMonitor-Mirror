package com.urremote.bridge.service;

import android.app.Activity;

public interface UpdateListener {
	
//	public Activity getActivity();
	
	public boolean lauchMyTracks();
	public void updateSystemMessages();
	
	public void onSystemStart();
	public void onSystemStop();
	
}
