package com.urremote.invoker.server.tophone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.google.gwt.user.server.Base64Utils;


public class MessageToPhoneUtil {
	
	public static String encodeMessage(MessageToPhone msg) throws IOException {
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
		objectOutputStream.writeObject(msg);
		objectOutputStream.close();
		
		byte[] values = byteOutputStream.toByteArray();
		String base64Values = Base64Utils.toBase64(values);
		
		return base64Values;
	}

	public static MessageToPhone decodeMessage(String msg) throws IOException, ClassNotFoundException {
		byte[] values = Base64Utils.fromBase64(msg);
		
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream(values);
		ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
		
		Object value = objectInputStream.readObject();

		return (MessageToPhone)value;
	}
	
}
