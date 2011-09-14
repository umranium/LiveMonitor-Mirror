package au.csiro.antplus.scanner;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Main extends Activity {

//	private static final short WILDCARD = 0;
//	private static final byte DEFAULT_BIN = 7;
//	private static final short DEFAULT_BUFFER_THRESHOLD = 0;
	
	private AntChannelManager antManager;

//	private Button btnStart;

	private int startScan = 12;
	private int stopScan = 12;
	private int simulScans = 2;
	
	private int type = startScan;
	
	private RandomAntChannel.DeviceIdSet deviceIdSet = new RandomAntChannel.DeviceIdSet() {
		@Override
		public void onDeviceIdSet(RandomAntChannel channel) {
			Log.i("Devices Found", "type="+channel.getDeviceType()+", id="+channel.getDeviceId());
			
			channel.close();
			createNextDevice();
		}
		
		public void onScanTimeOut(RandomAntChannel channel) {
			Log.i(Constants.TAG, "type="+channel.getDeviceType()+" timeout");
			createNextDevice();
		};
	};
	
	private void createNextDevice() {
		
		if (type>stopScan || isFinishing()) return;
		
		Log.i(Constants.TAG, "Test Type="+type);
		RandomAntChannel channel =  new RandomAntChannel(antManager);
		channel.setNetworkNumber((byte)0);
		channel.setFrequency((byte)50);
		channel.setDeviceType((byte)type);
		channel.setOnDeviceIdSet(deviceIdSet);
		channel.search((byte)0, (byte)4);
		
		++type;
	}
	
	private Thread scanThread = new Thread() {
		public void run() {
			for (int i=0; i<simulScans; ++i)
			{
				createNextDevice();
				if (Main.this.isFinishing()) break;
			}
		};
	};
	
	private AntManagerEventsCallback antManagerEventsCallback = new AntManagerEventsCallback() {
		
		@Override
		public void onServiceConnected() {
			scanThread.start();
		}

		@Override
		public void onServiceDisconneted() {
		}
		
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		antManager = new AntChannelManager(this);
		antManager.setEventsCallback(antManagerEventsCallback);
		antManager.connect();
		
//		btnStart = (Button)this.findViewById(R.id.btn_start);
//		btnStart.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//			}
//		});
	}
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		antManager.shutdown();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		this.finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	 

}