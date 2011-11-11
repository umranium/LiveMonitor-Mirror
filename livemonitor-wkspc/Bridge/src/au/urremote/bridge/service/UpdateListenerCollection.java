package au.urremote.bridge.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.util.Log;
import au.urremote.bridge.common.Constants;

public class UpdateListenerCollection extends ArrayList<UpdateListener> {
	
	private static final long serialVersionUID = 8438516574896193680L;
	
	private static final Method LAUNCH_MYTRACKS;
	private static final Method UPDATE_SYSTEM_MESSAGES;
	private static final Method ON_SYSTEM_START;
	private static final Method ON_SYSTEM_STOP;
	
	static {
		Method launchMyTracks = null;
		Method updateSystemMessages = null;
		Method onSystemStart = null;
		Method onSystemStop = null;
		Method onUpdateRecordsBuffer = null;
		
		try {
			launchMyTracks = UpdateListener.class.getDeclaredMethod("lauchMyTracks", null);
			updateSystemMessages = UpdateListener.class.getDeclaredMethod("updateSystemMessages", null);
			onSystemStart = UpdateListener.class.getDeclaredMethod("onSystemStart", null);
			onSystemStop = UpdateListener.class.getDeclaredMethod("onSystemStop", null);
		} catch (SecurityException e) {
			Log.e(Constants.TAG, "Error Initializing UpdateListenerCollection Class", e);
		} catch (NoSuchMethodException e) {
			Log.e(Constants.TAG, "Error Initializing UpdateListenerCollection Class", e);
		}
		
		LAUNCH_MYTRACKS = launchMyTracks;
		UPDATE_SYSTEM_MESSAGES = updateSystemMessages;
		ON_SYSTEM_START = onSystemStart;
		ON_SYSTEM_STOP = onSystemStop;
	}
	
	public void lauchMyTracks() {
		for (UpdateListener upd:this) {
			upd.getActivity().runOnUiThread(new UpdateInvoker(upd, LAUNCH_MYTRACKS, null));
		}
	}
	
	public void updateSystemMessages() {
		for (UpdateListener upd:this) {
			upd.getActivity().runOnUiThread(new UpdateInvoker(upd, UPDATE_SYSTEM_MESSAGES, null));
		}
	}
	
	public void onSystemStart() {
		for (UpdateListener upd:this) {
			upd.getActivity().runOnUiThread(new UpdateInvoker(upd, ON_SYSTEM_START, null));
		}
	}
	
	public void onSystemStop() {
		for (UpdateListener upd:this) {
			upd.getActivity().runOnUiThread(new UpdateInvoker(upd, ON_SYSTEM_STOP, null));
		}
	}
	
	private class UpdateInvoker implements Runnable {
		
		private UpdateListener listener;
		private Method method;
		private Object[] args;
		
		public UpdateInvoker(UpdateListener listener, Method method, Object[] args) {
			this.listener = listener;
			this.method = method;
			this.args = args;
		}
		
		@Override
		public void run() {
			try {
				method.invoke(listener, args);
			} catch (IllegalArgumentException e) {
				Log.e(Constants.TAG, "Error while invoking method: "+method, e);
			} catch (IllegalAccessException e) {
				Log.e(Constants.TAG, "Error while invoking method: "+method, e);
			} catch (InvocationTargetException e) {
				Log.e(Constants.TAG, "Error while invoking method: "+method, e);
			}
		}
	}
}
