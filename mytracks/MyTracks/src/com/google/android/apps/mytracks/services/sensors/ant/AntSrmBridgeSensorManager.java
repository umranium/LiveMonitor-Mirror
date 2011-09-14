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
import com.dsi.ant.AntMesg;
import com.dsi.ant.exception.AntInterfaceException;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * A sensor manager to the PC7 SRM ANT+ bridge.
 * 
 *  Original code from Sandor Dornbush
 *  Modified by Umran Abdulla.
 *  
 */
public class AntSrmBridgeSensorManager extends AntSensorManager {

  /*
   * These constants are defined by the ANT+ spec.
   */
  public static final byte CHANNEL_NUMBER = 0;
  public static final byte NETWORK_NUMBER = 0;
  public static final byte DEVICE_TYPE = 12;
  public static final byte NETWORK_ID = 1;
  public static final short CHANNEL_PERIOD = 8192;
  public static final byte RF_FREQUENCY = 50;

  private static final int INDEX_MESSAGE_TYPE     = 1;
  private static final int INDEX_MESSAGE_ID       = 2;
  private static final int INDEX_MESSAGE_POWER    = 3;
  private static final int INDEX_MESSAGE_SPEED    = 5;
  private static final int INDEX_MESSAGE_CADENCE  = 7;
  private static final int INDEX_MESSAGE_BPM      = 8;
  
  private static final int MSG_INITIAL    = 5;
  private static final int MSG_DATA       = 6;
  
  private short deviceNumber;

  public AntSrmBridgeSensorManager(Context context) {
    super(context);
    
    Log.i(Constants.TAG, "new ANT SRM Bridge Sensor Manager created");

    deviceNumber = WILDCARD;

    // First read the the device id that we will be pairing with.
    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, 0);
    if (prefs != null) {
      deviceNumber =
        (short) prefs.getInt(context.getString(R.string.ant_srm_bridge_sensor_id_key), 0);
    }
    Log.i(TAG, "Will pair with device: " + deviceNumber);
  }
  
  
  @Override
  protected boolean handleMessage(byte messageId, byte[] messageData) {
    if (super.handleMessage(messageId, messageData)) {
      return true;
    }
    
    Log.i(Constants.TAG, "Received ANT msg: "+antMesgToStr(messageId)+"("+messageId+")");

    int channel = messageData[0] & AntDefine.CHANNEL_NUMBER_MASK;
    switch (channel) {
      case CHANNEL_NUMBER:
        decodeChannelMsg(messageId, messageData);
        break;
      default:
        Log.d(TAG, "Unhandled message: " + channel);
    }

    return true;
  }
  
  private String antMesgToStr(byte msg) {
    Field[] fields = AntMesg.class.getDeclaredFields();
    Set<String> fieldSet = new HashSet<String>();
    for (Field f:fields) {
      try {
        if (f.getType()==Byte.TYPE &&
            (f.getModifiers() & Modifier.STATIC)!=0 &&
            f.getName().matches("MESG_.*_ID") &&
            f.getByte(null)==msg) {
          fieldSet.add(f.getName());
        }
      } catch (IllegalArgumentException e) {
      } catch (IllegalAccessException e) {
      }
    }
    return fieldSet.toString();
  }

  private String antEventToStr(byte event) {
    Field[] fields = AntDefine.class.getDeclaredFields();
    Set<String> fieldSet = new HashSet<String>();
    for (Field f:fields) {
      try {
        if (f.getType()==Byte.TYPE &&
            (f.getModifiers() & Modifier.STATIC)!=0 &&
            f.getName().matches("EVENT.*") &&
            f.getByte(null)==event) {
          fieldSet.add(f.getName());
        }
      } catch (IllegalArgumentException e) {
      } catch (IllegalAccessException e) {
      }
    }
    return fieldSet.toString();
  }
  
  /**
   * Decode an ant device message.
   * @param messageData The byte array received from the device.
   */
  private void decodeChannelMsg(int messageId, byte[] messageData) {
    switch (messageId) {
      case AntMesg.MESG_BROADCAST_DATA_ID:
        handleBroadcastData(messageData);
        break;
      case AntMesg.MESG_RESPONSE_EVENT_ID:
        handleMessageResponse(messageData);
        break;
      case AntMesg.MESG_CHANNEL_ID_ID:
        handleChannelId(messageData);
        break;
      default:
        Log.e(TAG, "Unexpected message id: " + messageId);
    }
  }

  private void handleBroadcastData(byte[] antMessage) {
    if (deviceNumber == WILDCARD) {
      try {
        getAntReceiver().ANTRequestMessage(CHANNEL_NUMBER,
            AntMesg.MESG_CHANNEL_ID_ID);
      } catch (AntInterfaceException e) {
        Log.e(TAG, "ANT error handling broadcast data", e);
      }
      Log.d(TAG, "Requesting channel id id.");
    }

    setSensorState(Sensor.SensorState.CONNECTED);
    
//    int bpm = (int) antMessage[8] & 0xFF;
//    Sensor.SensorData.Builder b = Sensor.SensorData.newBuilder()
//      .setValue(bpm)
//      .setState(Sensor.SensorState.SENDING);
//    sensorData =
//      Sensor.SensorDataSet.newBuilder()
//      .setCreationTime(System.currentTimeMillis())
//      .setHeartRate(b)
//      .build();
  
    int messageType = antMessage[INDEX_MESSAGE_TYPE] & 0xFF;
    Log.d(Constants.TAG, "message-type="+messageType);
    
    switch (messageType) {
      case MSG_INITIAL:
          break;
      case MSG_DATA:
          parseDataMsg(antMessage);
          break;
    }
  }

  private void parseDataMsg(byte[] msg)
  {
      int messageId = msg[INDEX_MESSAGE_ID] & 0xFF;
      Log.d(Constants.TAG, "message-id="+messageId);
      
      int powerVal = (((msg[INDEX_MESSAGE_POWER] & 0xFF) << 8) | (msg[INDEX_MESSAGE_POWER+1] & 0xFF));
      int speedVal = (((msg[INDEX_MESSAGE_SPEED] & 0xFF) << 8) | (msg[INDEX_MESSAGE_SPEED+1] & 0xFF));
      int cadenceVal = (msg[INDEX_MESSAGE_CADENCE] & 0xFF);
      int bpmVal = (msg[INDEX_MESSAGE_BPM] & 0xFF);
      long time = System.currentTimeMillis();
      
      Sensor.SensorData.Builder power = 
        Sensor.SensorData.newBuilder()
            .setValue(powerVal)
            .setState(Sensor.SensorState.SENDING);
    
//      Sensor.SensorData.Builder speed = 
//        Sensor.SensorData.newBuilder()
//            .setValue(speedVal)
//            .setState(Sensor.SensorState.SENDING);

      Sensor.SensorData.Builder cadence = 
        Sensor.SensorData.newBuilder()
            .setValue(cadenceVal)
            .setState(Sensor.SensorState.SENDING);

      Sensor.SensorData.Builder bpm = 
        Sensor.SensorData.newBuilder()
            .setValue(bpmVal)
            .setState(Sensor.SensorState.SENDING);

      sensorData =
        Sensor.SensorDataSet.newBuilder()
            .setCreationTime(time)
            .setPower(power)
            .setCadence(cadence)
            .setHeartRate(bpm)
            .build();
      
      //Log.d("DATA", String.format("power=%4d, speed=%.1f, cadence=%4d, bpm=%4d", powerVal, (float)speedVal/10.0f, cadenceVal, bpmVal));
  }
  
  void handleChannelId(byte[] rawMessage) {
    AntChannelIdMessage message = new AntChannelIdMessage(rawMessage);
    deviceNumber = message.getDeviceNumber();
    Log.i(TAG, "Found device id: " + deviceNumber);

    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(context.getString(R.string.ant_srm_bridge_sensor_id_key), deviceNumber);
    editor.commit();
  }

  private void handleMessageResponse(byte[] rawMessage) {
    AntChannelResponseMessage message = new AntChannelResponseMessage(rawMessage);
    Log.i(Constants.TAG, "Received ANT Response: "+antMesgToStr(message.getMessageId())+"("+message.getMessageId()+")" +
    		", Code: "+antEventToStr(message.getMessageCode())+"("+message.getMessageCode()+")");
    switch (message.getMessageId()) {
      case AntMesg.MESG_EVENT_ID:
        if (message.getMessageCode() == AntDefine.EVENT_RX_SEARCH_TIMEOUT) {
          // Search timeout
          Log.w(TAG, "Search timed out. Unassigning channel.");
          try {
            getAntReceiver().ANTUnassignChannel((byte) 0);
          } catch (AntInterfaceException e) {
            Log.e(TAG, "ANT error unassigning channel", e);
          }
          //setSensorState(Sensor.SensorState.DISCONNECTED);
        }
        break;

      case AntMesg.MESG_UNASSIGN_CHANNEL_ID:
        setSensorState(Sensor.SensorState.DISCONNECTED);
        Log.i(TAG, "Disconnected from the sensor: " + getSensorState());
        break;
    }
  }

  @Override protected void setupAntSensorChannels() {
    Log.i(Constants.TAG, "Setting up ANT sensor channels");
    setupAntSensorChannel(
        NETWORK_NUMBER,
        CHANNEL_NUMBER,
        deviceNumber,
        DEVICE_TYPE,
        (byte) 0x01,
        CHANNEL_PERIOD,
        RF_FREQUENCY,
        (byte) 0);
  }

  public short getDeviceNumber() {
    return deviceNumber;
  }

  void setDeviceNumber(short deviceNumber) {
    this.deviceNumber = deviceNumber;
  }
}
