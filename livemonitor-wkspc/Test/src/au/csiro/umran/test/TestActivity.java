package au.csiro.umran.test;

import com.google.android.apps.mytracks.services.ITrackRecordingService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class TestActivity extends Activity {
	
	private ITrackRecordingService mytracks;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	try {
			connectToMyTracks();
		} catch (Exception e) {
			Log.e(Constants.TAG, "Error connecting to mytracks", e);
		}
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	try {
			disconnectFromMyTracks();
		} catch (Exception e) {
			Log.e(Constants.TAG, "Error disconnecting to mytracks", e);
		}
    }

	private void connectToMyTracks() throws Exception {
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(
				Constants.MY_TRACKS_PACKAGE,
				Constants.MY_TRACKS_SERVICE_CLASS));
		if (!this.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
			throw new Exception("Unable to find MyTracks service. MyTracks may not be installed.");
		} else {
			Log.i(Constants.TAG, "Connecting to MyTracks service");
		}
	}
	
	private void disconnectFromMyTracks() throws Exception {
		if (mytracks!=null)
			mytracks.endCurrentTrack();
		this.unbindService(connection);
	}

	
	private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        	try {
            	mytracks = ITrackRecordingService.Stub.asInterface(arg1);
				mytracks.startNewTrack();
			} catch (Exception e) {
				Log.e(Constants.TAG, "Error", e);
			}
        }

        public void onServiceDisconnected(ComponentName arg0) {
        	mytracks = null;
        }

    };
	
}