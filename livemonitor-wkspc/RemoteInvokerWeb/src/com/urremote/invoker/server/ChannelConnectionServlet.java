package com.urremote.invoker.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.urremote.invoker.server.model.ConnectedChannel;
import com.urremote.invoker.server.model.PhoneDetails;

@SuppressWarnings("serial")
public class ChannelConnectionServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(ChannelConnectionServlet.class.getName());

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		ChannelPresence presence = channelService.parsePresence(req);
		
		String clientId = presence.clientId();
		
		log.severe("ChannelConnectionServlet: "+req.getRequestURI());
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {				
			if (presence.isConnected()) {
				log.severe("connected to "+clientId);
				ConnectedChannel channel = new ConnectedChannel(clientId);
				pm.makePersistent(channel);
			} else {
				log.severe("disconnected from "+clientId);
				try {
					ConnectedChannel channel = pm.getObjectById(ConnectedChannel.class, clientId);
					pm.deletePersistent(channel);
				} catch (javax.jdo.JDOObjectNotFoundException e) {
					//	ignore
				}
			}
		} finally {
			pm.close();
		}
		
		log.info(presence.clientId()+" has "+(presence.isConnected()?"connected":"disconnected"));
    }
	
}
