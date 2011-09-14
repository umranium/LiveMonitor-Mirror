package au.csiro.antplus.scanner;

import android.util.Log;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntInterface;

public class AntChannel {
	
	protected AntChannelManager antChannelManager;
	protected int networkNumber;
	protected Byte channelId;
	protected int deviceId;
	protected int deviceType;
	protected int period;
	protected int frequency;
	protected ChannelState channelState;
	
	public AntChannel(AntChannelManager antChannelManager)
	{
		this.antChannelManager = antChannelManager;
		this.networkNumber = 1;
		this.channelId = null;
		this.deviceId = 0;
		this.deviceType = 0;
		this.period = 0;
		this.frequency = 57;
		this.channelState = ChannelState.CLOSED;
	}
	
	public void search(byte searchTimeout, byte lowPrioritySearchTimeout) {
		this.channelId = antChannelManager.assignChannelId(this);
		if (antChannelSetup(
				(byte)networkNumber,	//	ANT+
				(byte)0,	//	device-id=wild-card
				(byte)deviceType,
				(byte)0,	// transmission-type=wild-card search
				(short)period,
				(byte)frequency,	//	ANT+
				Constants.DEFAULT_BIN,	//	Default search threshold
				searchTimeout,
				lowPrioritySearchTimeout
				))
		{
			channelState = ChannelState.SEARCHING;
		} else {
			antChannelManager.unassignChannelId(this.channelId);
			channelState = ChannelState.CLOSED;
		}
	}
	
	public void close() {
		if (!ChannelState.CLOSED.equals(channelState)) {
			try {
				AntInterface antReceiver = antChannelManager.getAntReceiver();
				antReceiver.ANTCloseChannel((byte) channelId);
			} catch (Exception e) {
				Log.e(Constants.TAG, "Error while trying to close channel", e);
			}
		}
		if (this.channelId!=null) {
			antChannelManager.unassignChannelId(channelId);
		}
		channelState = ChannelState.CLOSED;
	}
	
	public void decodeMsg(byte[] msg) {
		channelState = ChannelState.CONNECTED;
	}
	
	public void onSearchTimeOut() {
		channelId = null;
	}

	public byte getChannelId() {
		return channelId;
	}
	
	public int getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(short deviceId) {
		this.deviceId = deviceId;
	}

	public int getDeviceType() {
		return deviceType;
	}

	public int getPeriod() {
		return period;
	}

	public ChannelState getChannelState() {
		return channelState;
	}

	public void setChannelState(ChannelState channelState) {
		this.channelState = channelState;
	}

    /**
     * ANT Channel Configuration.
     *
     * @param networkNumber the network number
     * @param channelId the channel number
     * @param deviceNumber the device number
     * @param deviceType the device type
     * @param txType the tx type
     * @param channelPeriod the channel period
     * @param radioFreq the radio freq
     * @param proxSearch the prox search
     * @return true, if successfully configured and opened channel
     */   
    protected final boolean antChannelSetup(
    		byte networkNumber,
    		short deviceNumber,
    		byte deviceType,
    		byte txType,
    		short channelPeriod,
    		byte radioFreq,
    		byte proxSearch,
    		byte scanTimeout,
    		byte lowPriorityScanTimeout)
    {
       boolean channelOpen = false;

       try {
    	   AntInterface antReceiver = antChannelManager.getAntReceiver();
    	   
    	   antReceiver.ANTAssignChannel((byte)channelId, AntDefine.PARAMETER_RX_NOT_TX, networkNumber);  // Assign as slave channel on selected network (0 = public, 1 = ANT+, 2 = ANTFS)
    	   antReceiver.ANTSetChannelId((byte)channelId, deviceNumber, deviceType, txType);
    	   antReceiver.ANTSetChannelPeriod((byte)channelId, channelPeriod);
    	   antReceiver.ANTSetChannelRFFreq((byte)channelId, radioFreq);
    	   antReceiver.ANTSetChannelSearchTimeout((byte)channelId, (byte)0); // Disable high priority search
           
    	   antReceiver.ANTSetChannelSearchTimeout((byte)channelId, scanTimeout);	// 5 seconds
    	   antReceiver.ANTSetLowPriorityChannelSearchTimeout((byte)channelId, lowPriorityScanTimeout);
    	   
           if(deviceNumber == Constants.WILDCARD) {
        	   antReceiver.ANTSetProximitySearch((byte)channelId, proxSearch);   // Configure proximity search, if using wild card search
           }
           
           antReceiver.ANTOpenChannel((byte)channelId);
           
           channelOpen = true;
       } catch(Exception e) {
           Log.e(Constants.TAG, "Error while trying to open channel", e);
       }
      
       return channelOpen;
    }
	
}
