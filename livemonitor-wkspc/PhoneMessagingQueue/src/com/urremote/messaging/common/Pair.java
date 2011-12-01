package com.urremote.messaging.common;

public class Pair<FirstType,SecondType> {

	private FirstType first;
	private SecondType second;
	
	public Pair(FirstType first, SecondType second) {
		super();
		this.first = first;
		this.second = second;
	}
	
	public FirstType getFirst() {
		return first;
	}
	
	public SecondType getSecond() {
		return second;
	}
	
	public static <T1,T2> Pair<T1,T2> create(T1 t1, T2 t2) {
		return new Pair<T1, T2>(t1, t2);
	}
	
}
