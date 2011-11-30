package com.urremote.invoker.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.urremote.invoker.server.c2dm.C2dmDeviceUpdateMessage;
import com.urremote.invoker.server.common.Constants;
import com.urremote.invoker.server.model.PhoneDetails;
import com.urremote.invoker.server.model.QueuedMessageToPhone;
import com.urremote.invoker.server.tophone.MessageToPhoneError;
import com.urremote.invoker.server.tophone.MessageToPhoneUtil;
import com.urremote.invoker.shared.DeviceChannelMessage;

@SuppressWarnings("serial")
public class PhoneOutputServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(PhoneOutputServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGetOrPost(req, resp);
	} 
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGetOrPost(req, resp);
	}
	
	public void doGetOrPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		String account = req.getParameter(Constants.C2DM_MSG_PARAM_ACCOUNT);
		String lastMessageTimeStr = req.getParameter(Constants.C2DM_MSG_PARAM_LAST_UPDATE);
		
		if (account==null || account.isEmpty() || lastMessageTimeStr==null || lastMessageTimeStr.isEmpty()) {
			returnInvalidParamMessage(req, resp);
			return;
		}
		
		long lastMessageTime;
		try {
			lastMessageTime = Long.parseLong(lastMessageTimeStr);
		} catch (Exception e) {
			returnInvalidParamMessage(req, resp);
			return;
		}
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			javax.jdo.Query q = pm.newNamedQuery(QueuedMessageToPhone.class, "fetchDeviceMessagesSinceLastUpdate");
			List<QueuedMessageToPhone> messages = (List<QueuedMessageToPhone>)q.executeWithArray(new Object[]{account, lastMessageTime});
			
			log.info("device="+account+", since "+lastMessageTime+", messages.size()="+messages.size());
			
		} finally {
			pm.close();
		}
	}
	
	private void returnInvalidParamMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		//resp.getWriter().print(MessageToPhoneUtil.encodeMessage(new MessageToPhoneError("Invalid arguements")));
		resp.getWriter().print("Invalid arguements");
		resp.setStatus(400);
	}
	
}
