package au.csiro.livemonitor;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import au.csiro.livemonitor.common.Constants;
import au.csiro.livemonitor.mapmymaps.ActivityType;
import au.csiro.livemonitor.scroller.ScrollerMessage;
import au.csiro.livemonitor.scroller.ScrollerUpdater;
import au.csiro.livemonitor.service.ILiveMonitorBinder;
import au.csiro.livemonitor.service.LiveMonitorService;
import au.csiro.livemonitor.service.SystemMessage;
import au.csiro.livemonitor.service.UpdateListener;

public class Main extends Activity {

	private static final int MAX_STATUS_LINES = 50;
	
	private static final int SETTINGS_ACTIVITY_REQ_CODE = 1;
    
	private ILiveMonitorBinder binder;
	
	private Button btnStart;
	private Button btnStop;
	private Button btnEditAccountSettings;
	private Button btnEditActivitySettings;
	
	private ListView lstStatus;
	private ArrayAdapter<ScrollerMessage> lstStatusAdapter;
	private ScrollerUpdater systemMessagesUpdater;
	private boolean isUiActive = false;
	private boolean pendingRegisterUpdateListener = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onCreate()");
        setContentView(R.layout.main);
        
        btnStart = (Button)this.findViewById(R.id.btn_start);
        btnStop = (Button)this.findViewById(R.id.btn_stop);
        btnEditAccountSettings = (Button)this.findViewById(R.id.btn_edit_account_settings);
        btnEditActivitySettings = (Button)this.findViewById(R.id.btn_edit_activity_settings);
        
        lstStatus = (ListView)this.findViewById(R.id.lst_status);
        lstStatusAdapter = new ArrayAdapter<ScrollerMessage>(this, R.layout.status_list_item);
        lstStatus.setAdapter(lstStatusAdapter);
        systemMessagesUpdater = new ScrollerUpdater(lstStatusAdapter);
        
        btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(Constants.TAG, "btnStart clicked");
				if (binder!=null) {
					try {
						if (!binder.isRecording())
							binder.startRecording();
						else
							Log.d(Constants.TAG, "Service is already recording");
					} catch (Exception e) {
						Log.e(Constants.TAG, "Error", e);
					}
				}
				else
					Toast.makeText(Main.this, "Not connected to\nMonitoring Service", Toast.LENGTH_SHORT);
			}
		});
        
        btnStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(Constants.TAG, "btnStop clicked");
				if (binder!=null) {
					try {
						if (binder.isRecording())
							binder.stopRecording();
						else
							Log.d(Constants.TAG, "Service is not recording");
					} catch (Exception e) {
						Log.e(Constants.TAG, "Error", e);
					}
				}
				else
					Toast.makeText(Main.this, "Not connected to\nMonitoring Service", Toast.LENGTH_SHORT);
			}
		});
        
        btnEditAccountSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showAccountSettings(false);
			}
		});
        
        btnEditActivitySettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showActivitySettings();
			}
		});
        
    	Intent intent = new Intent(this, LiveMonitorService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onDestroy()");
    	if (binder!=null) {
    		if (binder.isStarted() && !binder.isRecording())
    			binder.stopService();
    		binder.unregisterUpdateListener(updateListener);
    	}
    	unbindService(connection);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
        SharedPreferences preferences = this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
    	boolean accountSettingsAvailable = preferences.getBoolean(Constants.KEY_ACCOUNT_SETTINGS_AVAILABLE, false);
    	boolean activitySettingsAvailable = preferences.getBoolean(Constants.KEY_ACTIVITY_SETTINGS_AVAILABLE, false);
    	if (!accountSettingsAvailable && !accountSettingsAvailable) {
    		showAccountSettings(true);
    	} else
	    	if (!accountSettingsAvailable) {
	    		showAccountSettings(false);
	    	} else
		    	if (!activitySettingsAvailable) {
		    		showActivitySettings();
		    	}
        
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (binder==null)
    		pendingRegisterUpdateListener = true;
    	else
    		registerUpdateListener();
    	
    	isUiActive = true;
    	
    	if (binder!=null) {
    		binder.setUiActive(isUiActive);
    		updateListener.updateSystemMessages();
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	if (!pendingRegisterUpdateListener) {
    		unregisterUpdateListener();
    	}
    	
    	isUiActive = false;
    	
    	if (binder!=null) {
    		binder.setUiActive(isUiActive);
    	}
    }
    
    private void registerUpdateListener()
    {
    	if (binder!=null)
    		binder.registerUpdateListener(updateListener);
    }
    
    private void unregisterUpdateListener()
    {
    	if (binder!=null)
    		binder.unregisterUpdateListener(updateListener);
    }
    
    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        	binder = (ILiveMonitorBinder)arg1;
        	if (pendingRegisterUpdateListener) {
        		registerUpdateListener();
        		pendingRegisterUpdateListener = false;
        	}
        	binder.setUiActive(isUiActive);
        	updateListener.updateSystemMessages();
        	
        	systemMessagesUpdater.setMessageList(binder.getSystemMessages());
        }

        public void onServiceDisconnected(ComponentName arg0) {
        	binder = null;
        }

    };
    
    private UpdateListener updateListener = new UpdateListener() {
		
		@Override
		public Activity getActivity() {
			return Main.this;
		}
		
		@Override
		public void lauchMyTracks() {
			Log.d(Constants.TAG, "Launching MyTracks");
	    	Intent intent = new Intent();
			intent.setComponent(new ComponentName(
					Constants.MY_TRACKS_PACKAGE,
					Constants.MY_TRACKS_UI_CLASS));
	    	Main.this.startActivity(intent);
		}

		@Override
		synchronized
		public void updateSystemMessages() {
			systemMessagesUpdater.update();
			if (binder!=null) {
				if (binder.isStarted() && !binder.isRecording()) {
					binder.stopService();
				}
			}
		}
	}; 
    
    
    private void showAccountSettings(boolean showActivityToo) {
    	Intent intent = new Intent();
    	intent.setClass(this, AccountSettings.class);
    	intent.putExtra(Constants.SHOW_ACTIVITY_SETTINGS_TOO, showActivityToo);
    	this.startActivityForResult(intent, SETTINGS_ACTIVITY_REQ_CODE);
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
    			break;
    		}
    		case RESULT_CANCELED:
    		{
    	        SharedPreferences preferences = this.getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
    	    	boolean accountSettingsAvailable = preferences.getBoolean(Constants.KEY_ACCOUNT_SETTINGS_AVAILABLE, false);
    	    	boolean activitySettingsAvailable = preferences.getBoolean(Constants.KEY_ACTIVITY_SETTINGS_AVAILABLE, false);
    	    	if (!accountSettingsAvailable || !activitySettingsAvailable) {
    	    		this.finish();
    	    	}
    			break;
    		}
    		}
    		break;
    	}
    	}
    }
    
}