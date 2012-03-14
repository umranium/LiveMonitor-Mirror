package au.csiro.umran.test.readblue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	
	private static final DateFormat dateFormat = new SimpleDateFormat(Constants.STD_DATE_TIME_FORMAT);//DateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
	
	public static void setInterceptHandler(Context context, Thread thread) {
		internSetInterceptHandler(context, thread);
		
		ThreadGroup group = thread.getThreadGroup();
		int count = group.activeCount();
		Thread[] threads = new Thread[count];
		int enumeratedCount = group.enumerate(threads, true);
		
		for (int i=0; i<enumeratedCount; ++i) {
			internSetInterceptHandler(context, threads[i]);
		}
		
//		Log.d(Constants.TAG+"-THREADS", "threads-checked = "+enumeratedCount);
	}
	
	private static void internSetInterceptHandler(Context context, Thread thread) {
		UncaughtExceptionHandler prevHandler = thread.getUncaughtExceptionHandler();
		
		if (!(prevHandler instanceof CustomUncaughtExceptionHandler)) {
			CustomUncaughtExceptionHandler handler = new CustomUncaughtExceptionHandler(context, prevHandler);
			thread.setUncaughtExceptionHandler(handler);
//			Log.d(Constants.TAG+"-THREADS", "Set CustomUncaughtExceptionHandler for "+thread.getName());
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
	
    
    private Context context;
	private UncaughtExceptionHandler prev;
	
	public CustomUncaughtExceptionHandler(Context context, UncaughtExceptionHandler prev) {
		this.context = context;
		this.prev = prev;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e(Constants.TAG, "Uncaught exception in thread: "+thread.getName(), ex);
		
		reportCrash(context, thread, ex);
		
		if (this.prev!=null)
			this.prev.uncaughtException(thread, ex);
	}
	
	public static void reportCrash(Context context, Thread thread, Throwable ex) {
		String crashReport = createCrashReport(context, thread, ex); 
		writeToFile(crashReport);
	}
	
	private static String createCrashReport(Context context, Thread thread, Throwable ex) {
		StringWriter strWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(strWriter);
		
		writer.println("Application: "+getAppName(context));
		writer.println("Version: "+getVersionName(context));
		writer.println("Uncaught exception in thread: "+thread.getName());
		ex.printStackTrace(writer);
		
		writer.close();
		
		return strWriter.toString();
	}
	
	private static String timeToFilename(long time) {
		return dateFormat.format(new Date(time)).replaceAll("[^0-9A-Za-z]", "-");
	}
	
	private static void writeToFile(String crashReport) {
        try {
        	File outputFile = new File(Constants.PATH_SD_CARD_APP_LOC + File.separator +
        			timeToFilename(System.currentTimeMillis())+".stacktrace");
        	
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

	
}
