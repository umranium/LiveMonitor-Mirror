package au.csiro.antplus.scanner;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntInterface;
import com.dsi.ant.AntInterfaceIntent;
import com.dsi.ant.AntMesg;
import com.dsi.ant.exception.AntInterfaceException;
import com.dsi.ant.exception.AntServiceNotConnectedException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

public class AntChannelManager {
	
	private static final String ANT_MESG_FIELD_PAT = "MESG_.*_ID";
	private static final String ANT_EVENT_FIELD_PAT = ".*";

	private Activity activity;
	private AntInterface antReceiver;
	private IntentFilter statusIntentFilter;
	private IntentFilter messageIntentFilter;
	private boolean serviceIsConnected = false;
	private Map<Byte,AntChannel> antChannels;
	private boolean antInterfaceClaimed = false;
	private boolean sentResetRequest = false;
	private AntManagerEventsCallback eventsCallback = null;
	
	/** Data buffered for event buffering before flush. */
	private short eventBufferThreshold;

	public AntChannelManager(Activity context) {
		this.activity = context;
		
		statusIntentFilter = new IntentFilter();
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_ENABLED_ACTION);
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_DISABLED_ACTION);
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_RESET_ACTION);
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION);
        
        messageIntentFilter = new IntentFilter();
        messageIntentFilter.addAction(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION);
        
        antChannels = new HashMap<Byte, AntChannel>(256);
	}
	
	public AntInterface getAntReceiver() {
		return antReceiver;
	}
	
	public AntManagerEventsCallback getEventsCallback() {
		return eventsCallback;
	}
	
	public void setEventsCallback(AntManagerEventsCallback eventsCallback) {
		this.eventsCallback = eventsCallback;
	}
	
	public void connect()
	{
        if(AntInterface.hasAntSupport(activity)) {
        	antReceiver = AntInterface.getInstance(activity, antServiceListener);

            if(null == antReceiver) {
                // There is no point running this application without ANT support.
            	Toast.makeText(activity, "Ant+ Service Not Installed", Toast.LENGTH_LONG).show();
            }
        }
        else {
        	Toast.makeText(activity, "No Ant+ Support Found", Toast.LENGTH_LONG).show();
        }
        
        if (antReceiver.isServiceConnected()) {
        	serviceConnected();
        }
        
	}
	
	public void shutdown()
	{
		enableReceiveAntMesgs(false);
		
        try {
            activity.unregisterReceiver(antStatusReceiver);
        } catch(IllegalArgumentException e) {
            // Receiver wasn't registered, ignore as that's what we wanted anyway
        }
		
        try
        {
           if(activity.isFinishing()) {
              Log.d(Constants.TAG, "AntChannelManager.shutDown: isFinishing");
            
              if(antReceiver.isServiceConnected()) {
                 if(antReceiver.hasClaimedInterface()) {
                    Log.d(Constants.TAG, "AntChannelManager.shutDown: Releasing interface");
                 
                    antReceiver.releaseInterface();
                 }
                 
                 antReceiver.stopRequestForceClaimInterface();
                 
                 antReceiver.destroy();
              }
           }
        } catch(AntServiceNotConnectedException e) {
            // Ignore as we are disconnecting the service/closing the app anyway
        } catch(AntInterfaceException e) {
           Log.w(Constants.TAG, "Exception in AntChannelManager.shutDown", e);
        }
	}
	
	public void resetAnt()
	{
        try {
            antReceiver.ANTResetSystem();
            setAntConfiguration();
        	sentResetRequest = true;
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error while reseting ANT+", e);
        }
	}
	
    /**
     * Attempts to claim the Ant interface
     */
    public void tryClaimAnt()
    {
        try {
            antReceiver.requestForceClaimInterface(activity.getResources().getString(R.string.app_name));
        } catch(Exception e) {
            Log.e(Constants.TAG, "Error while trying to claim ANT+ interface", e);
        }
    }
    
	private void serviceConnected()
	{
		if (serviceIsConnected || !antReceiver.isServiceConnected()) return;
		
        try {
    		if (!antReceiver.claimInterface()) {
    			Toast.makeText(activity, "Unable to claim ANT+ interface", Toast.LENGTH_LONG).show();
    			return;
    		}
    		
            antInterfaceClaimed = antReceiver.hasClaimedInterface();
            
            activity.registerReceiver(antStatusReceiver, statusIntentFilter);
            
            enableReceiveAntMesgs(true);
            
            if (!antReceiver.isEnabled()) {
            	antReceiver.enable();
            }
            
            serviceIsConnected = true;
            
            if (eventsCallback!=null)
            	eventsCallback.onServiceConnected();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error While Initializing Service Connection", e);
        }
	}
	
	private void serviceDisconnected()
	{
		if (serviceIsConnected) {
	        if (eventsCallback!=null)
	        	eventsCallback.onServiceDisconneted();
			serviceIsConnected = false;
		}
	}
	
	private void enableReceiveAntMesgs(boolean enable)
	{
		if (enable) {
            activity.registerReceiver(antMessageReceiver, messageIntentFilter);
		} else {
			try {
				activity.unregisterReceiver(antMessageReceiver);
			} catch (Exception e) {
				//	receiver not registered, ignore
			}
		}
	}
	
	private void onInterfaceClaimed()
	{
 	   boolean wasClaimed = antInterfaceClaimed;
	   
	   try {
		   antInterfaceClaimed = antReceiver.hasClaimedInterface();
		   
		   if (antInterfaceClaimed!=wasClaimed) {
			   if (antInterfaceClaimed) {
				   enableReceiveAntMesgs(true);
			   } else {
				   enableReceiveAntMesgs(false);
				   String appName = activity.getResources().getString(R.string.app_name);
				   Toast.makeText(activity, appName+" Lost Ant+ Interface", Toast.LENGTH_LONG).show();
			   }
		   }
	   } catch (Exception e) {
		   Log.e(Constants.TAG, "Error after receiving interface claim event", e);
	   }
	}
	
    /**
     * Configure the ANT radio to the user settings.
     */
	private void setAntConfiguration()
	{
		if (antReceiver.isServiceConnected()) {
			try {
				if (eventBufferThreshold>0) {
                    //TODO For easy demonstration will set screen on and screen off thresholds to the same value.
                    // No buffering by interval here.
                    antReceiver.ANTConfigEventBuffering((short)0xFFFF, eventBufferThreshold, (short)0xFFFF, eventBufferThreshold);
				} else {
					antReceiver.ANTDisableEventBuffering();
				}
			} catch (Exception e) {
				Log.e(Constants.TAG, "Error while trying to set ant configuration settings", e);
			}
		}
	}

    /**
     * Class for receiving notifications about ANT service state.
     */
    private AntInterface.ServiceListener antServiceListener = new AntInterface.ServiceListener()
    {
        public void onServiceConnected()
        {
            Log.d(Constants.TAG, "antServiceListener:onServiceConnected()");
            serviceConnected();
        }

        public void onServiceDisconnected()
        {
            Log.d(Constants.TAG, "antServiceListener:onServiceDisconnected()");
            serviceDisconnected();
        }
    };
    
    /** Receives all of the ANT status intents. */
    private final BroadcastReceiver antStatusReceiver = new BroadcastReceiver() 
    {      
       public void onReceive(Context context, Intent intent) 
       {
           String action = intent.getAction();
           Log.d(Constants.TAG, "antStatusReceiver:onReceive: " + action);
       
           if (AntInterfaceIntent.ANT_ENABLED_ACTION.equals(action)) {
        	   
           } else
           if (AntInterfaceIntent.ANT_DISABLED_ACTION.equals(action)) {
        	   for (AntChannel d:antChannels.values())
        		   d.setChannelState(ChannelState.CLOSED);
           } else
           if (AntInterfaceIntent.ANT_RESET_ACTION.equals(action)) {
        	   if (sentResetRequest) {
        		   setAntConfiguration();
            	   for (AntChannel d:antChannels.values())
            		   d.setChannelState(ChannelState.CLOSED);
        		   sentResetRequest = false;
        	   } else {
            	   for (AntChannel d:antChannels.values())
            		   d.setChannelState(ChannelState.CLOSED);
        		   eventBufferThreshold = 0;
        	   }
           } else
           if (AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION.equals(action)) {
        	   onInterfaceClaimed();
           } else
           {
        	   Log.w(Constants.TAG, "Unsupported Action Received: "+action);
           }
       }
    };
    
    /** Receives all of the ANT message intents and dispatches to the proper handler. */
    private final BroadcastReceiver antMessageReceiver = new BroadcastReceiver() 
    {      
       public void onReceive(Context context, Intent intent) 
       {
           String action = intent.getAction();
//           Log.d(Constants.TAG, "antMessageReceiver:onReceive: " + action);
           
           if (AntInterfaceIntent.ANT_RX_MESSAGE_ACTION.equals(action)) {
        	   byte[] mesg = intent.getByteArrayExtra(AntInterfaceIntent.ANT_MESSAGE);
        	   
//        	   Log.d(Constants.TAG, "antMessageReceiver: Received " +
//        			   Constants.getHexString(mesg) + " : " + 
//        			   Constants.findFinalStaticFieldName(
//        					   AntMesg.class,
//        					   ANT_MESG_FIELD_PAT,
//        					   Byte.TYPE,
//        					   new Byte(mesg[AntMesg.MESG_ID_OFFSET])
//        			   ));
        	   
               switch(mesg[AntMesg.MESG_ID_OFFSET])
               {
                   case AntMesg.MESG_STARTUP_MESG_ID:
                       break;
                   case AntMesg.MESG_BROADCAST_DATA_ID:
                   case AntMesg.MESG_ACKNOWLEDGED_DATA_ID:
                   {
                       byte channelNum = mesg[AntMesg.MESG_DATA_OFFSET];
                       if (antChannels.containsKey(channelNum)) {
                    	   AntChannel channel = antChannels.get(channelNum);
                    	   
                    	   if (channel.getDeviceId()==Constants.WILDCARD) {
                               try {
                                   Log.d(Constants.TAG, "Requesting device number");
                                   antReceiver.ANTRequestMessage(channelNum, AntMesg.MESG_CHANNEL_ID_ID);
                               } catch(AntInterfaceException e) {
                                   Log.e(Constants.TAG, "Error while requesting device details", e);
                               }
                    	   }
                    	   
                    	   antChannels.get(channelNum).decodeMsg(mesg);
                       } else {
                    	   Log.w(Constants.TAG, "Received message for unused channel:"+channelNum);
                       }
                       break;
                   }
                   case AntMesg.MESG_BURST_DATA_ID:
                       break;
                   case AntMesg.MESG_RESPONSE_EVENT_ID:
                       responseEventHandler(mesg);
                       break;
                   case AntMesg.MESG_CHANNEL_STATUS_ID:
                       break;
                   case AntMesg.MESG_CHANNEL_ID_ID:
                   {
                       short deviceId = (short)(
                    		   (mesg[AntMesg.MESG_DATA_OFFSET + 1]&0xFF | 
            				   ((mesg[AntMesg.MESG_DATA_OFFSET + 2]&0xFF) << 8)
            				   ) & 0xFFFF);
                       byte channelNum = mesg[AntMesg.MESG_DATA_OFFSET];
                       if (antChannels.containsKey(channelNum)) {
                    	   antChannels.get(channelNum).setDeviceId(deviceId);
                       } else {
                    	   Log.w(Constants.TAG, "Received ID for unused channel:"+channelNum+", device ID="+deviceId);
                       }
                       break;
                   }
                   case AntMesg.MESG_VERSION_ID:
                       break;
                   case AntMesg.MESG_CAPABILITIES_ID:
                       break;
                   case AntMesg.MESG_GET_SERIAL_NUM_ID:
                       break;
                   case AntMesg.MESG_EXT_ACKNOWLEDGED_DATA_ID:
                       break;
                   case AntMesg.MESG_EXT_BROADCAST_DATA_ID:
                       break;
                   case AntMesg.MESG_EXT_BURST_DATA_ID:
                       break;
               }
           }
       }
    };
    
    /**
     * Handles response and channel event messages
     * @param mesg
     */
    private void responseEventHandler(byte[] mesg)
    {
       byte channelNum = mesg[AntMesg.MESG_DATA_OFFSET];
       byte responseMsgId = mesg[AntMesg.MESG_DATA_OFFSET+1];
       byte responseCode = mesg[AntMesg.MESG_DATA_OFFSET+2];
       
 	   //	check that we're responding to events,
 	   //		not message ACKs
 	   if (responseMsgId!=1) {
// 	 	   Log.d(Constants.TAG, "responseEventHandler: Received ACK For Message ID " + 
// 				   Constants.findFinalStaticFieldName(
// 						   AntMesg.class,
// 						   ANT_MESG_FIELD_PAT,
// 						   Byte.TYPE,
// 						   new Byte(responseMsgId)
// 				   ));
 		   return;
 	   }
       
// 	   Log.d(Constants.TAG, "responseEventHandler: Received Message Code " + 
//			   Constants.findFinalStaticFieldName(
//					   AntDefine.class,
//					   ANT_EVENT_FIELD_PAT,
//					   Byte.TYPE,
//					   new Byte(responseCode)
//			   ));
 	   
        // For a list of possible message codes
        // see ANT Message Protocol and Usage section 9.5.6.1
        // available from thisisant.com
        switch(responseCode) //Switch on message code
        {
            case AntDefine.EVENT_RX_SEARCH_TIMEOUT:
            {
            	try {
    				if (antChannels.containsKey(channelNum)) {
    					AntChannel channel = antChannels.get(channelNum);
    					antChannels.remove(channelNum);
    					antReceiver.ANTUnassignChannel(channelNum);
    					channel.onSearchTimeOut();
    				} else {
    					Log.w(Constants.TAG, "Received message for unused channel:"
    							+ channelNum);
    				}
            	} catch (Exception e) {
            		Log.e(Constants.TAG, "Error after receiving search timeout", e);
            	}
				break;
            }
        }
    }
    
    public Byte assignChannelId(AntChannel antChannel)
    {
		int channelId = -1;
		for (int i=0; i<=255; ++i) {
			if (!antChannels.containsKey((byte)i)) {
				channelId = i;
				break;
			}
		}
		
		//	no empty channel
		if (channelId<0)
			return null;
    	
		Byte id = new Byte((byte)channelId);
		antChannels.put(id, antChannel);
		
		return id;
    }
    
    public void unassignChannelId(Byte id)
    {
    	antChannels.remove(id);
    }
    
//    public <E extends AntChannel> E createChannel(Class<E> cl) {
//    	try {
//    		int channelId = -1;
//    		for (int i=0; i<=255; ++i) {
//    			if (!antChannels.containsKey((byte)i)) {
//    				channelId = i;
//    				break;
//    			}
//    		}
//    		
//    		//	no empty channel
//    		if (channelId<0)
//    			return null;
//    		
//			Constructor<E> constructor = cl.getConstructor(
//					AntChannelManager.class,
//					Byte.TYPE
//					);
//			
//			E instance = constructor.newInstance(this, new Byte((byte)channelId));
//			
//			antChannels.put((byte)channelId, instance);
//			Log.i(Constants.TAG, "Channel assigned: "+channelId);
//			
//			return instance;
//		} catch (Exception e) {
//			Log.e(Constants.TAG, "Error while creating channel", e);
//			return null;
//		}
//    }
}
