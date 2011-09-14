package au.csiro.livemonitor.service;

import android.app.Activity;

public interface UpdateListener {
	
	public Activity getActivity();
	public void lauchMyTracks();
	public void updateSystemMessages();

}
