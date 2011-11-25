package com.urremote.invoker.server.model;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class MessageToPhone {
	
	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long key;
	
	@Persistent
	private String deviceId;
	
	@Persistent
	private long timestamp;
	
	@Persistent
	private String collapseKey;
	
	@Persistent
	private String messageDataKey;
	
	@Persistent
	private String messageDataContent;

	public MessageToPhone(String deviceId, long timestamp, String collapseKey,
			String messageDataKey, String messageDataContent) {
		this.deviceId = deviceId;
		this.timestamp = timestamp;
		this.collapseKey = collapseKey;
		this.messageDataKey = messageDataKey;
		this.messageDataContent = messageDataContent;
	}

	public String getDeviceId() {
		return deviceId;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public String getCollapseKey() {
		return collapseKey;
	}
	
	public String getMessageDataKey() {
		return messageDataKey;
	}
	
	public String getMessageDataContent() {
		return messageDataContent;
	}

}