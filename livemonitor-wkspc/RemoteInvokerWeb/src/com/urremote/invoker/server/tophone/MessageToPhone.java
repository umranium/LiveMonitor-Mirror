package com.urremote.invoker.server.tophone;

import java.io.Serializable;

public class MessageToPhone implements Serializable {
	
	private MessageToPhoneType messageToPhoneType;

	public MessageToPhone(MessageToPhoneType messageToPhoneType) {
		super();
		this.messageToPhoneType = messageToPhoneType;
	}
	
	public MessageToPhoneType getMessageToPhoneType() {
		return messageToPhoneType;
	}
	
	public void setMessageToPhoneType(MessageToPhoneType messageToPhoneType) {
		this.messageToPhoneType = messageToPhoneType;
	}

}
