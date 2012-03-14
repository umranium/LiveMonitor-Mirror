package au.csiro.umran.test.readblue;

import au.csiro.umran.test.readblue.ParsedMsg;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

public class ParsedMsgQueue extends TwoWayBlockingQueue<ParsedMsg> {

	public ParsedMsgQueue() {
		super(Constants.BUFFER_MSG_LENGTH);
	}

	@Override
	protected ParsedMsg getNewInstance() {
		return new ParsedMsg();
	}
	
	

}
