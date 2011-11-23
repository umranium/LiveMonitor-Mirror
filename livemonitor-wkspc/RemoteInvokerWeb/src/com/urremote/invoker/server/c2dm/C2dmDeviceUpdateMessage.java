package com.urremote.invoker.server.c2dm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.urremote.invoker.shared.MessageTokenizer;

public class C2dmDeviceUpdateMessage {
	
	public static enum MessageType {
		RecordingStateUpdate
	}
	
	public static String encode(Set<MessageType> msgTypes) {
		String[] tokens = new String[msgTypes.size()];
		int index = 0; 
		for (MessageType msgType:msgTypes) {
			tokens[index] = msgType.toString();
			++index;
		}
		return MessageTokenizer.encodeTokens(tokens);
	}
	
	public static Set<MessageType> decode(String messageTypeStr) {
		List<String> tokens = MessageTokenizer.parseTokens(messageTypeStr);
		Set<MessageType> msgTypes = new HashSet<MessageType>(); 
		for (String token:tokens) {
			msgTypes.add(MessageType.valueOf(token));
		}
		return msgTypes;
	}

}
