package com.urremote.bridge.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;


import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CustomUncaughtExceptionHandler implements
		UncaughtExceptionHandler {
	
	public static void setInterceptHandler(Context context, Thread thread) {
		UncaughtExceptionHandler prevHandler = thread.getUncaughtExceptionHandler();
		
		if (!(prevHandler instanceof CustomUncaughtExceptionHandler)) {
			CustomUncaughtExceptionHandler handler = new CustomUncaughtExceptionHandler(context, prevHandler);
			thread.setUncaughtExceptionHandler(handler);
		}
	}

    private static String getAppName(Context context) {
        try {
            return context.getPackageManager().getApplicationLabel(context
                    .getPackageManager().getPackageInfo(context.getPackageName(), 0)
                    .applicationInfo).toString().replaceAll("[^A-Za-z]", "");
        } catch (NameNotFoundException ex) {
            return "Unknown";
        }
    }
    
    private static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException ex) {
            return "Unknown";
        }
    }
	
    
    private String appName;
    private String version;
	private UncaughtExceptionHandler prev;
	
	public CustomUncaughtExceptionHandler(Context context, UncaughtExceptionHandler prev) {
		this.appName = getAppName(context);
		this.version = getVersionName(context);
		this.prev = prev;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e(Constants.TAG, "Uncaught exception in thread: "+thread.getName(), ex);
		
		StringWriter strWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(strWriter);
		
		writer.println("Application: "+appName);
		writer.println("Version: "+version);
		writer.println("Uncaught exception in thread: "+thread.getName());
		ex.printStackTrace(writer);
		
		writer.close();
		
		String crashReport = strWriter.toString();
		
		writeToFile(crashReport, Long.toString(System.currentTimeMillis())+".stacktrace");
//		sendToServer(crashReport);
		
		if (this.prev!=null)
			this.prev.uncaughtException(thread, ex);
	}
	
    private void writeToFile(String crashReport, String filename) {
        try {
        	File outputFile = new File(Constants.PATH_SD_CARD_APP_LOC + File.separator + filename);
        	
        	if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        		Log.e(Constants.TAG, "Unable to write stacktrace to file. External media not mounted!");
        		return;
        	}
        	
        	Log.v(Constants.TAG, "Output Trace File:"+outputFile);
        	if (!outputFile.getParentFile().exists()) {
        		if (!outputFile.getParentFile().mkdirs()) {
                    Log.e(Constants.TAG, 
                    		"Unable to create log file directory:\n"+
                    		outputFile.getParentFile()+
                    		"\nCaused when writing stack to file:\n"+
                    		crashReport);
        		}
        	}
        	if (!outputFile.exists()) {
        		if (!outputFile.createNewFile()) {
                    Log.e(Constants.TAG, 
                    		"Unable to create log file:\n"+
                    		outputFile.getParentFile()+
                    		"\nCaused when writing stack to file:\n"+
                    		crashReport);
        		}
        	}
            BufferedWriter bos = new BufferedWriter(new FileWriter(outputFile));
            bos.write(crashReport);
            bos.flush();
            bos.close();
            Log.e(Constants.TAG, crashReport);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToServer(String crashReport) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(Constants.URI_CRASH_REPORT);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("crashreport", crashReport));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            httpClient.execute(httpPost);
        } catch (IOException e) {
            // Do nothing
        }
    }	
	
}
