package com.urremote.invoker.server;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.urremote.invoker.server.common.Constants;
import com.urremote.invoker.server.model.ConnectedChannel;

@SuppressWarnings("serial")
public class DeviceListServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(DeviceListServlet.class.getName());
	
	private static final String htmlTemplate;
	
	static {
		String template = "<html><body><h2>Server error, please try again later</h2></body></html>";
		try {
			String templatePath = "html/device_list_template.html";
		    FileReader reader = new FileReader(templatePath);
		    CharBuffer buffer = CharBuffer.allocate(16384);
		    reader.read(buffer);
		    template = new String(buffer.array());
		} catch (FileNotFoundException e) {
			log.log(Level.SEVERE, "Error while trying to load html template", e);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while trying to load html template", e);
		}
		htmlTemplate = template;
	}
	
	@Override
	public void init() throws ServletException {
		super.init();
		
		//	clear all channels
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery(ConnectedChannel.class);
			
			@SuppressWarnings("unchecked")
			Collection<ConnectedChannel> channels = (Collection<ConnectedChannel>)q.execute();
			
			pm.deletePersistentAll(channels);
		} finally {
			pm.close();
		}
		
		log.severe("All connected channel records cleared.");
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		//	get logged in user
		if (Constants.AUTHENTICATE_USER) {
			UserService userService = UserServiceFactory.getUserService();

			if (userService.getCurrentUser() == null) {
				String url = getCompleteUri(req);
				
				String loginUrl = userService.createLoginURL(url);
				
				resp.sendRedirect(resp.encodeRedirectURL(loginUrl));
				
				return;
			}
		}
		
		log.severe("Serving device list page ("+req.getRequestURI()+")");

		HttpSession session = req.getSession(true);
		String sessionId = session.getId();		
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		String channelToken = channelService.createChannel(sessionId);
		
		if (htmlTemplate.contains("{{CHANNEL_TOKEN}}"))
			log.severe("Serving channel token into the html");
		else
			log.severe("Channel token not served");
		
		String html = htmlTemplate.replace("{{CHANNEL_TOKEN}}", channelToken);
		
	    resp.setContentType("text/html");
	    resp.getWriter().write(html);
    }

	
	private String getCompleteUri(HttpServletRequest req)
			throws IOException {
		try {
			String query = req.getQueryString();
			URI thisUri = new URI(req.getRequestURL().toString());
			URI uriWithOptionalGameParam = new URI(thisUri.getScheme(), thisUri.getUserInfo(),
					thisUri.getHost(), thisUri.getPort(), thisUri.getPath(), query, "");
			return uriWithOptionalGameParam.toString();
		} catch (URISyntaxException e) {
			throw new IOException(e.getMessage());
		}

	}
		  	
}
