package com.urremote.invoker.server;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
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
import com.urremote.invoker.server.model.PhoneDetails;

@SuppressWarnings("serial")
public class DeviceDetailsServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(DeviceDetailsServlet.class.getName());

	private static final String htmlTemplate;
	
	static {
		String template = "<html><body><h2>Server error, please try again later</h2></body></html>";
		try {
			String templatePath = "html/device_details_template.html";
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
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		//	get logged in user
		if (Constants.AUTHENTICATE_USER) {
			UserService userService = UserServiceFactory.getUserService();

			if (userService.getCurrentUser() == null) {
				String url = getCompleteUri(req, null);
				
				String loginUrl = userService.createLoginURL(url);
				
				resp.sendRedirect(resp.encodeRedirectURL(loginUrl));
				
				return;
			}
		}
		
		String deviceId = req.getPathInfo();
		
		log.info("Serving device details page: deviceId="+deviceId+" ("+req.getRequestURI()+")");
		
		//	check if there is a device specified
		if (deviceId==null || deviceId.isEmpty()) {
			String url = getCompleteUri(req, "/welcome");
			log.info("device not found in path, redirecting to welcome page:"+url);
			resp.sendRedirect(resp.encodeRedirectURL(url));
			return;
		}
		
		if (deviceId.startsWith("/"))
			deviceId = deviceId.substring(1);
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			PhoneDetails phoneDetails = pm.getObjectById(PhoneDetails.class, deviceId);
			
			HttpSession session = req.getSession(true);
			String sessionId = session.getId();
			ChannelService channelService = ChannelServiceFactory.getChannelService();
			String channelToken = channelService.createChannel(sessionId);
			
			log.info("Channel token prepared for device-details session-id='"+session.getId()+"'");
			
			String html = htmlTemplate.replace("{{DEVICE_ID}}", phoneDetails.getAccount())
					.replace("{{DEVICE_NAME}}", phoneDetails.getAccount())
					.replace("{{CHANNEL_TOKEN}}", channelToken);
			
		    resp.setContentType("text/html");
		    resp.getWriter().write(html);
		} catch (Exception e) {
			String url = getCompleteUri(req, "/welcome");
			log.log(Level.SEVERE, "An error occurred while trying to serve page for device: "+deviceId, e);
			resp.sendRedirect(resp.encodeRedirectURL(url));
		} finally {
			pm.close();
		}
		
		
	}


	private String getCompleteUri(HttpServletRequest req, String path)
			throws IOException {
		try {
			String query = req.getQueryString();
			URI thisUri = new URI(req.getRequestURL().toString());
			URI uriWithOptionalGameParam = new URI(thisUri.getScheme(), thisUri.getUserInfo(),
					thisUri.getHost(), thisUri.getPort(), (path==null)?thisUri.getPath():path, query, "");
			return uriWithOptionalGameParam.toString();
		} catch (URISyntaxException e) {
			throw new IOException(e.getMessage());
		}
	}
	
	
}
