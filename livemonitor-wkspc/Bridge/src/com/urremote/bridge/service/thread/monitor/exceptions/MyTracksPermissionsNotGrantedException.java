package com.urremote.bridge.service.thread.monitor.exceptions;

import android.content.Context;

public class MyTracksPermissionsNotGrantedException extends MyTracksException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8622106974555319005L;

	public MyTracksPermissionsNotGrantedException(Context context) {
		super(getAppName(context)+" has not been granted permission to access My Tracks. " +
				"This might be because My Tracks was installed AFTER "+getAppName(context)+". " +
						"Please try uninstalling "+getAppName(context)+", and reinstalling it.");
	}

}
