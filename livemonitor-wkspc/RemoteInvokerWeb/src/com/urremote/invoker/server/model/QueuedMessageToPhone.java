package com.urremote.invoker.server.model;

import java.io.Serializable;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.persistence.NamedQuery;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable
@NamedQuery(name="fetchDeviceMessagesSinceLastUpdate",query=
		"SELECT c FROM QueuedMessageToPhone c " +
		"WHERE c.deviceId=device && c.timeStamp>time " +
		"ORDER BY c.collapseKey, c.timeStamp " +
		"PARAMETERS String device, long time")
public class QueuedMessageToPhone implements Serializable {
	
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

	public QueuedMessageToPhone(String deviceId, long timestamp, String collapseKey,
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
