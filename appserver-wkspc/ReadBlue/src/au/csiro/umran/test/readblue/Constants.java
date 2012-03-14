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
	
	public static final int ALERT_SOUND_TYPE = RingtoneManager.TYPE_ALARM;
	public static final int ALERT_STREAM_TYPE = AudioManager.STREAM_NOTIFICATION;
	
}
