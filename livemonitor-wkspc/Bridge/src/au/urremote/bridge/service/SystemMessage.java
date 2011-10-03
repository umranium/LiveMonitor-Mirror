package au.urremote.bridge.service;

import au.urremote.bridge.scroller.ScrollerMessage;

public class SystemMessage extends ScrollerMessage {
	
	public final String message;
	
	public SystemMessage(long timeStamp, String message) {
		super(timeStamp);
		this.message = message;
	}

	@Override
	public String toString() {
		return this.message;
	}
	
}
