package com.urremote.messaging.common;

public interface InstanceCreator<ReturnType,ParamType> {

	ReturnType createInstance(ParamType param);
	
}
