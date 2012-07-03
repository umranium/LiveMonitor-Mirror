package au.csiro.umran.test.readblue;

import java.io.File;

import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Environment;


public class Constants {

	public final static String TAG = "ReadBlue";
	
	public final static int BUFFERED_MSG_COUNT = 500*60*2; // 2min @ 500Hz
	public final static int BUFFER_MSG_LENGTH = 32;
	
	public static final String STD_DATE_FORMAT = "dd-MM-yyyy";
	public static final String STD_TIME_FORMAT = "HH-mm-ss";
	public static final String STD_DATE_TIME_FORMAT = STD_DATE_FORMAT+" "+STD_TIME_FORMAT;
	
	public static final String PATH_SD_CARD_APP_LOC = Environment.getExternalStorageDirectory() + File.separator + "Bridge";
	
	public static final int ALERT_LONG_SOUND_TYPE = RingtoneManager.TYPE_ALARM;
	public static final int ALERT_SHORT_SOUND_TYPE = RingtoneManager.TYPE_NOTIFICATION;
	public static final int ALERT_STREAM_TYPE = AudioManager.STREAM_NOTIFICATION;
	
	public static final int[] POSIBLE_DEVICE_FREQUENCIES = new int[] {100,250,500}; 
	public static final long MIN_TIME_FOR_FREQ_DET = 5000L; // ms
	
	public static final double MIN_SD_CALIBRATION_MOVEMENT = 1.0; // ms
	public static final int DURATION_CALIBARATION = 2000; // ms
	
	
}
