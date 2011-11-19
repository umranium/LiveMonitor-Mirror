package com.urremote.bridge;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.urremote.bridge.R;

import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.DefSettings;
import com.urremote.bridge.mapmymaps.MapMyTracksInterfaceApi;

public class AccountSettings extends Activity {

	private static final int SETTINGS_ACTIVITY_REQ_CODE = 1;
	private static final int REGISTER_MAPMYTRACKS_REQ_CODE = 2;
	
	private TextView txtUsername;
	private TextView txtPassword;

	private Button btnRegister;
	private Button btnDone;
	
	private boolean showActivitySettingsToo = false;
	
	private Handler mainLooperHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_settings);
        
        mainLooperHandler = new Handler(this.getMainLooper());
        
        showActivitySettingsToo = this.getIntent().getBooleanExtra(Constants.SHOW_ACTIVITY_SETTINGS_TOO, false);
        
        txtUsername = (TextView)this.findViewById(R.id.txt_username);
        txtPassword = (TextView)this.findViewById(R.id.txt_password);
        btnRegister = (Button)this.findViewById(R.id.btn_settings_register);
        btnDone = (Button)this.findViewById(R.id.btn_settings_done);
        
        btnRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Constants.URI_MAPMYTRACKS_REGISTER);
				startActivityForResult(browserIntent, REGISTER_MAPMYTRACKS_REQ_CODE);
			}
		});
        
        btnDone.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (txtUsername.getText().length()==0) {
					Toast.makeText(AccountSettings.this, "Please provide a MapMyTracks username", Toast.LENGTH_LONG).show();
					return;
				}
				if (txtPassword.getText().length()==0) {
					Toast.makeText(AccountSettings.this, "Please provide a MapMyTracks password", Toast.LENGTH_LONG).show();
					return;
				}
				
				verifyAccount(txtUsername.getText(), txtPassword.getText());
			}
		});
        
        SharedPreferences preferences = this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
    	loadState(preferences);
    	
    	setResult(RESULT_CANCELED);
    }
    
    private void loadState(SharedPreferences state)
    {
		txtUsername.setText(DefSettings.getUsername(state));
		txtPassword.setText(DefSettings.getPassword(state));
    }
    
    private void saveState(SharedPreferences state)
    {
    	Editor editor = state.edit();
    	editor.putBoolean(Constants.KEY_ACCOUNT_SETTINGS_AVAILABLE, true);
    	editor.putString(Constants.KEY_USERNAME, new String(new StringBuilder(txtUsername.getText())));
    	editor.putString(Constants.KEY_PASSWORD, new String(new StringBuilder(txtPassword.getText())));
    	
    	editor.commit();
    }

    private void showActivitySettings() {
    	Intent intent = new Intent();
    	intent.setClass(this, ActivitySettings.class);
    	this.startActivityForResult(intent, SETTINGS_ACTIVITY_REQ_CODE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case SETTINGS_ACTIVITY_REQ_CODE:
    	{
    		switch (resultCode) {
    		case RESULT_OK:
    		{
				AccountSettings.this.setResult(RESULT_OK);
				AccountSettings.this.finish();
    			break;
    		}
    		case RESULT_CANCELED:
    		{
//    	        SharedPreferences preferences = this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
//    	    	boolean activitySettingsAvailable = preferences.getBoolean(Constants.KEY_ACTIVITY_SETTINGS_AVAILABLE, false);
//    	    	if (!activitySettingsAvailable) {
//    	    		this.finish();
//    	    	}
    			break;
    		}
    		}
    		break;
    	}
    	}
    }
    
    
    private void verifyAccount(CharSequence username, CharSequence password)
    {
    	final MapMyTracksInterfaceApi mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
    			-1,
    			new String(new StringBuilder(username)), 
    			new String(new StringBuilder(password)));
    	
		final ProgressDialog dialog = ProgressDialog.show(this, 
				"Verifying details",
				"Verifying login details with the server.\nPlease wait...");
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.d(Constants.TAG, "Verification ProgressDialog Cancelled");
				mapMyTracksInterfaceApi.shutdown();
			}
		});
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				Log.d(Constants.TAG, "Verification ProgressDialog Dismissed");
				mapMyTracksInterfaceApi.shutdown();
			}
		});
		
		Thread th = new Thread() {
			@Override
			public void run() {
		    	try {
					Long serverTime = mapMyTracksInterfaceApi.getServerTime();
					if (serverTime!=null) {
						Log.d(Constants.TAG, "Success. Server time="+serverTime);
						displayToastFromAnotherThread("Login details verified");
						finishActivity();
					} else {
						displayToastFromAnotherThread("Invalid login details or unknown verification failure.");
					}
				} catch (IOException e) {
					Log.d(Constants.TAG, "Error while attempting to verify login details", e);
					displayToastFromAnotherThread("Error: "+e.getMessage());
				} finally {
					Log.d(Constants.TAG, "Dismissing Verification ProgressDialog");
					dialog.dismiss();
				}
			}
		};
		
		th.start();
    }
    
    private void finishActivity()
    {
        SharedPreferences preferences = AccountSettings.this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
    	saveState(preferences);
    	
		if (showActivitySettingsToo) {
			showActivitySettings();
		} else {
			AccountSettings.this.setResult(RESULT_OK);
			AccountSettings.this.finish();
		}
    }
    
    private void displayToastFromAnotherThread(final String msg)
    {
		mainLooperHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AccountSettings.this, msg, Toast.LENGTH_LONG).show();
			}
		});
    }
    
}
