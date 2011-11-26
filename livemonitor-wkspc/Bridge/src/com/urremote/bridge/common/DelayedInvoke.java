package com.urremote.bridge.common;

import java.lang.reflect.Method;

import android.content.Context;
import android.os.Handler;
import android.util.Log;


class DelayedInvoke implements Runnable {
	
	private Object _this;
	private Method method;
	private Object[] args;

	public DelayedInvoke(Context context, long delay, Class<?> _class, Object _this, String methodName, Class<?>[] paramTypes, Object ... args) throws SecurityException, NoSuchMethodException {
		this._this = _this;
		this.method = _class.getMethod(methodName, paramTypes);
		this.args = args;
		
		Context appContext = context.getApplicationContext();
		Handler mainLooperHandler = new Handler(appContext.getMainLooper());
		mainLooperHandler.postDelayed(this, delay);
	}
	
	@Override
	public void run() {
		try {
			
			method.invoke(_this, args);
			
		} catch (Exception e) {
			Log.e(Constants.TAG, "Exception while trying to run method "+method, e);
		}
	}
	
}