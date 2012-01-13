package com.urremote.bridge;

import com.urremote.bridge.common.Constants;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager manager = this.getPreferenceManager();
		manager.setSharedPreferencesName(Constants.SHARE_PREF);
		manager.setSharedPreferencesMode(MODE_PRIVATE);
		
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
