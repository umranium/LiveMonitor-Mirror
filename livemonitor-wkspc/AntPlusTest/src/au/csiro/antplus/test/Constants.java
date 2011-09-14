package au.csiro.antplus.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;

public class Constants {

	public static final String TAG = "Ant+Test";
	
	/** Pair to any device. */
	public static final short WILDCARD = 0;

	/** The default proximity search bin. */
	public static final byte DEFAULT_BIN = 7;

	/** The default event buffering buffer threshold. */
	public static final short DEFAULT_BUFFER_THRESHOLD = 0;

	public static Set<String> findFinalStaticFieldName(Class cl, String nameRegex, Class type, Object value)
	{
		try {
			Set<String> result = new HashSet<String>();
			for (Field f:cl.getDeclaredFields()) {
				if ((f.getModifiers() & Modifier.FINAL)!=0 &&
					(f.getModifiers() & Modifier.STATIC)!=0 &&
					f.getType().equals(type) &&
					f.getName().matches(nameRegex) &&
					f.get(null).equals(value)) {
					result.add(f.getName());
				}
			}
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Error while finding field name", e);
			return null;
		}
	}
	
    public static String getHexString(byte[] data)
    {
        if(null == data)
        {
            return "";
        }

        StringBuffer hexString = new StringBuffer();
        for(int i = 0;i < data.length; i++)
        {
           hexString.append("[").append(String.format("%02X", data[i] & 0xFF)).append("]");
        }

        return hexString.toString();
    }
    
}
