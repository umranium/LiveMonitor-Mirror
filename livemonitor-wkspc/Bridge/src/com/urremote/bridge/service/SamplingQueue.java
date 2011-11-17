package com.urremote.bridge.service;

import com.urremote.bridge.service.utils.TwoWayBlockingQueue;

public class SamplingQueue extends TwoWayBlockingQueue<Sample> {
	
	public SamplingQueue(int capacity) {
		super(capacity);
	}

	@Override
	protected Sample getNewInstance() {
		return new Sample();
	}
	
}
