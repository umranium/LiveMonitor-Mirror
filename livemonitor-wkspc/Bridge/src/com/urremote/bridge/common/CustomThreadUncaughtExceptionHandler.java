package com.urremote.bridge.common;

import java.lang.Thread.UncaughtExceptionHandler;


import android.util.Log;

public class CustomThreadUncaughtExceptionHandler implements
		UncaughtExceptionHandler {
	
	public static final CustomThreadUncaughtExceptionHandler LOG_UNCAUGHT_EXCEPTION_HANDLER = 
			new CustomThreadUncaughtExceptionHandler(null);
	
	public static void setInterceptHandler(Thread thread) {
		UncaughtExceptionHandler prevHandler = thread.getUncaughtExceptionHandler();
		
		if (!(prevHandler instanceof CustomThreadUncaughtExceptionHandler)) {
			CustomThreadUncaughtExceptionHandler handler = new CustomThreadUncaughtExceptionHandler(prevHandler);
			thread.setUncaughtExceptionHandler(handler);
		}
	}

	
	private UncaughtExceptionHandler prev;
	
	public CustomThreadUncaughtExceptionHandler(UncaughtExceptionHandler prev) {
		this.prev = prev;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e(Constants.TAG, "Uncaught exception in thread: "+thread.getName(), ex);
		if (this.prev!=null)
			this.prev.uncaughtException(thread, ex);
	}
	
}
