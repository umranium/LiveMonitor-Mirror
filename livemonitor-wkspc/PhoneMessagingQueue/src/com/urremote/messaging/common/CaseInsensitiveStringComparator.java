package com.urremote.messaging.common;

import java.util.Comparator;

public class CaseInsensitiveStringComparator implements Comparator<String> {
	
	public static final CaseInsensitiveStringComparator INSTANCE = new CaseInsensitiveStringComparator();

	@Override
	public int compare(String s1, String s2) {
		return s1.compareToIgnoreCase(s2);
	}

}
