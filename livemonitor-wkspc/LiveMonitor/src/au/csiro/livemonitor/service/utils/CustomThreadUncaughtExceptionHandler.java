package au.csiro.livemonitor.service.utils;

import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;
import au.csiro.livemonitor.common.Constants;

public class CustomThreadUncaughtExceptionHandler implements
		UncaughtExceptionHandler {
	
	public static final CustomThreadUncaughtExceptionHandler LOG_UNCAUGHT_EXCEPTION_HANDLER = 
			new CustomThreadUncaughtExceptionHandler(null);
	
	public static void setInterceptHandler(Thread thread) {
		CustomThreadUncaughtExceptionHandler handler = 
				new CustomThreadUncaughtExceptionHandler(thread.getUncaughtExceptionHandler());
		thread.setUncaughtExceptionHandler(handler);
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
