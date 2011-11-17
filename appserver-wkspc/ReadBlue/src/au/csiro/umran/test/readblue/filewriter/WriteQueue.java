package au.csiro.umran.test.readblue.filewriter;

import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

public class WriteQueue extends TwoWayBlockingQueue<WriteMessage> {

	public WriteQueue(int capacity) {
		super(capacity);
	}

	@Override
	protected WriteMessage getNewInstance() {
		return new WriteMessage();
	}
	
	

}
