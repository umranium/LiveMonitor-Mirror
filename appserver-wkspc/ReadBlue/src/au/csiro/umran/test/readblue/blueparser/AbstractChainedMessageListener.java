package au.csiro.umran.test.readblue.blueparser;

public class AbstractChainedMessageListener implements OnMessageListener {
	
	private OnMessageListener messageListener;
	
	public AbstractChainedMessageListener(OnMessageListener messageListener) {
		this.messageListener = messageListener;
	}

	@Override
	public void onMessage(byte[] message, int length) {
		if (messageListener!=null) {
			messageListener.onMessage(message, length);
		}
	}

}
