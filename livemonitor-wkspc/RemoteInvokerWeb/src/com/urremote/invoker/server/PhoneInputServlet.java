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
import com.urremote.invoker.shared.DeviceChannelMessage;

@SuppressWarnings("serial")
public class PhoneInputServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(PhoneInputServlet.class.getName());
	
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
		
		if (account==null || account.isEmpty() || type==null || type.isEmpty()) {
			returnInvalidParamMessage(req, resp);
			return;
		}
		
		Set<C2dmDeviceUpdateMessage.MessageType> msgTypes = C2dmDeviceUpdateMessage.decode(type);
		
		for (C2dmDeviceUpdateMessage.MessageType msgType:msgTypes) {
			if (!req.getParameterMap().containsKey(msgType.toString())) {
				returnInvalidParamMessage(req, resp);
				return;
			}
		}
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			PhoneDetails phoneDetails = pm.getObjectById(PhoneDetails.class, account);
			List<DeviceChannelMessage> msgsToChannel = new ArrayList<DeviceChannelMessage>();
			
			for (C2dmDeviceUpdateMessage.MessageType msgType:msgTypes) {
				String value = req.getParameter(msgType.toString());
				switch (msgType) {
				case RecordingStateUpdate:
				{
					Boolean newState = Boolean.parseBoolean(value);
					if (phoneDetails.getIsRecording()!=newState) {
						phoneDetails.setIsRecording(newState);
						log.severe("phone '"+account+"' recording state changed to "+newState);
						DeviceChannelMessage channelMsg = DeviceChannelMessage.createIsRecordingStateChangedMsg(account, newState);
						msgsToChannel.add(channelMsg);
					}
					break;
				}
				}
			}
			
			long currentTime = System.currentTimeMillis();
			phoneDetails.setLastPhoneUpdate(currentTime);
			log.severe("phone '"+account+"' last update time set to "+currentTime);
			DeviceChannelMessage channelMsg = DeviceChannelMessage.createUpdateTimeChangedMsg(account, currentTime);
			msgsToChannel.add(channelMsg);
			
			String channelMsgStr = DeviceChannelMessage.encode(msgsToChannel);
			ChannelMessagingUtil.broadcastMessage(phoneDetails.getAccount(), channelMsgStr);
			
			
			resp.setContentType("text/plain");
			resp.getWriter().println("Message received.");
			resp.setStatus(200);
		} finally {
			pm.close();
		}
	}
	
	private void returnInvalidParamMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("Invalid parameters! Received: "+req.getParameterMap().toString());
		resp.setStatus(400);
	}
	
}
