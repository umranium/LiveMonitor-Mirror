package com.urremote.messaging.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.jdo.PersistenceManager;

import com.urremote.messaging.common.InstanceCreator;

public class PersistenceUtil {
		
	public static <Type,InstanceCreatorParamType>
	Type findOrCreate(PersistenceManager pm, Class<? extends Type> cl, Object key, 
			InstanceCreator<Type,InstanceCreatorParamType> creator, InstanceCreatorParamType creatorParam) {
		try {
			return pm.getObjectById(cl, key);
		} catch (javax.jdo.JDOObjectNotFoundException e) {
			Type newVal = creator.createInstance(creatorParam);
			pm.makePersistent(newVal);
			return newVal;
		}
	}

	public static <Type,InstanceCreatorParamType>
	Type findOrCreate(PersistenceManager pm, Class<? extends Type> cl, Object key, Object[] constructorParams) {
		try {
			return pm.getObjectById(cl, key);
		} catch (javax.jdo.JDOObjectNotFoundException e) {
			Class<?>[] paramTypes = null;
			if (constructorParams!=null) {
				paramTypes = new Class[constructorParams.length];
				for (int i=0; i<constructorParams.length; ++i) {
					paramTypes[i] = constructorParams[i].getClass();
				}
			}
			try {
				Constructor<? extends Type> constructor = cl.getConstructor(paramTypes);
				Type newVal = constructor.newInstance(constructorParams);
				pm.makePersistent(newVal);
				return newVal;
			} catch (NoSuchMethodException e2) {
				throw new RuntimeException(e2);
			} catch (SecurityException e2) {
				throw new RuntimeException(e2);
			} catch (InstantiationException e2) {
				throw new RuntimeException(e2);
			} catch (IllegalAccessException e2) {
				throw new RuntimeException(e2);
			} catch (IllegalArgumentException e2) {
				throw new RuntimeException(e2);
			} catch (InvocationTargetException e2) {
				throw new RuntimeException(e2);
			}
		}
	}
	
}
