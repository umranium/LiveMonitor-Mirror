package au.csiro.umran.test.servlets;

import java.io.IOException;
import java.util.HashMap;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.repackaged.org.json.JSONObject;

import au.csiro.umran.test.PMF;
import au.csiro.umran.test.model.ViewingSession;

@SuppressWarnings("serial")
public class ReceiveServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		process(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		process(req, resp);
	}
	
	private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		System.out.println("data received");
		resp.setContentType("text/plain");
        if (!req.getParameterMap().isEmpty()) {
            PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
            
            ViewingSession session = PMF.getSession(persistenceManager);
            
    		JSONObject jsonObject = new JSONObject(req.getParameterMap());
    		String message = jsonObject.toString();
    		System.out.println("data: "+message);
    		session.updateAllUsers(message);
    		
    		persistenceManager.close();
    		
    		resp.getWriter().println("thanks for your input.");
    		resp.setStatus(200);
        } else {
    		resp.getWriter().println("we couldn't find any parameters! did you forget to give some?");
    		resp.setStatus(401);
        }
	}
	
	
}

