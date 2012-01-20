package com.urremote.bridge.mapmymaps;

@SuppressWarnings("serial")
public class UnparsableReplyException extends MapMyMapsException {

	public UnparsableReplyException(String reply) {
		super("Unparsable Reply: "+reply);
	}

	public UnparsableReplyException(Throwable throwable) {
		super(throwable);
	}

	public UnparsableReplyException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
