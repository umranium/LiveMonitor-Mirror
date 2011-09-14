package au.csiro.umran.test.servlets;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.csiro.umran.test.PMF;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class ViewServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		process(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		process(req, resp);
	}

	private void process(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		System.out.println("view request");
		
        UserService userService = UserServiceFactory.getUserService();

        String thisURL = req.getRequestURI();
        PrintWriter out = resp.getWriter();
        
        if (req.getUserPrincipal() == null) {
        	resp.setContentType("text/html");
        	out.println("<p>Please <a href=\"" +
                    userService.createLoginURL(thisURL) +
                    "\" >sign in</a>.</p>");
        	return;
        }

        String logoutUrl = userService.createLogoutURL(thisURL); 
        
        String user = req.getUserPrincipal().getName();
        
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		String token = channelService.createChannel(user);
        
        String html = fillTemplate(user, logoutUrl, token);
        
        resp.setContentType("text/html");
        out.println(html);
        
        persistenceManager.close();
        
	}
	
	private String fillTemplate(String userName, String logoutUrl, String token) throws IOException
	{
		FileReader reader = new FileReader("view-template.txt");
	    CharBuffer buffer = CharBuffer.allocate(16384);
	    reader.read(buffer);
	    String index = new String(buffer.array());
	    index = index.replaceAll("\\{\\{ user_name \\}\\}", userName);
	    index = index.replaceAll("\\{\\{ logout_url \\}\\}", logoutUrl);
	    index = index.replaceAll("\\{\\{ token \\}\\}", token);
	    
	    return index;
	}

}
