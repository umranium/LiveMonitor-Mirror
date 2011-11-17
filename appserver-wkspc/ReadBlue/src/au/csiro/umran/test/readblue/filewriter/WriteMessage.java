package au.csiro.umran.test.readblue.filewriter;

public class WriteMessage {
	
	public long timeStamp;
	public byte[] message;
	public int msgLen;
	
	public WriteMessage() {
		this.timeStamp = 0;
		this.message = new byte[32];
		this.msgLen = 0;
	}
	
	public void assign(long timeStamp, byte[] message, int msgLen) {
		if (this.message==null || msgLen>this.message.length) {
			this.message = new byte[msgLen+10];
		}
		
		for (int i=0; i<msgLen; ++i)
			this.message[i] = message[i];
		this.msgLen = msgLen;
		this.timeStamp = timeStamp; 
	}
	

}
