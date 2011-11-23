package com.urremote.invoker.server.model;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class PhoneDetails {

	@PrimaryKey
	String account;
	
	@Persistent
	String deviceId;
	
	@Persistent
	Boolean isRecording;
	
	@Persistent
	Long lastPhoneUpdate;
	
	public PhoneDetails(String account, String deviceId) {
		this.account = account;
		this.deviceId = deviceId;
		this.isRecording = false;
	}
	
	public String getAccount() {
		return account;
	}
	
	public String getDeviceId() {
		return deviceId;
	}
	
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	public Boolean getIsRecording() {
		return isRecording;
	}
	
	public void setIsRecording(Boolean isRecording) {
		this.isRecording = isRecording;
	}
	
	public Long getLastPhoneUpdate() {
		return lastPhoneUpdate;
	}
	
	public void setLastPhoneUpdate(Long lastPhoneUpdate) {
		this.lastPhoneUpdate = lastPhoneUpdate;
	}
	
	@Override
	public String toString() {
		return account+":"+deviceId;
	}
	
	public static void assign(PhoneDetails from, PhoneDetails to) {
		to.account = from.account;
		to.deviceId = from.deviceId;
		to.isRecording = from.isRecording;
		to.lastPhoneUpdate = from.lastPhoneUpdate;
	}
	
	
}
