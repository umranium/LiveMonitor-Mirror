package com.urremote.invoker.server;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.urremote.invoker.server.model.ConnectedChannel;

public class ChannelMessagingUtil {
	
	private static final Logger log = Logger.getLogger(ChannelMessagingUtil.class.getName());
	
	public static void broadcastMessage(String message) {
		log.info("broadcasting message '"+message+"'");
		
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery(ConnectedChannel.class);
			
			@SuppressWarnings("unchecked")
			Collection<ConnectedChannel> channels = (Collection<ConnectedChannel>)q.execute();
			
			//byte[] msgBytes = message.getBytes();
			String webSafe = message;//new String(Base64Coder.encode(msgBytes));
			
			for (ConnectedChannel channel:channels) {
				log.severe("sending message to "+channel.getId()+": "+webSafe);
				ChannelMessage chanMsg = new ChannelMessage(channel.getId(), webSafe);
				channelService.sendMessage(chanMsg);
			}
		} finally {
			pm.close();
		}
	}

	public static void broadcastMessage(String interestedDevice, String message) {
		log.info("broadcasting message '"+message+"' for device '"+interestedDevice+"'");
		
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery(ConnectedChannel.class);
			
			@SuppressWarnings("unchecked")
			Collection<ConnectedChannel> channels = (Collection<ConnectedChannel>)q.execute();
			
			//byte[] msgBytes = message.getBytes();
			String webSafe = message;//new String(Base64Coder.encode(msgBytes));
			
			for (ConnectedChannel channel:channels) {
				String channelDevice = channel.getFilterDevice();
				
				//	either interested device for the channel is not set, or it is set to this particular device
				if (channelDevice==null || channelDevice.equals(interestedDevice)) {
					log.info("broadcasting message '"+message+"' to '"+channel.getId()+"'");
					ChannelMessage chanMsg = new ChannelMessage(channel.getId(), message);
					channelService.sendMessage(chanMsg);
				}
			}
		} finally {
			pm.close();
		}
	}
	
}
