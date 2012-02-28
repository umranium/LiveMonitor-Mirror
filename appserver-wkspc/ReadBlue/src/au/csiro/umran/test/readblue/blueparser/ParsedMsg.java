package au.csiro.umran.test.readblue.blueparser;

public class ParsedMsg {
	
	public final byte[] msg;
	public int msgLen;
	public long time;
	
	public ParsedMsg(int len) {
		msg = new byte[len];
	}

}
