package au.urremote.bridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import au.urremote.bridge.R;
import au.urremote.bridge.common.Constants;
import au.urremote.bridge.common.DefSettings;
import au.urremote.bridge.mapmymaps.ActivityType;

public class AccountSettings extends Activity {

	private static final int SETTINGS_ACTIVITY_REQ_CODE = 1;
	
	private TextView txtUsername;
	private TextView txtPassword;

	private Button btnDone;
	
	private boolean showActivitySettingsToo = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_settings);
        
        showActivitySettingsToo = this.getIntent().getBooleanExtra(Constants.SHOW_ACTIVITY_SETTINGS_TOO, false);
        
        txtUsername = (TextView)this.findViewById(R.id.txt_username);
        txtPassword = (TextView)this.findViewById(R.id.txt_password);
        btnDone = (Button)this.findViewById(R.id.btn_settings_done);
        
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
				
		        SharedPreferences preferences = AccountSettings.this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
		    	saveState(preferences);
		    	
				if (showActivitySettingsToo) {
					showActivitySettings();
				} else {
					AccountSettings.this.setResult(RESULT_OK);
					AccountSettings.this.finish();
				}
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
    
    
}
