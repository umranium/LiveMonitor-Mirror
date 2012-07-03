package au.csiro.umran.test.readblue;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class FileUtils {
	
	private static final DateFormat FNAME_FMT = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
	
	public static void ensureParentFolderExists(File file) {
		
		File parent = file.getParentFile();
		if (!parent.mkdirs() && !parent.exists()) {
			throw new RuntimeException("Unable to create folder:"+parent);
		}
		
	}
	
	private static String deviceNameToFileName(String name) {
		return name.replaceAll("[\\W]+", "_");
	}
	
	private static String timeToFileName(long time) {
		DateFormat fmt = FNAME_FMT;//SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
		
		return fmt.format(new Date(time)).replaceAll("[\\W]+", "_");
	}
	
	public static File getFileForDevice(boolean calibration, String name) {
		File extern = Environment.getExternalStorageDirectory();
		String path = extern.getAbsolutePath()+File.separator+
				"ReadBlue"+File.separator+
				(calibration?"Calibration_":"")+
				deviceNameToFileName(name)+"_"+timeToFileName(System.currentTimeMillis())+".csv";
		return new File(path);
	}

}
