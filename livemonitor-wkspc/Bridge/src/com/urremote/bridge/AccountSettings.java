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
import com.urremote.bridge.common.CustomUncaughtExceptionHandler;
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
	
	private ProgressDialog verificationProgressDlg;
	private VerificationThread verificationThread;
	
	private class VerificationThread extends Thread {
		
		CharSequence username;
		CharSequence password;
		MapMyTracksInterfaceApi mapMyTracksInterfaceApi;
		boolean quit = false;
		
		public VerificationThread(CharSequence username, CharSequence password) {
			this.username = username;
			this.password = password;
	    	this.mapMyTracksInterfaceApi = new MapMyTracksInterfaceApi(
	    			-1,
	    			new String(new StringBuilder(username)), 
	    			new String(new StringBuilder(password)));
		}
		
		public void quit() {
			quit = true;
			mapMyTracksInterfaceApi.shutdown();
			this.interrupt();
		}
		
		@Override
		public void run() {
			CustomUncaughtExceptionHandler.setInterceptHandler(AccountSettings.this, Thread.currentThread());
			
	    	try {
				Long serverTime = mapMyTracksInterfaceApi.getServerTime();
				if (!quit) {
					if (serverTime!=null) {
						Log.d(Constants.TAG, "Success. Server time="+serverTime);
						
	//					finishActivity();
						SharedPreferences preferences = AccountSettings.this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
						
						if (DefSettings.getUsername(preferences).length()==0) {
							saveState(preferences);
							displayToastFromAnotherThread("Login details verified and saved.\nPress the back button to continue.");
						} else {
							saveState(preferences);
							displayToastFromAnotherThread("Login details verified and saved.\nPress the back button to return to the application.");
						}
					} else {
						displayToastFromAnotherThread("Invalid login details or unknown verification failure.");
					}
				}
			} catch (IOException e) {
				if (!quit) {
					Log.d(Constants.TAG, "Error while attempting to verify login details", e);
					displayToastFromAnotherThread("Error: "+e.getMessage());
				}
			} finally {
				mapMyTracksInterfaceApi.shutdown();
				if (verificationThread!=null) {
					verificationThread = null;
				}
				if (verificationProgressDlg!=null) {
					verificationProgressDlg.dismiss();
					verificationProgressDlg = null;
				}
				Log.d(Constants.TAG, "Verification Thread Finished");
			}
		}
	}

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
				setResult(RESULT_OK);
				finish();
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
    	createVerificationProgressDlg();
    	verificationThread = new VerificationThread(username, password);
    	verificationThread.start();
    }
    
    private void createVerificationProgressDlg() {
    	Log.i(Constants.TAG, "Creating Verification Dialog");
    	verificationProgressDlg = ProgressDialog.show(this, 
				"Verifying details",
				"Verifying login details with the server.\nPlease wait...");
    	verificationProgressDlg.setCancelable(true);
    	verificationProgressDlg.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.d(Constants.TAG, "Verification ProgressDialog Cancelled");
				if (verificationThread!=null) {
					Log.d(Constants.TAG, "Requesting to cancel Verification Thread");
					verificationThread.quit();
				}
			}
		});
    	verificationProgressDlg.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				Log.d(Constants.TAG, "Verification ProgressDialog Dismissed");
//				if (verificationThread!=null) {
//					verificationThread.quit();
//				}
			}
		});
    }
    
//    private void finishActivity()
//    {
//        SharedPreferences preferences = AccountSettings.this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
//    	saveState(preferences);
//    	
//		if (showActivitySettingsToo) {
//			showActivitySettings();
//		} else {
//			AccountSettings.this.setResult(RESULT_OK);
//			AccountSettings.this.finish();
//		}
//    }
    
    @Override
    public void onBackPressed() {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
        boolean hasUsername = DefSettings.getUsername(preferences).length()>0; 
        
		if (hasUsername) {
//			if (showActivitySettingsToo) {
//				showActivitySettings();
//			} else {
				setResult(RESULT_OK);
				finish();
//			}
		} else {
			Toast.makeText(AccountSettings.this, "No username/password saved, exiting.", Toast.LENGTH_LONG).show();
			super.onBackPressed();
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
