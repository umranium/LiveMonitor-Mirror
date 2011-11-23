package com.urremote.invoker.shared;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;

public class DeviceChannelMessage {
	
	public static enum MessageType {
		DeviceAdded, DeviceDeleted, UpdateTimeChanged, IsRecordingStateChanged
	}
	
	private MessageType messageType;
	private String device;
	private Long newUpdateTime;
	private Boolean newIsRecordingState;
	
	private DeviceChannelMessage(MessageType messageType, String device) {
		this.messageType = messageType;
		this.device = device;
	}
	
	public MessageType getMessageType() {
		return messageType;
	}
	
	public String getDevice() {
		return device;
	}
	
	public Long getNewUpdateTime() {
		return newUpdateTime;
	}
	
	public void setNewUpdateTime(Long newUpdateTime) {
		this.newUpdateTime = newUpdateTime;
	}
	
	public Boolean getNewIsRecordingState() {
		return newIsRecordingState;
	}
	
	public void setNewIsRecordingState(Boolean newIsRecordingState) {
		this.newIsRecordingState = newIsRecordingState;
	}
	
	@Override
	public String toString() {
		switch (messageType) {
		case DeviceAdded:
			return device+" "+messageType.toString();
		case DeviceDeleted:
			return device+" "+messageType.toString();
		case IsRecordingStateChanged:
			return device + " "+ messageType.toString() + " " + newIsRecordingState;
		case UpdateTimeChanged:
			return device + " "+ messageType.toString() + " " + newUpdateTime;
		default:
			throw new RuntimeException("Unknown message type: "+messageType.toString()+". Unupdated code.");
		}
	}
	
	public static DeviceChannelMessage createDeviceAddedMsg(String device) {
		DeviceChannelMessage msg = new DeviceChannelMessage(MessageType.DeviceAdded, device);
		return msg;
	}
	
	public static DeviceChannelMessage createDeviceDeletedMsg(String device) {
		DeviceChannelMessage msg = new DeviceChannelMessage(MessageType.DeviceDeleted, device);
		return msg;
	}
	
	public static DeviceChannelMessage createUpdateTimeChangedMsg(String device, long newUpdateTime) {
		DeviceChannelMessage msg = new DeviceChannelMessage(MessageType.UpdateTimeChanged, device);
		msg.setNewUpdateTime(newUpdateTime);
		return msg;
	}
	
	public static DeviceChannelMessage createIsRecordingStateChangedMsg(String device, boolean newState) {
		DeviceChannelMessage msg = new DeviceChannelMessage(MessageType.IsRecordingStateChanged, device);
		msg.setNewIsRecordingState(newState);
		return msg;
	}
	
	public static String encode(List<DeviceChannelMessage> messages) {
		List<String> tokens = new ArrayList<String>();
		
		tokens.add(Integer.toString(messages.size()));
		
		for (DeviceChannelMessage message:messages) {
			tokens.add(Integer.toString(message.messageType.ordinal()));
			tokens.add(message.device);
			
			switch (message.messageType) {
			case IsRecordingStateChanged:
				tokens.add(message.newIsRecordingState.toString());
				break;
			case UpdateTimeChanged:
				tokens.add(message.newUpdateTime.toString());
				break;
			}
		}
		
		return MessageTokenizer.encodeTokens(tokens);
	}
	
	public static List<DeviceChannelMessage> decode(String msg) throws MessageParsingException {
		try {
			List<String> tokens = MessageTokenizer.parseTokens(msg);
			
			int numOfMessages = Integer.parseInt(retrieveToken(msg, tokens, 0));
			
			List<DeviceChannelMessage> messages = new ArrayList<DeviceChannelMessage>(numOfMessages);
			
			int tokenIndex = 1;
			for (int msgIndex=0; msgIndex<numOfMessages; ++msgIndex) {
				
				MessageType messageType = MessageType.values()[Integer.parseInt(retrieveToken(msg, tokens, tokenIndex))];
				++tokenIndex;
				
				String device = retrieveToken(msg, tokens, tokenIndex);
				++tokenIndex;
				
				if (messageType==null || device.isEmpty()) {
					throw new MessageParsingException("Invalid message: \""+msg+"\"");
				}
				
				DeviceChannelMessage message = new DeviceChannelMessage(messageType, device);
				
				switch (messageType) {
				case UpdateTimeChanged: {
					message.setNewUpdateTime(Long.parseLong(retrieveToken(msg, tokens, tokenIndex)));
					++tokenIndex;
					break;
				}
				case IsRecordingStateChanged: {
					message.setNewIsRecordingState(Boolean.parseBoolean(retrieveToken(msg, tokens,
							tokenIndex)));
					++tokenIndex;
					break;
				}
				}
				
				messages.add(message);
			}
	
			return messages;
		} catch (Exception e) {
			throw new MessageParsingException(e);
		}
	}
	
	private static String retrieveToken(String msg, List<String> tokens, int index) throws MessageParsingException {
		if (index>=tokens.size())
			throw new MessageParsingException("Invalid message: \""+msg+"\"");
		return tokens.get(index);
	}
	
}
