package com.urremote.messaging.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import com.urremote.messaging.common.Constants;
import com.urremote.messaging.common.Pair;
import com.urremote.messaging.dao.PMF;
import com.urremote.messaging.dao.QueueMessageDao;
import com.urremote.messaging.model.QueueMessage;

@SuppressWarnings("serial")
public class PhoneMessagingQueueServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(PhoneMessagingQueueServlet.class.getName());
	
	private static final DateFormat dateFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
	
	
	@SuppressWarnings("unchecked")
	private static final Pair<String, Map<String,String>>[] queues = 
			new Pair[] {
		Pair.create("umranium@gmail.com", new HashMap<String,String>() {
			{
				this.put("fname", "umran");
				this.put("lname", "abdulla");
			}
		}),
	};
	
	private static final long CREATE_TIME = System.currentTimeMillis(); 
	
	@Override
	public void init() throws ServletException {
		super.init();
		
		//QueueMessageDao.clearAllMessages();
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			
			for (Pair<String, Map<String,String>> pair:queues) {
				String deviceName = pair.getFirst();
				
				ArrayList<QueueMessage> tobesaved = new ArrayList<QueueMessage>();
				
				Map<String,String> messages = pair.getSecond();
				for (Map.Entry<String, String> message:messages.entrySet()) {
					long time = System.currentTimeMillis();
					log.info("added "+message.getKey()+"="+message.getValue()+", at "+time);
					tobesaved.add(new QueueMessage(
							deviceName, 
							time,
							message.getKey(),
							message.getValue()+" "+dateFormat.format(new Date(time))));
				}
				
				pm.makePersistentAll(tobesaved);
			}
			
		} finally {
			pm.flush();
			pm.close();
		}
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		String account = "umranium@gmail.com";//req.getParameter(Constants.C2DM_MSG_PARAM_ACCOUNT);
		String lastMessageTimeStr = Long.toString(CREATE_TIME);//req.getParameter(Constants.C2DM_MSG_PARAM_LAST_UPDATE);
		
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
			Map<String,String> messages = QueueMessageDao.getMessages(account, lastMessageTime);
			
			resp.setContentType("text/plain");
			PrintWriter out = resp.getWriter();
			
			out.println(account+":"+messages);
//			out.println(queue.getDeviceId()+":"+queue.getMessages());
			
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
