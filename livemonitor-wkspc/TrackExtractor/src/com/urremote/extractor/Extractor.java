package com.urremote.extractor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationFactory;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;

public class Extractor {

	private static final DateFormat DATETIME_FMT = new SimpleDateFormat(Constants.STD_DATE_TIME_FORMAT);

	private Context context;
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
	
	public Extractor(Context context) {
		this.context = context;

		this.primaryAccount = PrimaryAccountUtil.getPrimaryAccount(context);
		this.mainLooperHandler = new Handler(context.getMainLooper());
		this.utils = MyTracksProviderUtils.Factory.get(context);

		this.parentFolder = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + File.separator + "Extractor");
		if (!this.parentFolder.exists()) {
			this.parentFolder.mkdirs();
		}
	}

	public void runOnUi(final Runnable runnable) {
		mainLooperHandler.post(runnable);
	}
	
	private void showToastUi(final String text) {
		Log.d(Constants.TAG, text);
		runOnUi(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public void extract() {
    	
    	for (File f:this.parentFolder.listFiles()) {
    		if (!f.isDirectory())
    			f.delete();
    	}
    	
    	String logFName = DATETIME_FMT.format(new Date(System.currentTimeMillis()));
    	logFName = toFilename(logFName) + ".log";
		File logFile = new File(this.parentFolder.getAbsolutePath()
				+ File.separator + logFName);
		
		try {
			PrintStream logOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile)));
	    	List<Track> allTracks = utils.getAllTracks();
	    	for (int trackIndex=0; trackIndex<allTracks.size(); ++trackIndex) {
	    		Track track = allTracks.get(trackIndex);
	    		
	    		showToastUi("Preparing " + track.getName() + "("+(trackIndex+1)+"/"+allTracks.size()+")");
	    		
	    		try {
	    			writeTrack(logOut, track);
	    		} catch (Exception e) {
	    			Log.e(Constants.TAG, "Error while writing track details to SDCard", e);
	    			showToastUi("Error: "+e.getMessage());
	    		}
	    	}
	    	
	    	List<File> sendFiles = new ArrayList<File>();
	    	for (File f:this.parentFolder.listFiles()) {
	    		if (!f.isDirectory())
	    			sendFiles.add(f);
	    	}
	    	
//	    	showToastUi("Emailing Data");
//	    	email(Constants.EMAIL_TO, "", "MyTracks Data: "+primaryAccount, "", sendFiles);
	    	
	    	showToastUi("Done");
		} catch (FileNotFoundException e1) {
			throw new RuntimeException("Error while trying to create log file", e1);
		}
		
    	
    }

	private String toFilename(String value) {
		return value.replaceAll("[^A-Za-z0-9_-]+", "_");
	}
	
	private Long getFirstLocationTime(Track track) {
		LocationIterator iterator = utils.getLocationIterator(track.getId(),
				-1, false, locationFactory);
		try {
			if (iterator.hasNext()) {
				MyTracksLocation location = (MyTracksLocation) iterator.next();
				return location.getTime();
			} else {
				return null;
			}
		} finally {
			iterator.close();
		}
	}

	private void writeTrack(PrintStream logOut, Track track) throws FileNotFoundException {
		
		Long firstTimestamp = getFirstLocationTime(track);
		
		if (firstTimestamp==null) {
			logOut.println("Track '"+track.getName()+"' had no data. No output file generated.");
			return;
		}
		
		String fname = toFilename(primaryAccount + "--" + DATETIME_FMT.format(new Date(firstTimestamp)))
				+ ".txt";
		
		File file = new File(this.parentFolder.getAbsolutePath()
				+ File.separator + fname);

		PrintStream out = new PrintStream(new BufferedOutputStream(
				new FileOutputStream(file)));
		
		logOut.println(fname+" is Track '"+track.getName()+"'");

		LocationIterator iterator = utils.getLocationIterator(track.getId(),
				-1, false, locationFactory);
		try {
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
		} finally {
			iterator.close();
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
		context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}	
}
