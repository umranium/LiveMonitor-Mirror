package com.urremote.bridge.service.thread.monitor.exceptions;

import android.content.Context;

public class MyTracksNotInstalledException extends MyTracksException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4137423643740938810L;

	public MyTracksNotInstalledException(Context context) {
		super(getAppName(context)+" has not been granted permission to access My Tracks. " +
				"This might be because My Tracks was installed AFTER "+getAppName(context)+". " +
						"Please try uninstalling "+getAppName(context)+", and reinstalling it.");
	}
	
	
}
