package com.urremote.invoker.server;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.urremote.invoker.server.c2dm.C2dmDeviceRegistrationMessage;
import com.urremote.invoker.server.common.Constants;
import com.urremote.invoker.server.model.PhoneDetails;
import com.urremote.invoker.shared.DeviceChannelMessage;

@SuppressWarnings("serial")
public class RegisterPhoneServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(RegisterPhoneServlet.class.getName());
	
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
		String type = req.getParameter(Constants.C2DM_MSG_PARAM_TYPE);
		
		if (account==null || type==null) {
			returnInvalidMessage(req, resp);
			return;
		}
		
		C2dmDeviceRegistrationMessage.MessageType msgType =
				C2dmDeviceRegistrationMessage.MessageType.valueOf(type);
		
		if (msgType==null) {
			returnInvalidMessage(req, resp);
			return;
		}
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			switch (msgType) {
			case RegisterDevice:
			{
				String deviceId = req.getParameter(Constants.C2DM_MSG_PARAM_DEVICE_ID);
				if (deviceId==null) {
					returnInvalidMessage(req, resp);
					return;
				}
				
				try {
					PhoneDetails old = pm.getObjectById(PhoneDetails.class, account);
					old.setDeviceId(deviceId);
				} catch (javax.jdo.JDOObjectNotFoundException e) {
					PhoneDetails phoneDetails = new PhoneDetails(account, deviceId);
					pm.makePersistent(phoneDetails);
					ChannelMessagingUtil.broadcastMessage(
							DeviceChannelMessage.encode(
									Collections.singletonList(
											DeviceChannelMessage.createDeviceAddedMsg(account))));
				}
				
				log.info("Device '"+account+"' registered.");
				returnSuccessMessage(req, resp);
				return;
			}
			case UnregisterDevice:
			{
				try {
					PhoneDetails details = pm.getObjectById(PhoneDetails.class, account);
					pm.deletePersistent(details);
					ChannelMessagingUtil.broadcastMessage(
							DeviceChannelMessage.encode(
									Collections.singletonList(
											DeviceChannelMessage.createDeviceDeletedMsg(account))
									));
					log.info("Device '"+account+"' unregistered.");
					returnSuccessMessage(req, resp);
				} catch (javax.jdo.JDOObjectNotFoundException e) {
					// probably already deleted
				}
				return;
			}
			}
			
		} finally {
			pm.close();
		}
	}
	
	private void returnSuccessMessage(HttpServletRequest req, HttpServletResponse resp)
			throws IOException  {
		resp.setContentType("text/plain");
		resp.getWriter().println("Success.");
		resp.setStatus(200);
	}
	
	private void returnInvalidMessage(HttpServletRequest req, HttpServletResponse resp)
			throws IOException  {
		resp.setContentType("text/plain");
		resp.getWriter().println("Invalid parameters! Received: "+req.getParameterMap().toString());
		resp.setStatus(400);
	}
	
}
