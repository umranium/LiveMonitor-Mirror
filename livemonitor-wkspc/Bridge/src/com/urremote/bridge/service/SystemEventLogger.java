package com.urremote.bridge.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.os.Environment;
import android.util.Log;

import com.urremote.bridge.common.Constants;

public class SystemEventLogger implements UpdateListener {
	
	private PrintStream log = null;
	
	private ILiveMonitorBinder binder;
	
	private long lastSystemMessage = 0;
	
	public SystemEventLogger(ILiveMonitorBinder binder) {
    	if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
    		Log.e(Constants.TAG, "Unable to write stacktrace to file. External media not mounted!");
    		return;
    	}
    	
		DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT); 
		String filename = dateFormatter.format(new Date(System.currentTimeMillis())).replaceAll("[^A-Za-z0-9]+", "-") + ".log";
    	File outputFile = new File(Constants.PATH_SD_CARD_APP_LOC + File.separator + filename);
    	
    	this.binder = binder;
    	
    	try {
        	if (!outputFile.getParentFile().exists()) {
        		if (!outputFile.getParentFile().mkdirs()) {
                    Log.e(Constants.TAG, 
                    		"Unable to create log file directory:\n"+
                    		outputFile.getParentFile());
                    return;
        		}
        	}
        	
    		if (!outputFile.exists())
    			outputFile.createNewFile();
    		
			log = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)));

			log.println("Logging started.");
    		Log.i(Constants.TAG, "Logging to: "+outputFile);
    		
    	} catch (FileNotFoundException e) {
			Log.e(Constants.TAG, "Error while creating log", e);
		} catch (IOException e) {
			Log.e(Constants.TAG, "Error while creating log", e);
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		log.close();
		super.finalize();
	}

	@Override
	public boolean lauchMyTracks() {
		return false;
	}

	@Override
	public void updateSystemMessages() {
		if (log!=null) {
			List<SystemMessage> sysMsgs = binder.getSystemMessages();
			for (SystemMessage msg:sysMsgs) {
				if (msg.timeStamp>lastSystemMessage) {
					lastSystemMessage = msg.timeStamp;
					log.println(msg.timeStamp+":"+msg.message);
					log.flush();
				}
			}
			
		}
	}

	@Override
	public void onSystemStart() {
		if (log!=null) {
			log.println("System Started");
			log.flush();
		}
	}

	@Override
	public void onSystemStop() {
		if (log!=null) {
			log.println("System Stopped");
			log.flush();
		}
	}

}
