package com.urremote.messaging.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.urremote.messaging.common.CaseInsensitiveStringComparator;
import com.urremote.messaging.model.QueueMessage;

public class QueueMessageDao {

	private static final Logger log = Logger.getLogger(QueueMessageDao.class.getName());
	
	public static void clearAllMessages() {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery(QueueMessage.class);
			q.deletePersistentAll();
		} finally {
			pm.flush();
			pm.close();
		}
	}
	
	public static void deleteMessagesBefore(String device, long lastUpdate) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery("SELECT FROM "+QueueMessage.class.getCanonicalName()
					+ " WHERE deviceId==:device && timeStamp<:lastUpdate "
					+ " ORDER BY timeStamp "
					);
			HashMap<String,Object> params = new HashMap<String,Object>();
			params.put("device", device);
			params.put("lastUpdate", lastUpdate);
			
			q.deletePersistentAll(params);
			
		} finally {
			pm.flush();
			pm.close();
		}
	}
	
	public static Map<String,String> getMessages(String device, long lastUpdate) {
		Map<String,String> messages = new TreeMap<String,String>(CaseInsensitiveStringComparator.INSTANCE);

		log.info("device="+device+", since="+lastUpdate+":");
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery("SELECT FROM "+QueueMessage.class.getCanonicalName()
					+ " WHERE deviceId==:device && timeStamp>=:lastUpdate "
					+ " ORDER BY timeStamp "
					);
			HashMap<String,Object> params = new HashMap<String,Object>();
			params.put("device", device);
			params.put("lastUpdate", lastUpdate);
			List<QueueMessage> msgs = (List<QueueMessage>)q.executeWithMap(params);
			
			for (QueueMessage msg:msgs) {
				log.info("\tmsg={"+msg.getKey()+", "+msg.getTimeStamp()+", "+msg.getValue()+"}");
				messages.put(msg.getKey(), msg.getValue());
			}
			
		} finally {
			pm.close();
		}
		
		return messages;
	}

}
