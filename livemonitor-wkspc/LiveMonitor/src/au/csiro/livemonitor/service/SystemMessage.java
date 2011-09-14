package au.csiro.livemonitor.service;

import au.csiro.livemonitor.scroller.ScrollerMessage;

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
