package com.urremote.invoker.client.utils;

import java.util.Arrays;

import com.google.gwt.core.client.GWT;

public class Base64Decode {
	private final static String base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	public static byte[] decode(String s) {

		// remove/ignore any characters not in the base64 characters list
		// or the pad character -- particularly newlines
		s = s.replaceAll("[^" + base64chars + "=]", "");

		// replace any incoming padding with a zero pad (the 'A' character is
		// zero)
		String p = (s.charAt(s.length() - 1) == '=' ? (s.charAt(s.length() - 2) == '=' ? "AA" : "A")
				: "");

		s = s.substring(0, s.length() - p.length()) + p;
		int resLength = (int) Math.ceil(((float) (s.length()) / 4f) * 3f);
		byte[] bufIn = new byte[resLength];
		int bufIn_i = 0;

		// increment over the length of this encrypted string, four characters
		// at a time
		for (int c = 0; c < s.length(); c += 4) {

			// each of these four characters represents a 6-bit index in the
			// base64 characters list which, when concatenated, will give the
			// 24-bit number for the original 3 characters
			int n = (base64chars.indexOf(s.charAt(c)) << 18)
					+ (base64chars.indexOf(s.charAt(c + 1)) << 12)
					+ (base64chars.indexOf(s.charAt(c + 2)) << 6)
					+ base64chars.indexOf(s.charAt(c + 3));

			// split the 24-bit number into the original three 8-bit (ASCII)
			// characters

			char c1 = (char) ((n >>> 16) & 0xFF);
			char c2 = (char) ((n >>> 8) & 0xFF);
			char c3 = (char) (n & 0xFF);

			bufIn[bufIn_i++] = (byte) c1;
			bufIn[bufIn_i++] = (byte) c2;
			bufIn[bufIn_i++] = (byte) c3;

		}
		
		for (int i=0; i<resLength; ++i) {
			if ((bufIn[i] & 0xFF) == 0) {
				GWT.log("ERROR: bufIn["+i+"/"+resLength+"] = 0");
			}
		}

		//	for some reason, the last byte is sometimes zero...
		// TODO: Figure out why sometimes the last byte has 0 in it
		if (bufIn[resLength-1]==0) {
			byte[] newOut = new byte[resLength-1];
			for (int i=0; i<resLength-1; ++i)
				newOut[i] = bufIn[i];
			return newOut;
		} else {
			return bufIn;
		}
	}
}