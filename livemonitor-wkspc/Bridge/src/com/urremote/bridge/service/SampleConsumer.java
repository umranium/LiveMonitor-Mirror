package com.urremote.bridge.service;

public class SampleConsumer {

	private Object sampleMutex = new Object();
	private Sample sample = null;
	
	public Sample consumeSample() {
		if (sample==null) {
			try {
				synchronized (sampleMutex) {
					sampleMutex.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		synchronized (this) {
			Sample s = sample;
			sample = null;
			return s;
		}
	}
	
	public void depositSample(Sample sample)
	{
		synchronized (sampleMutex) {
			sampleMutex.notify();
		}
		synchronized (this) {
			this.sample = sample;
		}
	}
	
}
