package com.urremote.bridge;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.urremote.bridge.R;

import com.urremote.bridge.common.Constants;
import com.urremote.bridge.common.CustomThreadUncaughtExceptionHandler;
import com.urremote.bridge.scroller.ScrollerMessage;
import com.urremote.bridge.scroller.ScrollerUpdater;
import com.urremote.bridge.service.ILiveMonitorBinder;
import com.urremote.bridge.service.LiveMonitorService;
import com.urremote.bridge.service.UpdateListener;

public class Main extends Activity {

	private static final int MAX_STATUS_LINES = 50;
	
	private static final int SETTINGS_ACTIVITY_REQ_CODE = 1;
	private static final int GPS_OPTIONS_REQ_CODE 		= 2;
	private static final int MYTRACKS_MARKET_REQ_CODE	= 3;
    
	private LocationManager locationManager;
	
	private ILiveMonitorBinder binder;
	
	private Button btnStart;
	private Button btnStop;
	private Button btnEditAccountSettings;
	private Button btnEditActivitySettings;
	private CheckBox chkStatusAutoscroll;
	private TextView lblStatusAutoscroll;
	
	private ListView lstStatus;
	private ArrayAdapter<ScrollerMessage> lstStatusAdapter;
	private ScrollerUpdater systemMessagesUpdater;
	private boolean isUiActive = false;
	private boolean pendingRegisterUpdateListener = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onCreate()");
		
		CustomThreadUncaughtExceptionHandler.setInterceptHandler(Thread.currentThread());
        
        this.locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);
        
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onCreate()");
        setContentView(R.layout.main);
        
        btnStart = (Button)this.findViewById(R.id.btn_start);
        btnStop = (Button)this.findViewById(R.id.btn_stop);
        btnEditAccountSettings = (Button)this.findViewById(R.id.btn_edit_account_settings);
        btnEditActivitySettings = (Button)this.findViewById(R.id.btn_edit_activity_settings);
        chkStatusAutoscroll = (CheckBox)this.findViewById(R.id.chk_status_autoscroll);
        lblStatusAutoscroll = (TextView)this.findViewById(R.id.lbl_status_autoscroll);
        
        lstStatus = (ListView)this.findViewById(R.id.lst_status);
        lstStatusAdapter = new ArrayAdapter<ScrollerMessage>(this, R.layout.status_list_item);
        lstStatus.setAdapter(lstStatusAdapter);
        systemMessagesUpdater = new ScrollerUpdater(lstStatusAdapter);
        
    	LinearLayout layoutDevelWarning = (LinearLayout)this.findViewById(R.id.layout_devel_warning);
        if (!Constants.IS_TESTING) {
        	layoutDevelWarning.setVisibility(View.GONE);
        }
        
        btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(Constants.TAG, "btnStart clicked");
				if (binder!=null) {
					try {
						if (!binder.isRecording()) {
							if (!isGPSEnabled()) {
								createGpsDisabledAlert();
							} else {
								binder.startRecording();
							}
						}
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
						if (binder.isRecording()) {
							int dataCount = binder.getPendingUploadCount(); 
							if (dataCount>0) {
								Log.d(Constants.TAG, "Pending data found");
								AlertDialog.Builder bldr = new AlertDialog.Builder(Main.this);
								bldr.setTitle("Pending upload data");
								bldr.setMessage("Are you sure you wish to stop before data is uploaded?\n" +
										dataCount+" seconds of data will be lost.");
								bldr.setCancelable(true);
								bldr.setPositiveButton("Stop anyway!", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										binder.stopRecording();
										dialog.dismiss();
									}
								});
								bldr.show();
							} else {
								binder.stopRecording();
							}
						}
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
        
        chkStatusAutoscroll.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setAutoscroll(isChecked);
			}
		});
        lblStatusAutoscroll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setAutoscroll(!chkStatusAutoscroll.isChecked());
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
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onStart()");
    	
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
        
    	
    	if (!isMyTracksInstalled()) {
    		createMyTracksMarketAlert();
    	}
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onResume()");
		
    	if (binder==null)
    		pendingRegisterUpdateListener = true;
    	else
    		registerUpdateListener();
    	
    	isUiActive = true;
    	
    	if (binder!=null) {
    		binder.setUiActive(isUiActive);
    		updateListener.updateSystemMessages();
    	} else {
    		this.btnStart.setEnabled(true);
    		this.btnStart.setEnabled(true);
    	}
    	
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onPause()");
    	
    	if (!pendingRegisterUpdateListener) {
    		unregisterUpdateListener();
    	}
    	
    	isUiActive = false;
    	
    	if (binder!=null) {
    		binder.setUiActive(isUiActive);
    	}
    }
    
    private void setAutoscroll(boolean autoscroll)
    {
    	if (chkStatusAutoscroll.isChecked()!=autoscroll)
    		chkStatusAutoscroll.setChecked(autoscroll);
    	lstStatus.setTranscriptMode(
    			autoscroll?ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL:ListView.TRANSCRIPT_MODE_DISABLED);
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
        	systemMessagesUpdater.setMessageList(binder.getSystemMessages());
        	updateListener.updateSystemMessages();
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
	        	btnStart.setEnabled(!binder.isStarted());
	         	btnStop.setEnabled(binder.isStarted());
	         	
				if (binder.isStarted() && !binder.isRecording()) {
					binder.stopService();
				}
			}
		}
		
		@Override
		public void onSystemStart() {
			updateSystemMessages();
		}
		
		@Override
		public void onSystemStop() {
			updateSystemMessages();
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
    	case GPS_OPTIONS_REQ_CODE:
    	{
    		if (isGPSEnabled()) {
				try {
					if (!binder.isRecording()) {
						binder.startRecording();
					}
				} catch (Exception e) {
					Log.e(Constants.TAG, "Error", e);
				}
    		}
    			
    		break;
    	}
    	case MYTRACKS_MARKET_REQ_CODE:
    	{
    		if (!isMyTracksInstalled()) {
    			this.finish();
    		}
    	}
    	}
    }
    
    private boolean isGPSEnabled()
    {
    	return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
	private void createGpsDisabledAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your GPS is disabled!\nWould you like to enable it?")
				.setCancelable(false)
				.setPositiveButton("Enable GPS",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showGpsOptions();
							}
						});
		builder.setNegativeButton("Do nothing",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void showGpsOptions() {
		Intent gpsOptionsIntent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivityForResult(gpsOptionsIntent, GPS_OPTIONS_REQ_CODE);
	}
	
	private void createMyTracksMarketAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("This application requires Google MyTracks to be installed.\n" +
				"Go to the market to install?")
				.setCancelable(false)
				.setPositiveButton("Install",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showMyTracksMarket();
							}
						});
		builder.setNegativeButton("Exit",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Main.this.finish();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void showMyTracksMarket() {
		Intent myTrackMarketIntent = new Intent(Intent.ACTION_VIEW);
		myTrackMarketIntent.setData(Constants.URI_MYTRACKS_MARKET);
		startActivityForResult(myTrackMarketIntent, MYTRACKS_MARKET_REQ_CODE);
	}
	
	private boolean isMyTracksInstalled() {
		List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
		for (PackageInfo pack:packs) {
			if (pack.packageName.equals(Constants.MY_TRACKS_PACKAGE)) {
				return true;
			}
		}
		return false;
	}
	
}