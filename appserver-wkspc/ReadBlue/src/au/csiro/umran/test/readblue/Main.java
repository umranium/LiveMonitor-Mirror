package au.csiro.umran.test.readblue;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class Main extends Activity {
	
	private static final int REQUEST_ENABLE_BT = 1;
	
	private Button btnStartScan;
	private Button btnStopScan;
	private Button btnStartRecording;
	private Button btnStopRecording;
	private ListView lstMessages;
	private ArrayAdapter<SystemMessage> lstMessagesAdapter;
	private ListView lstConnectableDevices;
	private ArrayAdapter<ConnectableDevice> lstConnectableDevicesAdapter;
	
	private Looper looper;
	private Handler handler;
	private ReadBlueServiceBinder serviceBinder;
	
	private UpdateMessagesRunnable updateMessagesRunnable;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.looper = this.getMainLooper();
		this.handler = new Handler(looper);
		
		btnStartScan = (Button) findViewById(R.id.btn_start_scan);
		btnStartScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serviceBinder!=null)
					serviceBinder.startScanning();
			}
		});

		btnStopScan = (Button) findViewById(R.id.btn_stop_scan);
		btnStopScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serviceBinder!=null)
					serviceBinder.stopScanning();
			}
		});

		btnStartRecording = (Button) findViewById(R.id.btn_start_record);
		btnStartRecording.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serviceBinder!=null)
					serviceBinder.startRecording();
			}
		});
		
		btnStopRecording = (Button) findViewById(R.id.btn_stop_record);
		btnStopRecording.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serviceBinder!=null)
					serviceBinder.stopRecording();
			}
		});
		
		lstMessages = (ListView) findViewById(R.id.lst_messages);
		lstMessagesAdapter = new ArrayAdapter<SystemMessage>(this, R.layout.msg_item);
		lstMessages.setAdapter(lstMessagesAdapter);
		lstMessages.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View vew, int pos,
					long id) {
				Log.d(Constants.TAG, "Messages List Clicked");
			}
		});

		lstConnectableDevices = (ListView) findViewById(R.id.lst_connectable_devices);
		lstConnectableDevicesAdapter = new ArrayAdapter<ConnectableDevice>(
				this, R.layout.device_item);
		lstConnectableDevices.setAdapter(lstConnectableDevicesAdapter);
		lstConnectableDevices.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View vew, int pos,
					long id) {
				Log.d(Constants.TAG, "Connectable Device Item Clicked");
				ConnectableDevice connectableDevice = lstConnectableDevicesAdapter
						.getItem(pos);
				connectTo(connectableDevice);
			}
		});
		
		btnStartScan.setEnabled(false);
		btnStopScan.setEnabled(false);
		
		Log.d(Constants.TAG, "About to bind to service.");
		
		Intent serviceIntent = new Intent(this, ReadBlueService.class);
		this.bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		this.unbindService(serviceConnection);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (serviceBinder!=null) {
			serviceBinder.registerEventHandler(serviceEventHandler);
		}
		
		updateMessagesRunnable = new UpdateMessagesRunnable();
		updateMessagesRunnable.run();
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		if (serviceBinder!=null) {
			serviceBinder.registerEventHandler(null);
		}
		
		if (updateMessagesRunnable!=null) {
			updateMessagesRunnable.disable();
			updateMessagesRunnable = null;
		}
	}
	
	private void connectTo(ConnectableDevice connectableDevice) {
		if (serviceBinder!=null) {
			serviceBinder.toggleDeviceConnection(connectableDevice);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ENABLE_BT: {
			switch (resultCode) {
			case RESULT_OK: {
				if (serviceBinder!=null)
					serviceBinder.startScanning();
				break;
			}
			case RESULT_CANCELED: {
				break;
			}
			}
			break;
		}
		}
	}
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(Constants.TAG, "Connected to service");
			serviceBinder = (ReadBlueServiceBinder)service;
			if (serviceBinder!=null) {
				serviceBinder.registerEventHandler(serviceEventHandler);
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
		
	};
	
	private class UpdateMessagesRunnable implements Runnable {
		
		boolean enabled = true;
		
		public void disable() {
			enabled = false;
		}
		
		@Override
		public void run() {
			if (enabled) {
				serviceEventHandler.onMessagesUpdated();
				lstConnectableDevicesAdapter.notifyDataSetChanged();
				handler.postDelayed(this, 500L);
			}
		}
		
		
	}

	private ServiceEventHandler serviceEventHandler = new ServiceEventHandler() {
		
		@Override
		public void onScanningStateChanged() {
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (serviceBinder!=null) {
						boolean scanningEnabled = serviceBinder.isScanningEnabled();
						boolean isScanning = serviceBinder.isScanning();
						
						btnStartScan.setEnabled(scanningEnabled && !isScanning);
						btnStopScan.setEnabled(scanningEnabled && isScanning);
					}
				}
			});
		}
		
		@Override
		public void onMessagesUpdated() {
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (serviceBinder!=null) {
						List<SystemMessage> messages = serviceBinder.getMessages();
						
						//	remove from the top
						
						long earliestMsgInService = 0;
						
						if (!messages.isEmpty()) {
							earliestMsgInService = messages.get(0).timeStamp;
						}
						
						List<SystemMessage> filterOff = new ArrayList<SystemMessage>();
						for (int i=0; i<lstMessagesAdapter.getCount(); ++i) {
							SystemMessage msg = lstMessagesAdapter.getItem(i); 
							if (msg.timeStamp<earliestMsgInService) {
								filterOff.add(msg);
							} else {
								break;
							}
						}
						
						for (SystemMessage msg:filterOff)
							lstMessagesAdapter.remove(msg);
						
						// add to the bottom
						
						long latestMsgInUi = 0;
						
						if (!lstMessagesAdapter.isEmpty()) {
							latestMsgInUi = lstMessagesAdapter.getItem(lstMessagesAdapter.getCount()-1).timeStamp;
						}
						
						for (int l=messages.size(), i=0; i<l; ++i) {
							SystemMessage msg = messages.get(i);
							if (msg!=null) {
								if (msg.timeStamp>latestMsgInUi) {
									lstMessagesAdapter.add(msg);
								}
							}
						}
					}
				}
			});
		}
		
		@Override
		public void onConnectableDevicesUpdated() {
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (serviceBinder!=null) {
						List<ConnectableDevice> devices = serviceBinder.getConnectableDevices();
						while (lstConnectableDevicesAdapter.getCount()<devices.size()) {
							lstConnectableDevicesAdapter.add(devices.get(lstConnectableDevicesAdapter.getCount()));
						}
						while (lstConnectableDevicesAdapter.getCount()>devices.size()) {
							lstConnectableDevicesAdapter.remove(
									(ConnectableDevice)lstConnectableDevicesAdapter.getItem(
											lstConnectableDevices.getCount()-1));
						}
					}
					
					lstConnectableDevicesAdapter.notifyDataSetChanged();
					
					boolean hasItems = lstConnectableDevicesAdapter.getCount()>0;
					btnStartRecording.setEnabled(hasItems);
					btnStopRecording.setEnabled(hasItems);
				}
			});
			
		}

		@Override
		public void onAttemptEnableBluetooth() {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
	};
	

}