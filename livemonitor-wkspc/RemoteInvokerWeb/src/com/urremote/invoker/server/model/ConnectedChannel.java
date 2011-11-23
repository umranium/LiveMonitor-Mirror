package com.urremote.invoker.server.model;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class ConnectedChannel {
	
	@PrimaryKey
	String id;
	
	// when sending channel messages, only send messages of this device to to this channel
	@Persistent
	String deviceFilter;

	public ConnectedChannel(String id) {
		this.id = id;
		this.deviceFilter = null;
	}
	
	public String getId() {
		return id;
	}
	
	public String getFilterDevice() {
		return deviceFilter;
	}
	
	public void setFilterDevice(String filterDevice) {
		this.deviceFilter = filterDevice;
	}
	
}
