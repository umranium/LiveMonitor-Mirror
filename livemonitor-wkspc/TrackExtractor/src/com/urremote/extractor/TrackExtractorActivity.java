package com.urremote.extractor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationFactory;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class TrackExtractorActivity extends Activity {

	private Button btnExtract;
	private String primaryAccount;
	private Handler mainLooperHandler;
	private MyTracksProviderUtils utils;
	private File parentFolder;

	private LocationFactory locationFactory = new LocationFactory() {
		@Override
		public Location createLocation() {
			return new MyTracksLocation("gps");
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.primaryAccount = PrimaryAccountUtil.getPrimaryAccount(this);
		this.mainLooperHandler = new Handler(this.getMainLooper());
		this.utils = MyTracksProviderUtils.Factory.get(this);

		this.parentFolder = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + File.separator + "Extractor");
		if (!this.parentFolder.exists()) {
			this.parentFolder.mkdirs();
		}

		btnExtract = (Button) findViewById(R.id.btn_extract);

		btnExtract.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				btnExtract.setEnabled(false);
				Thread th = new Thread() {
					public void run() {
						extract();
						TrackExtractorActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								btnExtract.setEnabled(true);							}
						});
						
					};
				};
				th.start();
			}
		});
	}
	
	
	private void showToastUi(final String text) {
		TrackExtractorActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(TrackExtractorActivity.this, text, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	private void extract() {
    	
    	for (File f:this.parentFolder.listFiles()) {
    		if (!f.isDirectory())
    			f.delete();
    	}
    	
    	List<Track> allTracks = utils.getAllTracks();
    	for (int trackIndex=0; trackIndex<allTracks.size(); ++trackIndex) {
    		Track track = allTracks.get(trackIndex);
    		
    		showToastUi("Preparing " + track.getName() + "("+(trackIndex+1)+"/"+allTracks.size()+")");
			
    		try {
    			writeTrack(track);
    		} catch (Exception e) {
    			Log.e(Constants.TAG, "Error while writing track details to SDCard", e);
    			Toast.makeText(TrackExtractorActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_LONG).show();
    		}
    	}
    	
    	List<File> sendFiles = new ArrayList<File>();
    	for (File f:this.parentFolder.listFiles()) {
    		if (!f.isDirectory())
    			sendFiles.add(f);
    	}
    	
//    	showToastUi("Emailing Data");
//    	email(Constants.EMAIL_TO, "", "MyTracks Data: "+primaryAccount, "", sendFiles);
    	
    	showToastUi("Done");
    }

	private String toFilename(String value) {
		return value.replaceAll("[^A-Za-z0-9_-]+", "_");
	}

	private void writeTrack(Track track) throws FileNotFoundException {

		String fname = toFilename(primaryAccount + "--" + track.getName())
				+ ".txt";

		File file = new File(this.parentFolder.getAbsolutePath()
				+ File.separator + fname);

		PrintStream out = new PrintStream(new BufferedOutputStream(
				new FileOutputStream(file)));

		LocationIterator iterator = utils.getLocationIterator(track.getId(),
				-1, false, locationFactory);
		while (iterator.hasNext()) {
			MyTracksLocation location = (MyTracksLocation) iterator.next();

			int heartRate = -1;
			int power = -1;

			SensorDataSet sensorDataSet = location.getSensorDataSet();
			if (sensorDataSet != null) {
				if (sensorDataSet.getHeartRate().hasValue())
					heartRate = sensorDataSet.getHeartRate().getValue();
				if (sensorDataSet.getPower().hasValue())
					power = sensorDataSet.getPower().getValue();
			}

			out.println(location.getTime() + "\t" + location.getLatitude() + "\t" + location.getLongitude() + "\t" + location.getAltitude() + "\t" + heartRate + "\t" + power);
		}

		out.close();
	}

	public void email(String emailTo, String emailCC, String subject, String emailText, List<File> files) {
		// need to "send multiple" to get more than one attachment
		final Intent emailIntent = new Intent(
				android.content.Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] { emailTo });
		emailIntent.putExtra(android.content.Intent.EXTRA_CC,
				new String[] { emailCC });
		// has to be an ArrayList
		ArrayList<Uri> uris = new ArrayList<Uri>();
		// convert from paths to Android friendly Parcelable Uri's
		for (File fileIn:files) {
			Uri u = Uri.fromFile(fileIn);
			uris.add(u);
		}
		emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}

}