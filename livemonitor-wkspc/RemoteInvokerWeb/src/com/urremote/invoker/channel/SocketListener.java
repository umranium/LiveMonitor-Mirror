package com.urremote.invoker.channel;

public interface SocketListener {
	
	void onOpen();

	void onMessage(String message);
	
}