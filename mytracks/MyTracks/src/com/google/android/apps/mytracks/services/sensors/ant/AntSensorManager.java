/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.apps.mytracks.services.sensors.ant;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntInterface;
import com.dsi.ant.AntInterfaceIntent;
import com.dsi.ant.AntMesg;
import com.dsi.ant.exception.AntInterfaceException;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.services.sensors.SensorManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

/**
 * This is the common superclass for ANT-based sensors.  It handles tasks which
 * apply to the ANT framework as a whole, such as framework initialization and
 * destruction.  Subclasses are expected to handle the initialization and
 * management of individual sensors.
 *
 * Implementation:
 *
 * The initialization process is somewhat complicated.  This is due in part to
 * the asynchronous nature of ANT Radio Service initialization, and in part due
 * to an apparent bug in that service.  The following is an overview of the
 * initialization process.
 *
 * Initialization begins in {@link #setupChannel}, which is invoked by the
 * {@link SensorManager} when track recording begins.  {@link #setupChannel}
 * asks the ANT Radio Service (via {@link AntInterface}) to start, using a
 * {@link AntInterface.ServiceListener} to indicate when the service has
 * connected.  {@link #serviceConnected} claims and enables the Radio Service,
 * and then resets it to a known state for our use.  Completion of reset is
 * indicated by receipt of a startup message (see {@link AntStartupMessage}).
 * Once we've received that message, the ANT service is ready for use, and we
 * can start sensor-specific initialization using
 * {@link #setupAntSensorChannels}.  The initialization of each sensor will
 * usually result in a call to {@link #setupAntSensorChannel}.
 *
 * @author Sandor Dornbush
 */
public abstract class AntSensorManager extends SensorManager {

  // Pairing
  protected static final short WILDCARD = 0;
  
  private static final long DURATION_WAIT_FOR_CONNECTION = 5000L;   // 5s 

  private AntInterface antReceiver;

  // Flag to know if the ANT App was interrupted
  // TODO: This code path is not used but probably should be.
  private boolean antInterrupted;

  /**
   * The data from the sensors.
   */
  protected SensorDataSet sensorData;

  protected Context context;
  
  private boolean antConnected = false;
  private long timeStartedWaitingForConnection = -1;
  private boolean antServiceInitialized = false;
  private boolean pendingAntReset = false;

  private static final boolean DEBUGGING = false;

  /** Receives and logs all status ANT intents. */
  private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context ctx, Intent intent) {
      String antAction = intent.getAction();
      Log.i(TAG, "enter status onReceive" + antAction);
    }
  };

  /** Receives all data ANT intents. */
  private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context ctx, Intent intent) {
      String antAction = intent.getAction();
      Log.i(TAG, "enter data onReceive" + antAction);

      if (antAction.equals(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION)) {
        byte[] antMessage = intent.getByteArrayExtra(AntInterfaceIntent.ANT_MESSAGE);
        if (DEBUGGING) {
          Log.d(TAG, "Received RX message " + messageToString(antMessage));
        }

        handleMessage(antMessage);
      }
    }
  };

  /**
   * ANT uses this listener to tell us when it has bound to the ANT Radio
   * Service.  We can't start sending ANT commands until we've been notified
   * (via this listener) that the Radio Service has connected.
   */
  private AntInterface.ServiceListener antServiceListener = new AntInterface.ServiceListener() {
    @Override
    public void onServiceConnected() {
      try {
        serviceConnected();
      } catch (AntException e) {
        Log.e(Constants.TAG, "Error setting up ANT after service connection", e);
        Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
      }
    }

    @Override
    public void onServiceDisconnected() {
      Log.d(TAG, "ANT interface reports disconnection");
      antConnected = false;
      antServiceInitialized = false;
    }
  };

  public AntSensorManager(Context context) {
    this.context = context;

    // We handle this unpleasantly because the UI should've checked for ANT
    // support before it even instantiated this class.
    if (!AntInterface.hasAntSupport(context)) {
      throw new IllegalStateException("device does not have ANT support");
    }

    // We register for ANT intents early because we want to have a record of
    // the status intents in the log as we start up.
    registerForAntIntents();
    
    try {
      initAntInterface();
    } catch (AntException e) {
      Log.e(Constants.TAG, "Error initializing ANT interface", e);
      Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onDestroy() {
    cleanAntInterface();
    
    try {
      context.unregisterReceiver(statusReceiver);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Failed to unregister ANT status receiver", e);
    }

    try {
      context.unregisterReceiver(dataReceiver);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Failed to unregister ANT data receiver", e);
    }

  }

  @Override
  public SensorDataSet getSensorDataSet() {
    return sensorData;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public AntInterface getAntReceiver() {
    return antReceiver;
  }

  /**
   * This is the interface used by the {@link SensorManager} to tell this
   * class when to start.  It handles initialization of the ANT framework,
   * eventually resulting in sensor-specific initialization via
   * {@link #setupAntSensorChannels}.
   */
  @Override
  protected final void setupChannel() {
    Log.i(Constants.TAG, "MyTracks request to set up ANT Channel");
    
    setSensorState(Sensor.SensorState.CONNECTING);
    
    if (antReceiver==null ||
        (!antConnected && System.currentTimeMillis()-timeStartedWaitingForConnection>DURATION_WAIT_FOR_CONNECTION)) {
      if (antReceiver==null) {
        Log.d(TAG, "no ant receiver interface found");
      } else {
        Log.d(TAG, "ant receiver interface found, but connection not established over an extended period");
      }
      
      cleanAntInterface();
      try {
        initAntInterface();
      } catch (AntException e) {
        Log.e(Constants.TAG, "Error resetting ANT interface", e);
        Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
      }
      pendingAntReset = true;
    } else {
      if (antReceiver!=null && antConnected) {
        Log.d(TAG, "ant receiver interface found, service connected, resetting channel");
        pendingAntReset = false;
        resetChannel();
      } else {
        pendingAntReset = true;
        Log.d(TAG, "ant receiver interface found, but waiting for connection");
      }
    }
    
  }
  
  /**
   * Initializes the ANT+ receiver interface by registering a
   * service listener.
   * 
   * If the the service is connected by the time the method returns
   * (which sometimes happens), the {@link #serviceConnected()} method is called. 
   * 
   * @throws AntException
   */
  private void initAntInterface() throws AntException
  {
    Log.i(TAG, "Initializing AntSensorManager");
    
    antReceiver = AntInterface.getInstance(context, antServiceListener);
    if (antReceiver == null) {
      throw new AntException("Failed to get ANT Receiver");
    }
    timeStartedWaitingForConnection = System.currentTimeMillis();
    
    Log.i(TAG, "got ANT Receiver");
    
    if (antConnected) {
      serviceConnected();
    }
    
  }
  
  /**
   * Cleans up the ANT+ receiver interface, by releasing the interface
   * and destroying it.
   */
  private void cleanAntInterface()
  {
    Log.i(TAG, "Destroying AntSensorManager");
    
    if (antReceiver!=null) {
      try {
        antReceiver.releaseInterface();
      } catch (AntInterfaceException e) {
        Log.e(TAG, "failed to release ANT interface", e);
      }
  
      antReceiver.destroy();
      
      antReceiver = null;
      antConnected = false;
    }
  }
  
  private void resetChannel() {
    try {
      Log.i(TAG, "Resetting ANT Channel");
      // We expect this call to throw an exception due to a bug in the ANT
      // Radio Service.  It won't actually fail, though, as we'll get the
      // startup message (see {@link AntStartupMessage}) one normally expects
      // after a reset.  Channel initialization can proceed once we receive
      // that message.
      antReceiver.ANTResetSystem();
    } catch (AntInterfaceException e) {
      Log.e(TAG, "failed to reset ANT (expected exception)", e);
    }
  }
  
  /**
   * This method is invoked via the ServiceListener when we're connected to
   * the ANT service.  If we're just starting up, this is our first opportunity
   * to initiate any ANT commands.
   * @throws AntException 
   */
  private synchronized void serviceConnected() throws AntException {
    Log.d(TAG, "ANT service connected");
    antConnected = true;
    
    //  sometimes, this method is invoked before the AntInterface.getInstance(..) returns
    //      since we need the ANT receiver interface, we return without doing anything
    //      the initAntInterface() function will then invoke this method to initialize
    if (antReceiver==null) {
      Log.w(TAG, "WARNING: ANT service connected, but AntReceiver interface not assigned!!!");
      return;
    }

    //  initialization after the service is connected, and the ANT receiver interface has been obtained
    if (!antServiceInitialized) {
      antServiceInitialized = true;
      Log.i(TAG, "Initializing Ant Service");
      
      try {
        if (!antReceiver.claimInterface()) {
          Log.e(TAG, "failed to claim ANT interface");
          return;
        }
  
        if (!antReceiver.isEnabled()) {
          // Make sure not to call AntInterface.enable() again, if it has been
          // already called before
          if (antInterrupted == false) {
            Log.i(TAG, "Powering on Radio");
            try {
              antReceiver.enable();
            } catch (Exception e) {
              Log.e(TAG, "Error while enabling ANT Receiver", e);
            }
          } else {
            Log.i(TAG, "Radio on, but interrupted");
          }
        } else {
          Log.i(TAG, "Radio already enabled");
        }
      } catch (AntInterfaceException e) {
        throw new AntException("Failed to enable ANT", e);
      }
    }

    if (pendingAntReset) {
      pendingAntReset = false;
      resetChannel();
    }
  }

  /**
   * Process a raw ANT message.
   * @param antMessage the ANT message, including the size and message ID bytes
   * @deprecated Use {@link #handleMessage(int, byte[])} instead.
   */
  protected void handleMessage(byte[] antMessage) {
    int len = antMessage[0];
    if (len != antMessage.length - 2 || antMessage.length <= 2) {
      Log.e(TAG, "Invalid message: " + messageToString(antMessage));
      return;
    }

    byte messageId = antMessage[1];
    // Arrays#copyOfRange doesn't exist??
    byte[] messageData = new byte[antMessage.length - 2];
    System.arraycopy(antMessage, 2, messageData, 0, antMessage.length - 2);
    handleMessage(messageId, messageData);
  }

  /**
   * Process a raw ANT message.
   * @param messageId the message ID.  See the ANT Message Protocol and Usage
   *     guide, section 9.3.
   * @param messageData the ANT message, without the size and message ID bytes.
   * @return true if this method has taken responsibility for the passed
   *     message; false otherwise.
   */
  protected boolean handleMessage(byte messageId, byte[] messageData) {
    if (messageId == AntMesg.MESG_STARTUP_MESG_ID) {
      Log.d(TAG, String.format(
          "Received startup message (reason %02x); initializing channel",
          new AntStartupMessage(messageData).getMessage()));
      setupAntSensorChannels();
      return true;
    }

    return false;
  }

  /**
   * Subclasses define this method to perform sensor-specific initialization.
   * When this method is called, the ANT framework has been enabled, and is
   * ready for use.
   */
  protected abstract void setupAntSensorChannels();

  /**
   * Used by subclasses to set up an ANT channel for a single sensor.  A given
   * subclass may invoke this method multiple times if the subclass is
   * responsible for more than one sensor.
   *
   * @return true on success
   */
  protected boolean setupAntSensorChannel(byte networkNumber, byte channelNumber,
      short deviceNumber, byte deviceType, byte txType, short channelPeriod,
      byte radioFreq, byte proxSearch) {

    try {
      // Assign as slave channel on selected network (0 = public, 1 = ANT+, 2 =
      // ANTFS)
      antReceiver.ANTAssignChannel(channelNumber, AntDefine.PARAMETER_RX_NOT_TX, networkNumber);

      antReceiver.ANTSetChannelId(channelNumber, deviceNumber, deviceType, txType);
      antReceiver.ANTSetChannelPeriod(channelNumber, channelPeriod);
      antReceiver.ANTSetChannelRFFreq(channelNumber, radioFreq);

      // Disable high priority search
      antReceiver.ANTSetChannelSearchTimeout(channelNumber, (byte) 0);

      // Set search timeout to 30 seconds (low priority search))
      antReceiver.ANTSetLowPriorityChannelSearchTimeout(channelNumber, (byte) 12);

      if (deviceNumber == WILDCARD) {
        // Configure proximity search, if using wild card search
        antReceiver.ANTSetProximitySearch(channelNumber, proxSearch);
      }

      antReceiver.ANTOpenChannel(channelNumber);
      return true;

    } catch (AntInterfaceException e) {
      Log.e(TAG, "failed to setup ANT channel", e);
      return false;
    }
  }

  private void registerForAntIntents() {
    Log.i(TAG, "Registering for ant intents.");
    // Register for ANT intent broadcasts.
    IntentFilter statusIntentFilter = new IntentFilter();
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_ENABLED_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_DISABLED_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_RESET_ACTION);
    context.registerReceiver(statusReceiver, statusIntentFilter);

    IntentFilter dataIntentFilter = new IntentFilter();
    dataIntentFilter.addAction(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION);
    context.registerReceiver(dataReceiver, dataIntentFilter);
  }

  private String messageToString(byte[] message) {
    StringBuilder out = new StringBuilder();
    for (byte b : message) {
      out.append(String.format("%s%02x", (out.length() == 0 ? "" : " "), b));
    }
    return out.toString();
  }
}
