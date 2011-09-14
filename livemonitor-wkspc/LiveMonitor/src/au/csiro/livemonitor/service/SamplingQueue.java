package au.csiro.livemonitor.service;

import au.csiro.livemonitor.service.utils.TwoWayBlockingQueue;

public class SamplingQueue extends TwoWayBlockingQueue<Sample> {

	public SamplingQueue(int capacity) {
		super(capacity);
	}

	@Override
	protected Sample getNewInstance() {
		return new Sample();
	}

}
