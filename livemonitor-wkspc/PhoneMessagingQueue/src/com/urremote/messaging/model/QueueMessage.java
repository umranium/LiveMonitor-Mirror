package com.urremote.messaging.model;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class QueueMessage {

	@PrimaryKey
	@Persistent(valueStrategy=IdGeneratorStrategy.SEQUENCE)
	private Long id;
	
	@Persistent
	protected String deviceId;
	
	@Persistent
	protected long timeStamp;
	
	@Persistent
	protected String key;
	
	@Persistent
	protected String value;
	
	public QueueMessage(String deviceId, long timeStamp, String key, String value) {
		super();
		this.deviceId = deviceId;
		this.timeStamp = timeStamp;
		this.key = key;
		this.value = value;
	}
	
	public String getDeviceId() {
		return deviceId;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
}
