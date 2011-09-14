package au.csiro.livemonitor.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageContainer {
	
	private int maxMessages;
	private List<SystemMessage> messages;
	private List<SystemMessage> readOnlyList;
	
	public MessageContainer(int maxMessages) {
		this.maxMessages = maxMessages;  
		this.messages = new ArrayList<SystemMessage>(maxMessages);
		this.readOnlyList = Collections.unmodifiableList(messages);
	}
	
	public List<SystemMessage> getMessages() {
		return readOnlyList;
	}
	
	public void addMessage(String msg) {
		messages.add(new SystemMessage(System.currentTimeMillis(), msg));
		while (messages.size()>maxMessages) {
			messages.remove(0);
		}
	}
	
}
