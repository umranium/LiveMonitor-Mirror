package com.urremote.invoker.server.tophone;

public class MessageToPhoneError extends MessageToPhone {
	
	private String errorMessage;

	public MessageToPhoneError(String errorMessage) {
		super(MessageToPhoneType.ERROR);
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
}
