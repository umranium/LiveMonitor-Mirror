package au.csiro.umran.test.readblue;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class FileUtils {
	
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
		DateFormat fmt = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
		
		return fmt.format(new Date(time)).replaceAll("[\\W]+", "_");
	}
	
	public static File getFileForDevice(String name) {
		File extern = Environment.getExternalStorageDirectory();
		String path = extern.getAbsolutePath()+File.separator+
				"ReadBlue"+File.separator+
				deviceNameToFileName(name)+"_"+timeToFileName(System.currentTimeMillis())+".csv";
		return new File(path);
	}

}
