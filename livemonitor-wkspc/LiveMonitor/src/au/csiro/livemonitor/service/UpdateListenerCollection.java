package au.csiro.livemonitor.service;

import java.util.ArrayList;

import android.util.Log;
import au.csiro.livemonitor.common.Constants;

public class UpdateListenerCollection extends ArrayList<UpdateListener> {

	
	public void lauchMyTracks() {
		for (UpdateListener upd:this) {
			upd.getActivity().runOnUiThread(new LauchMyTracks(upd));
		}
	}
	
	public void updateSystemMessages() {
		for (UpdateListener upd:this) {
			upd.getActivity().runOnUiThread(new UpdateSystemMessages(upd));
		}
	}
	
	private class LauchMyTracks implements Runnable {
		
		private UpdateListener listener;
		
		public LauchMyTracks(UpdateListener listener) {
			this.listener = listener;
		}

		@Override
		public void run() {
			this.listener.lauchMyTracks();
		}
		
	}
	
	private class UpdateSystemMessages implements Runnable {
		
		private UpdateListener listener;
		
		public UpdateSystemMessages(UpdateListener listener) {
			this.listener = listener;
		}

		@Override
		public void run() {
			this.listener.updateSystemMessages();
		}
		
	}
	
}
