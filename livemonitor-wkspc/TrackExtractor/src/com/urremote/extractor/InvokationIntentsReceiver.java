package com.urremote.extractor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class InvokationIntentsReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Constants.TAG, "Received intent: "+intent.getAction()+":"+(intent.getExtras()==null?"[]":intent.getExtras().keySet()));
		
		final String intentExtract = context.getString(R.string.INTENT_EXTRACT);
		
		if (intentExtract.equals(intent.getAction())) {
			extract(context.getApplicationContext());
		}
	}
	
	private void extract(Context context) {
		Extractor extractor = new Extractor(context);
		extractor.extract();
	}
	
	
}
