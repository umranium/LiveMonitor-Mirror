// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: proto/sensor.proto

package com.google.android.apps.mytracks.content;

public final class Sensor {
  private Sensor() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }
  public enum SensorState
      implements com.google.protobuf.Internal.EnumLite {
    NONE(0, 1),
    CONNECTING(1, 2),
    CONNECTED(2, 3),
    DISCONNECTED(3, 4),
    SENDING(4, 5),
    ;
    
    
    public final int getNumber() { return value; }
    
    public static SensorState valueOf(int value) {
      switch (value) {
        case 1: return NONE;
        case 2: return CONNECTING;
        case 3: return CONNECTED;
        case 4: return DISCONNECTED;
        case 5: return SENDING;
        default: return null;
      }
    }
    
    public static com.google.protobuf.Internal.EnumLiteMap<SensorState>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static com.google.protobuf.Internal.EnumLiteMap<SensorState>
        internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<SensorState>() {
            public SensorState findValueByNumber(int number) {
              return SensorState.valueOf(number)
    ;        }
          };
    
    private final int index;
    private final int value;
    private SensorState(int index, int value) {
      this.index = index;
      this.value = value;
    }
    
    // @@protoc_insertion_point(enum_scope:com.google.android.apps.mytracks.content.SensorState)
  }
  
  public static final class SensorData extends
      com.google.protobuf.GeneratedMessageLite {
    // Use SensorData.newBuilder() to construct.
    private SensorData() {
      initFields();
    }
    private SensorData(boolean noInit) {}
    
    private static final SensorData defaultInstance;
    public static SensorData getDefaultInstance() {
      return defaultInstance;
    }
    
    public SensorData getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    // required .com.google.android.apps.mytracks.content.SensorState state = 1 [default = NONE];
    public static final int STATE_FIELD_NUMBER = 1;
    private boolean hasState;
    private com.google.android.apps.mytracks.content.Sensor.SensorState state_;
    public boolean hasState() { return hasState; }
    public com.google.android.apps.mytracks.content.Sensor.SensorState getState() { return state_; }
    
    // optional int32 value = 2;
    public static final int VALUE_FIELD_NUMBER = 2;
    private boolean hasValue;
    private int value_ = 0;
    public boolean hasValue() { return hasValue; }
    public int getValue() { return value_; }
    
    private void initFields() {
      state_ = com.google.android.apps.mytracks.content.Sensor.SensorState.NONE;
    }
    public final boolean isInitialized() {
      if (!hasState) return false;
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasState()) {
        output.writeEnum(1, getState().getNumber());
      }
      if (hasValue()) {
        output.writeInt32(2, getValue());
      }
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasState()) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(1, getState().getNumber());
      }
      if (hasValue()) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(2, getValue());
      }
      memoizedSerializedSize = size;
      return size;
    }
    
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorData parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.google.android.apps.mytracks.content.Sensor.SensorData prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageLite.Builder<
          com.google.android.apps.mytracks.content.Sensor.SensorData, Builder> {
      private com.google.android.apps.mytracks.content.Sensor.SensorData result;
      
      // Construct using com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new com.google.android.apps.mytracks.content.Sensor.SensorData();
        return builder;
      }
      
      protected com.google.android.apps.mytracks.content.Sensor.SensorData internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new com.google.android.apps.mytracks.content.Sensor.SensorData();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public com.google.android.apps.mytracks.content.Sensor.SensorData getDefaultInstanceForType() {
        return com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public com.google.android.apps.mytracks.content.Sensor.SensorData build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private com.google.android.apps.mytracks.content.Sensor.SensorData buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public com.google.android.apps.mytracks.content.Sensor.SensorData buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        com.google.android.apps.mytracks.content.Sensor.SensorData returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(com.google.android.apps.mytracks.content.Sensor.SensorData other) {
        if (other == com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance()) return this;
        if (other.hasState()) {
          setState(other.getState());
        }
        if (other.hasValue()) {
          setValue(other.getValue());
        }
        return this;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              return this;
            default: {
              if (!parseUnknownField(input, extensionRegistry, tag)) {
                return this;
              }
              break;
            }
            case 8: {
              int rawValue = input.readEnum();
              com.google.android.apps.mytracks.content.Sensor.SensorState value = com.google.android.apps.mytracks.content.Sensor.SensorState.valueOf(rawValue);
              if (value != null) {
                setState(value);
              }
              break;
            }
            case 16: {
              setValue(input.readInt32());
              break;
            }
          }
        }
      }
      
      
      // required .com.google.android.apps.mytracks.content.SensorState state = 1 [default = NONE];
      public boolean hasState() {
        return result.hasState();
      }
      public com.google.android.apps.mytracks.content.Sensor.SensorState getState() {
        return result.getState();
      }
      public Builder setState(com.google.android.apps.mytracks.content.Sensor.SensorState value) {
        if (value == null) {
          throw new NullPointerException();
        }
        result.hasState = true;
        result.state_ = value;
        return this;
      }
      public Builder clearState() {
        result.hasState = false;
        result.state_ = com.google.android.apps.mytracks.content.Sensor.SensorState.NONE;
        return this;
      }
      
      // optional int32 value = 2;
      public boolean hasValue() {
        return result.hasValue();
      }
      public int getValue() {
        return result.getValue();
      }
      public Builder setValue(int value) {
        result.hasValue = true;
        result.value_ = value;
        return this;
      }
      public Builder clearValue() {
        result.hasValue = false;
        result.value_ = 0;
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:com.google.android.apps.mytracks.content.SensorData)
    }
    
    static {
      defaultInstance = new SensorData(true);
      com.google.android.apps.mytracks.content.Sensor.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:com.google.android.apps.mytracks.content.SensorData)
  }
  
  public static final class SensorDataSet extends
      com.google.protobuf.GeneratedMessageLite {
    // Use SensorDataSet.newBuilder() to construct.
    private SensorDataSet() {
      initFields();
    }
    private SensorDataSet(boolean noInit) {}
    
    private static final SensorDataSet defaultInstance;
    public static SensorDataSet getDefaultInstance() {
      return defaultInstance;
    }
    
    public SensorDataSet getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    // optional uint64 creation_time = 1 [default = 0];
    public static final int CREATION_TIME_FIELD_NUMBER = 1;
    private boolean hasCreationTime;
    private long creationTime_ = 0L;
    public boolean hasCreationTime() { return hasCreationTime; }
    public long getCreationTime() { return creationTime_; }
    
    // optional .com.google.android.apps.mytracks.content.SensorData heart_rate = 2;
    public static final int HEART_RATE_FIELD_NUMBER = 2;
    private boolean hasHeartRate;
    private com.google.android.apps.mytracks.content.Sensor.SensorData heartRate_;
    public boolean hasHeartRate() { return hasHeartRate; }
    public com.google.android.apps.mytracks.content.Sensor.SensorData getHeartRate() { return heartRate_; }
    
    // optional .com.google.android.apps.mytracks.content.SensorData cadence = 3;
    public static final int CADENCE_FIELD_NUMBER = 3;
    private boolean hasCadence;
    private com.google.android.apps.mytracks.content.Sensor.SensorData cadence_;
    public boolean hasCadence() { return hasCadence; }
    public com.google.android.apps.mytracks.content.Sensor.SensorData getCadence() { return cadence_; }
    
    // optional .com.google.android.apps.mytracks.content.SensorData power = 4;
    public static final int POWER_FIELD_NUMBER = 4;
    private boolean hasPower;
    private com.google.android.apps.mytracks.content.Sensor.SensorData power_;
    public boolean hasPower() { return hasPower; }
    public com.google.android.apps.mytracks.content.Sensor.SensorData getPower() { return power_; }
    
    // optional .com.google.android.apps.mytracks.content.SensorData battery_level = 5;
    public static final int BATTERY_LEVEL_FIELD_NUMBER = 5;
    private boolean hasBatteryLevel;
    private com.google.android.apps.mytracks.content.Sensor.SensorData batteryLevel_;
    public boolean hasBatteryLevel() { return hasBatteryLevel; }
    public com.google.android.apps.mytracks.content.Sensor.SensorData getBatteryLevel() { return batteryLevel_; }
    
    private void initFields() {
      heartRate_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
      cadence_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
      power_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
      batteryLevel_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
    }
    public final boolean isInitialized() {
      if (hasHeartRate()) {
        if (!getHeartRate().isInitialized()) return false;
      }
      if (hasCadence()) {
        if (!getCadence().isInitialized()) return false;
      }
      if (hasPower()) {
        if (!getPower().isInitialized()) return false;
      }
      if (hasBatteryLevel()) {
        if (!getBatteryLevel().isInitialized()) return false;
      }
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasCreationTime()) {
        output.writeUInt64(1, getCreationTime());
      }
      if (hasHeartRate()) {
        output.writeMessage(2, getHeartRate());
      }
      if (hasCadence()) {
        output.writeMessage(3, getCadence());
      }
      if (hasPower()) {
        output.writeMessage(4, getPower());
      }
      if (hasBatteryLevel()) {
        output.writeMessage(5, getBatteryLevel());
      }
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasCreationTime()) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(1, getCreationTime());
      }
      if (hasHeartRate()) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(2, getHeartRate());
      }
      if (hasCadence()) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(3, getCadence());
      }
      if (hasPower()) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(4, getPower());
      }
      if (hasBatteryLevel()) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(5, getBatteryLevel());
      }
      memoizedSerializedSize = size;
      return size;
    }
    
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.google.android.apps.mytracks.content.Sensor.SensorDataSet parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.google.android.apps.mytracks.content.Sensor.SensorDataSet prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageLite.Builder<
          com.google.android.apps.mytracks.content.Sensor.SensorDataSet, Builder> {
      private com.google.android.apps.mytracks.content.Sensor.SensorDataSet result;
      
      // Construct using com.google.android.apps.mytracks.content.Sensor.SensorDataSet.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new com.google.android.apps.mytracks.content.Sensor.SensorDataSet();
        return builder;
      }
      
      protected com.google.android.apps.mytracks.content.Sensor.SensorDataSet internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new com.google.android.apps.mytracks.content.Sensor.SensorDataSet();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public com.google.android.apps.mytracks.content.Sensor.SensorDataSet getDefaultInstanceForType() {
        return com.google.android.apps.mytracks.content.Sensor.SensorDataSet.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public com.google.android.apps.mytracks.content.Sensor.SensorDataSet build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private com.google.android.apps.mytracks.content.Sensor.SensorDataSet buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public com.google.android.apps.mytracks.content.Sensor.SensorDataSet buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        com.google.android.apps.mytracks.content.Sensor.SensorDataSet returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(com.google.android.apps.mytracks.content.Sensor.SensorDataSet other) {
        if (other == com.google.android.apps.mytracks.content.Sensor.SensorDataSet.getDefaultInstance()) return this;
        if (other.hasCreationTime()) {
          setCreationTime(other.getCreationTime());
        }
        if (other.hasHeartRate()) {
          mergeHeartRate(other.getHeartRate());
        }
        if (other.hasCadence()) {
          mergeCadence(other.getCadence());
        }
        if (other.hasPower()) {
          mergePower(other.getPower());
        }
        if (other.hasBatteryLevel()) {
          mergeBatteryLevel(other.getBatteryLevel());
        }
        return this;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              return this;
            default: {
              if (!parseUnknownField(input, extensionRegistry, tag)) {
                return this;
              }
              break;
            }
            case 8: {
              setCreationTime(input.readUInt64());
              break;
            }
            case 18: {
              com.google.android.apps.mytracks.content.Sensor.SensorData.Builder subBuilder = com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder();
              if (hasHeartRate()) {
                subBuilder.mergeFrom(getHeartRate());
              }
              input.readMessage(subBuilder, extensionRegistry);
              setHeartRate(subBuilder.buildPartial());
              break;
            }
            case 26: {
              com.google.android.apps.mytracks.content.Sensor.SensorData.Builder subBuilder = com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder();
              if (hasCadence()) {
                subBuilder.mergeFrom(getCadence());
              }
              input.readMessage(subBuilder, extensionRegistry);
              setCadence(subBuilder.buildPartial());
              break;
            }
            case 34: {
              com.google.android.apps.mytracks.content.Sensor.SensorData.Builder subBuilder = com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder();
              if (hasPower()) {
                subBuilder.mergeFrom(getPower());
              }
              input.readMessage(subBuilder, extensionRegistry);
              setPower(subBuilder.buildPartial());
              break;
            }
            case 42: {
              com.google.android.apps.mytracks.content.Sensor.SensorData.Builder subBuilder = com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder();
              if (hasBatteryLevel()) {
                subBuilder.mergeFrom(getBatteryLevel());
              }
              input.readMessage(subBuilder, extensionRegistry);
              setBatteryLevel(subBuilder.buildPartial());
              break;
            }
          }
        }
      }
      
      
      // optional uint64 creation_time = 1 [default = 0];
      public boolean hasCreationTime() {
        return result.hasCreationTime();
      }
      public long getCreationTime() {
        return result.getCreationTime();
      }
      public Builder setCreationTime(long value) {
        result.hasCreationTime = true;
        result.creationTime_ = value;
        return this;
      }
      public Builder clearCreationTime() {
        result.hasCreationTime = false;
        result.creationTime_ = 0L;
        return this;
      }
      
      // optional .com.google.android.apps.mytracks.content.SensorData heart_rate = 2;
      public boolean hasHeartRate() {
        return result.hasHeartRate();
      }
      public com.google.android.apps.mytracks.content.Sensor.SensorData getHeartRate() {
        return result.getHeartRate();
      }
      public Builder setHeartRate(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (value == null) {
          throw new NullPointerException();
        }
        result.hasHeartRate = true;
        result.heartRate_ = value;
        return this;
      }
      public Builder setHeartRate(com.google.android.apps.mytracks.content.Sensor.SensorData.Builder builderForValue) {
        result.hasHeartRate = true;
        result.heartRate_ = builderForValue.build();
        return this;
      }
      public Builder mergeHeartRate(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (result.hasHeartRate() &&
            result.heartRate_ != com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance()) {
          result.heartRate_ =
            com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder(result.heartRate_).mergeFrom(value).buildPartial();
        } else {
          result.heartRate_ = value;
        }
        result.hasHeartRate = true;
        return this;
      }
      public Builder clearHeartRate() {
        result.hasHeartRate = false;
        result.heartRate_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
        return this;
      }
      
      // optional .com.google.android.apps.mytracks.content.SensorData cadence = 3;
      public boolean hasCadence() {
        return result.hasCadence();
      }
      public com.google.android.apps.mytracks.content.Sensor.SensorData getCadence() {
        return result.getCadence();
      }
      public Builder setCadence(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (value == null) {
          throw new NullPointerException();
        }
        result.hasCadence = true;
        result.cadence_ = value;
        return this;
      }
      public Builder setCadence(com.google.android.apps.mytracks.content.Sensor.SensorData.Builder builderForValue) {
        result.hasCadence = true;
        result.cadence_ = builderForValue.build();
        return this;
      }
      public Builder mergeCadence(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (result.hasCadence() &&
            result.cadence_ != com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance()) {
          result.cadence_ =
            com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder(result.cadence_).mergeFrom(value).buildPartial();
        } else {
          result.cadence_ = value;
        }
        result.hasCadence = true;
        return this;
      }
      public Builder clearCadence() {
        result.hasCadence = false;
        result.cadence_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
        return this;
      }
      
      // optional .com.google.android.apps.mytracks.content.SensorData power = 4;
      public boolean hasPower() {
        return result.hasPower();
      }
      public com.google.android.apps.mytracks.content.Sensor.SensorData getPower() {
        return result.getPower();
      }
      public Builder setPower(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (value == null) {
          throw new NullPointerException();
        }
        result.hasPower = true;
        result.power_ = value;
        return this;
      }
      public Builder setPower(com.google.android.apps.mytracks.content.Sensor.SensorData.Builder builderForValue) {
        result.hasPower = true;
        result.power_ = builderForValue.build();
        return this;
      }
      public Builder mergePower(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (result.hasPower() &&
            result.power_ != com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance()) {
          result.power_ =
            com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder(result.power_).mergeFrom(value).buildPartial();
        } else {
          result.power_ = value;
        }
        result.hasPower = true;
        return this;
      }
      public Builder clearPower() {
        result.hasPower = false;
        result.power_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
        return this;
      }
      
      // optional .com.google.android.apps.mytracks.content.SensorData battery_level = 5;
      public boolean hasBatteryLevel() {
        return result.hasBatteryLevel();
      }
      public com.google.android.apps.mytracks.content.Sensor.SensorData getBatteryLevel() {
        return result.getBatteryLevel();
      }
      public Builder setBatteryLevel(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (value == null) {
          throw new NullPointerException();
        }
        result.hasBatteryLevel = true;
        result.batteryLevel_ = value;
        return this;
      }
      public Builder setBatteryLevel(com.google.android.apps.mytracks.content.Sensor.SensorData.Builder builderForValue) {
        result.hasBatteryLevel = true;
        result.batteryLevel_ = builderForValue.build();
        return this;
      }
      public Builder mergeBatteryLevel(com.google.android.apps.mytracks.content.Sensor.SensorData value) {
        if (result.hasBatteryLevel() &&
            result.batteryLevel_ != com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance()) {
          result.batteryLevel_ =
            com.google.android.apps.mytracks.content.Sensor.SensorData.newBuilder(result.batteryLevel_).mergeFrom(value).buildPartial();
        } else {
          result.batteryLevel_ = value;
        }
        result.hasBatteryLevel = true;
        return this;
      }
      public Builder clearBatteryLevel() {
        result.hasBatteryLevel = false;
        result.batteryLevel_ = com.google.android.apps.mytracks.content.Sensor.SensorData.getDefaultInstance();
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:com.google.android.apps.mytracks.content.SensorDataSet)
    }
    
    static {
      defaultInstance = new SensorDataSet(true);
      com.google.android.apps.mytracks.content.Sensor.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:com.google.android.apps.mytracks.content.SensorDataSet)
  }
  
  
  static {
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}