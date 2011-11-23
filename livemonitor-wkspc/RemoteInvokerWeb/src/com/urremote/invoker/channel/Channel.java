package com.urremote.invoker.channel;

import com.google.gwt.core.client.JavaScriptObject;

public class Channel extends JavaScriptObject {
	
	protected Channel() {
		super();
	}

	public final native Socket open(SocketListener listener) /*-{
	    var socket = this.open();
	    socket.onopen = function(event) {
	    	listener.@com.urremote.invoker.channel.SocketListener::onOpen()();
    	};
	    socket.onmessage = function(event) {
	    	listener.@com.urremote.invoker.channel.SocketListener::onMessage(Ljava/lang/String;)(event.data);
    	};
	    return socket;
	  }-*/;

}
