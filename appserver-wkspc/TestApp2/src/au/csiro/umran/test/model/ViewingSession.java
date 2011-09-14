package au.csiro.umran.test.model;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jdo.PersistenceManager;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;

@PersistenceCapable
public class ViewingSession implements Serializable {
	
	private static final long serialVersionUID = -8061405108471836088L;

	public static final String DEF_SESSION_KEY = "session";

	@PrimaryKey
	private String sessionKey;
	
	@Persistent
	private Set<String> users = new TreeSet<String>();
	
	public ViewingSession() {
		sessionKey = DEF_SESSION_KEY;
	}
	
	public void channelConnected(String user)
	{
		synchronized (users) {
			System.out.println("connected to: "+user);
			if (!users.contains(user)) {
				users.add(user);
			} else {
				System.out.println("User already exists: "+user);
			}
			System.out.println("number of users: "+users.size());
		}
	}
	
	public void channelDisconnected(String user)
	{
		synchronized (users) {
			System.out.println("disconnected from: "+user);
			if (users.contains(user)) {
				users.remove(user);
			} else {
				System.out.println("Couldn't find user: "+user);
			}
			System.out.println("number of users: "+users.size());
		}
	}
	
	public Set<String> getUsers() {
		return users;
	}
	
	public void updateAllUsers(String msg)
	{
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		synchronized (users) {
			System.out.println("about to send to users: "+users.toString());
			for (String user:users) {
				System.out.println("message to "+user+": "+msg);
				String key = user;
				ChannelMessage channelMsg = new ChannelMessage(key,msg);
				channelService.sendMessage(channelMsg);
			}
		}
	}
	
}
