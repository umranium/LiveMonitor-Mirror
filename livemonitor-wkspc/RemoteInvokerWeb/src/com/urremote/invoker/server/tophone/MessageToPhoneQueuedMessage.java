package com.urremote.invoker.server.tophone;

import com.urremote.invoker.server.model.QueuedMessageToPhone;

public class MessageToPhoneQueuedMessage extends MessageToPhone {

	private QueuedMessageToPhone queuedMessageToPhone;

	public MessageToPhoneQueuedMessage(QueuedMessageToPhone queuedMessageToPhone) {
		super(MessageToPhoneType.QUEUED_MESSAGE);
		this.queuedMessageToPhone = queuedMessageToPhone;
	}
	
	public QueuedMessageToPhone getQueuedMessageToPhone() {
		return queuedMessageToPhone;
	}
	
	public void setQueuedMessageToPhone(
			QueuedMessageToPhone queuedMessageToPhone) {
		this.queuedMessageToPhone = queuedMessageToPhone;
	}
	
}
