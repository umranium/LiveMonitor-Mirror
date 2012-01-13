package com.urremote.bridge.service.thread.monitor.exceptions;

import com.urremote.bridge.R;

import android.content.Context;

public class MyTracksException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6706888106322797112L;

	public MyTracksException() {
	}

	public MyTracksException(String detailMessage) {
		super(detailMessage);
	}

	public MyTracksException(Throwable throwable) {
		super(throwable);
	}

	public MyTracksException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	
	protected static String getAppName(Context context) {
		return context.getString(R.string.app_name);
	}

}
