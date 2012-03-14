package au.csiro.umran.test.readblue;

public class ParsedMsg {
	
	public final byte[] msg;
	public int msgLen;
	public long time;
	
	public ParsedMsg() {
		msg = new byte[Constants.BUFFER_MSG_LENGTH];
	}
	
	public void assign(long time, byte[] msg, int len)
	{
		this.time = time;
		this.msgLen = len;
		for (int i=0; i<len; ++i)
			this.msg[i] = msg[i];
	}

}
