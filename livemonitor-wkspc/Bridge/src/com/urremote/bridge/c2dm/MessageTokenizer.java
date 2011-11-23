package com.urremote.bridge.c2dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.urremote.bridge.common.Constants;

import android.util.Log;

public class MessageTokenizer {

	private static final char SEP = '.'; 
	
	public static List<String> parseTokens(String str) {
		List<String> list = new ArrayList<String>();
		
		boolean isSlash = false;
		StringBuilder currentToken = new StringBuilder();
		for (int len=str.length(), i=0; i<len; ++i) {
			char c = str.charAt(i);
			
			boolean prevWasSlash = isSlash;
			isSlash = false;
			
			switch (c) {
			case '\\': {
				if (prevWasSlash) {
					currentToken.append(c);
				} else {
					isSlash = true;
				}
				break;
			}
			case SEP: {
				if (prevWasSlash) {
					currentToken.append(c);
				} else {
					if (currentToken.length() > 0) {
						list.add(currentToken.toString());
						currentToken.setLength(0);
					}
				}
				break;
			}
			default:
				currentToken.append(c);
			}
		}

		if (currentToken.length()>0) {
			list.add(currentToken.toString());
		}
		
		//Log.d(Constants.TAG, "parsed: "+str+" to "+list);
		
		return list;
	}
	
	public static String encodeTokens(List<String> values) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<values.size(); ++i){
			if (i>0)
				builder.append(SEP);
			String val = values.get(i);
			
			val = val.replace("\\", "\\\\");
			val = val.replace(Character.toString(SEP), "\\"+SEP);
			builder.append(val);
		}
		return builder.toString();
	}
	
	public static String encodeTokens(String[] values) {
		return encodeTokens(Arrays.asList(values));
	}
	
}
