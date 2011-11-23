package com.urremote.invoker.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.urremote.invoker.client.DeviceService;
import com.urremote.invoker.server.c2dm.MessageUtil;
import com.urremote.invoker.server.common.Constants;
import com.urremote.invoker.server.model.ConnectedChannel;
import com.urremote.invoker.server.model.PhoneDetails;
import com.urremote.invoker.shared.DeviceChannelMessage;
import com.urremote.invoker.shared.InvalidDeviceStateException;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class DeviceServiceImpl extends RemoteServiceServlet implements
		DeviceService {

	private static final Logger log = Logger.getLogger(DeviceServiceImpl.class.getName());
	
	@Override
	public void setChannelDeviceFilter(String channelToken, String device) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			ConnectedChannel channel = pm.getObjectById(ConnectedChannel.class, channelToken);
			channel.setFilterDevice(device);
			log.info("Channel '"+channelToken+"' set device filter to '"+device+"'");
		} catch (javax.jdo.JDOObjectNotFoundException e) {
			// channel object not found... probably disconnected
		} finally {
			pm.close();
		}
	}
	
	@Override
	public List<String> getDeviceList() throws Exception {
		List<String> deviceList = new ArrayList<String>();
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery(PhoneDetails.class);
			
			@SuppressWarnings("unchecked")
			Collection<PhoneDetails> phoneDetails = (Collection<PhoneDetails>)q.execute();
			
			for (PhoneDetails details:phoneDetails) {
				deviceList.add(details.getAccount());
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while retrieving a list of all devices", e);
			throw e;
		} finally {
			pm.close();
		}
		return deviceList;
	}

	@Override
	public void deleteDevice(String device) throws InvalidDeviceStateException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			PhoneDetails phoneDetails = pm.getObjectById(PhoneDetails.class, device);
			pm.deletePersistent(phoneDetails);
			
			log.info("Device '"+device+"' deleted");
			
			ChannelMessagingUtil.broadcastMessage(
					DeviceChannelMessage.encode(
							Collections.singletonList(
									DeviceChannelMessage.createDeviceDeletedMsg(device))
					));
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while attempting to delete device '"+device+"'", e);
			throw new InvalidDeviceStateException(e.getMessage(), e);
		} finally {
			pm.close();
		}
	}
	
	@Override
	public Long getDeviceLastUpdate(String device) throws InvalidDeviceStateException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			PhoneDetails phoneDetails = pm.getObjectById(PhoneDetails.class, device);
			return phoneDetails.getLastPhoneUpdate();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while retrieving the last update time of the device '"+device+"'", e);
			throw new InvalidDeviceStateException(e.getMessage(), e);
		} finally {
			pm.close();
		}
	}
	
	@Override
	public boolean isDeviceRecordingState(String device) throws InvalidDeviceStateException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			PhoneDetails phoneDetails = pm.getObjectById(PhoneDetails.class, device);
			return phoneDetails.getIsRecording();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while retrieving the recording state of the device  '"+device+"'", e);			
			throw new InvalidDeviceStateException(e.getMessage(), e);
		} finally {
			pm.close();
		}
	}
	
	@Override
	public void startDeviceRecording(String device) throws InvalidDeviceStateException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			PhoneDetails phoneDetails = pm.getObjectById(PhoneDetails.class, device);

			Map<String,String> data = new HashMap<String, String>();
			data.put(Constants.C2DM_MSG_PARAM_TYPE, Constants.C2DM_MSG_TYPE_START_RECORDING);
			
			MessageUtil.sendMessage(
					phoneDetails.getAccount(),
					phoneDetails.getDeviceId(),
					Constants.C2DM_RECORDING_COLLAPSE_KEY,
					data);
			
			log.info("start recording request sent to device '"+device+"'");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while attempting to send a start recording request to device '"+device+"'", e);			
			throw new InvalidDeviceStateException("Error while sending start device message", e);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while attempting to send a start recording request to device '"+device+"'", e);			
			throw new InvalidDeviceStateException(e.getMessage(), e);
		} finally {
			pm.close();
		}
	}
	
	@Override
	public void stopDeviceRecording(String device) throws InvalidDeviceStateException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			PhoneDetails phoneDetails = pm.getObjectById(PhoneDetails.class, device);

			Map<String,String> data = new HashMap<String, String>();
			data.put(Constants.C2DM_MSG_PARAM_TYPE, Constants.C2DM_MSG_TYPE_STOP_RECORDING);
			
			MessageUtil.sendMessage(
					phoneDetails.getAccount(),
					phoneDetails.getDeviceId(),
					Constants.C2DM_RECORDING_COLLAPSE_KEY,
					data);
			
			log.info("stop recording request sent to device '"+device+"'");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while attempting to send a stop recording request to device '"+device+"'", e);			
			throw new InvalidDeviceStateException("Error while sending stop device message", e);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while attempting to send a stop recording request to device '"+device+"'", e);			
			throw new InvalidDeviceStateException(e.getMessage(), e);
		} finally {
			pm.close();
		}
	}
	
}
