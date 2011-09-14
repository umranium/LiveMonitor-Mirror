package au.csiro.bikometer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import au.csiro.bikometer.service.BikometerService;
import au.csiro.bikometer.service.IBikometerBinder;
import au.csiro.bikometer.service.UpdateListener;

public class Main extends Activity {
    
	private IBikometerBinder binder;
	
    private UpdateListener updateListener = new UpdateListener() {
		
    	@Override
    	public Activity getActivity() {
    		return Main.this;
    	}
    	
	}; 
	
    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        	binder = (IBikometerBinder)arg1;
        	binder.registerUpdateListener(updateListener);
        }

        public void onServiceDisconnected(ComponentName arg0) {
        	binder = null;
        }

    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	Intent intent = new Intent(this, BikometerService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    
        Button btnStart = (Button)this.findViewById(R.id.btn_start);
        Button btnStop = (Button)this.findViewById(R.id.btn_stop);
        
        btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
        btnStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
    }
}