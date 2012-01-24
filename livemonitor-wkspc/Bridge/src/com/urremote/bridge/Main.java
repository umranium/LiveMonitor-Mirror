package com.urremote.bridge;


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
import com.urremote.bridge.common.CustomUncaughtExceptionHandler;
import com.urremote.bridge.mapmymaps.ActivityDetails;
import com.urremote.bridge.scroller.ScrollerMessage;
import com.urremote.bridge.scroller.ScrollerUpdater;
import com.urremote.bridge.service.ILiveMonitorBinder;
import com.urremote.bridge.service.LiveMonitorService;
import com.urremote.bridge.service.UpdateListener;
import com.urremote.bridge.service.thread.monitor.MyTracksConnection;
import com.urremote.bridge.tasker.TaskerIntent;
import com.urremote.bridge.tasker.TaskerUtil;

public class Main extends Activity {

	private static final int MAX_STATUS_LINES = 50;
	
	private static final int SETTINGS_ACTIVITY_REQ_CODE = 1;
	private static final int GPS_OPTIONS_REQ_CODE 		= 2;
	private static final int MYTRACKS_MARKET_REQ_CODE	= 3;
    
	private LocationManager locationManager;
	
	private ILiveMonitorBinder binder;
	
	private Button btnStart;
	private Button btnPause;
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
	
	private MyUpdateListener updateListener = new MyUpdateListener();
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onCreate()");
		
		CustomUncaughtExceptionHandler.setInterceptHandler(this, Thread.currentThread());
        
        this.locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);
        
		Log.d(Constants.TAG, this.getClass().getSimpleName()+":onCreate()");
        setContentView(R.layout.main);
        
        btnStart = (Button)this.findViewById(R.id.btn_start);
        btnPause = (Button)this.findViewById(R.id.btn_pause);
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
						if (!binder.isRecording() || binder.isRecordingPaused()) {
							if (!isGPSEnabled()) {
								createGpsDisabledAlert();
							} else {
								binder.startRecording();
							}
						} else {
							Log.d(Constants.TAG, "Service is already recording");
						}
					} catch (Exception e) {
						Log.e(Constants.TAG, "Error", e);
					}
				}
				else
					Toast.makeText(Main.this, "Not connected to\nMonitoring Service", Toast.LENGTH_SHORT);
			}
		});
        
        btnPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(Constants.TAG, "btnPause clicked");
				if (binder!=null) {
					try {
						if (binder.isRecording()) {
							if (!binder.isRecordingPaused()) {
								binder.pauseRecording(true);
							}
							else
								Log.d(Constants.TAG, "Service is ALREADY paused");
						}
						else
							Log.d(Constants.TAG, "Service is NOT recording");
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
								bldr.setNegativeButton("No! Don't!", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								});
								bldr.show();
							} else {
//								binder.stopRecording();
								AlertDialog.Builder bldr = new AlertDialog.Builder(Main.this);
								bldr.setTitle("Stop recording");
								bldr.setMessage("Are you sure you wish to stop recording?");
								bldr.setCancelable(true);
								bldr.setPositiveButton("Yes please", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										binder.stopRecording();
										dialog.dismiss();
									}
								});
								bldr.setNegativeButton("No! Don't!", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								});
								bldr.show();
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
        
    	
//    	latencyTestThread = new LatencyTestThread();
//    	latencyTestThread.start();
    	
    	if (!MyTracksConnection.isMyTracksInstalled(this)) {
    		createMyTracksMarketAlert();
    	} else
	    	if (!MyTracksConnection.hasMyTracksPermission(this)) {
	    		createMyTracksPermissionAlert();
	    	}
    	
    }
    
//    private LatencyTestThread latencyTestThread = null;
    
    @Override
    protected void onStop() {
    	super.onStop();
    	
//    	if (latencyTestThread!=null) {
//    		latencyTestThread.quit();
//    		latencyTestThread = null;
//    	}
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
    		updateListener.internalUpdateSystemMessages();
    		updateListener.internalUpdateSystemState();
    	} else {
    		this.btnStart.setEnabled(false);
    		this.btnStart.setText("Start");
    		this.btnPause.setEnabled(false);
    		this.btnStop.setEnabled(false);
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
        	updateListener.internalUpdateSystemMessages();
        	updateListener.internalUpdateSystemState();
        }

        public void onServiceDisconnected(ComponentName arg0) {
        	binder = null;
        }

    };
    
    private class MyUpdateListener implements UpdateListener {
		
//		@Override
//		public Activity getActivity() {
//			return Main.this;
//		}
		
		@Override
		public boolean lauchMyTracks() {
			Main.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.d(Constants.TAG, "Launching MyTracks");
			    	Intent intent = new Intent();
					intent.setComponent(new ComponentName(
							Constants.MY_TRACKS_PACKAGE,
							Constants.MY_TRACKS_UI_CLASS));
			    	Main.this.startActivity(intent);
				}
			});
			return true;
		}

		@Override
		synchronized
		public void updateSystemMessages() {
			Main.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					internalUpdateSystemMessages();
				}
			});
		}
		
		@Override
		public void onSystemStart() {
			Log.d(Constants.TAG, "Main: Received: onSystemStart");
			Main.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					internalUpdateSystemState();
				}
			});
		}
		
		@Override
		public void onSystemPaused() {
			Log.d(Constants.TAG, "Main: Received: onSystemPaused");
			Main.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					internalUpdateSystemState();
				}
			});
		}
		
		@Override
		public void onSystemResumed() {
			Log.d(Constants.TAG, "Main: Received: onSystemResumed");
			Main.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					internalUpdateSystemState();
				}
			});
		}
		
		@Override
		public void onSystemStop() {
			Log.d(Constants.TAG, "Main: Received: onSystemStop");
			Main.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					internalUpdateSystemState();
				}
			});
		}
		
		synchronized private void internalUpdateSystemMessages() {
			systemMessagesUpdater.update();
		}
		
		synchronized private void internalUpdateSystemState() {
			if (binder!=null) {
	        	btnStart.setEnabled(!binder.isRecording() || binder.isRecordingPaused());
	        	if (binder.isRecordingPaused())
	        		btnStart.setText("Resume");
	        	else
	        		btnStart.setText("Start");
	        	btnPause.setEnabled(binder.isRecording() && !binder.isRecordingPaused());
	         	btnStop.setEnabled(binder.isRecording());
	         	
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
    		if (!MyTracksConnection.isMyTracksInstalled(this)) {
    			this.finish();
    		} else {
    			
    		}
    		break;
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
		builder.setMessage("This application requires Google My Tracks to be installed.\n" +
				"Go to the market to install?")
				.setCancelable(false)
				.setPositiveButton("Install",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showMyTracksMarket();
							}
						})
				.setNegativeButton("Exit",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Main.this.finish();
						}
					});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void createMyTracksPermissionAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("This application requires Google My Tracks permissions which haven't been granted.\n" +
				"This could be a result of installing MyTracks AFTER "+getString(R.string.app_name)+".\n" +
				"Please reinstall "+getString(R.string.app_name)+" to fix this issue."
				)
				.setCancelable(false)
				.setPositiveButton("Reinstall",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							showBridgeMarket();
						}
					})
				.setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Main.this.finish();
						}
					});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void showMyTracksMarket() {
		Intent marketIntent = new Intent(Intent.ACTION_VIEW);
		marketIntent.setData(Constants.URI_MYTRACKS_MARKET);
		startActivityForResult(marketIntent, MYTRACKS_MARKET_REQ_CODE);
	}
	
	private void showBridgeMarket() {
		Intent marketIntent = new Intent(Intent.ACTION_VIEW);
		marketIntent.setData(Constants.URI_BRIDGE_MARKET);
		this.finish();
		startActivity(marketIntent);
	}
	
	
}