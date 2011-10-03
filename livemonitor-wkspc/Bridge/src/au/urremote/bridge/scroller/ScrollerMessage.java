package au.urremote.bridge.scroller;

public abstract class ScrollerMessage {

	public final long timeStamp;
	
	public ScrollerMessage(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public abstract String toString();
	
}
