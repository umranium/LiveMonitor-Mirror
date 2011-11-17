package au.csiro.umran.test.readblue;

public class SystemMessage {
	
	public final long timeStamp;
	public final String message;
	
	public SystemMessage(long timeStamp, String message) {
		this.timeStamp = timeStamp;
		this.message = message;
	}
	
	@Override
	public String toString() {
		return this.message;
	}
	
}
