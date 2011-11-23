package com.urremote.invoker.channel;

import com.google.gwt.core.client.JavaScriptObject;

public class ChannelFactory {
	
	public static final native Channel createChannel(String channelId) /*-{
		return new $wnd.goog.appengine.Channel(channelId);
	}-*/;
	
}
