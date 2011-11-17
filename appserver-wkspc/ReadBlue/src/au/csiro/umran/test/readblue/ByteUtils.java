package au.csiro.umran.test.readblue;

public class ByteUtils {

	public static String bytesToString(byte[] bytes, int start, int len)
	{
		StringBuilder builder = new StringBuilder();
		
		for (int i=0; i<len && start+i<bytes.length; ++i) {
			if (i>0)
				builder.append(",");
			int val = bytes[start+i] & 0xFF;
			String hex = Integer.toHexString(val);
			if (hex.length()<2)
				builder.append("0"+hex);
			else
				builder.append(hex);
		}
		
		return builder.toString();
	}
	
}
