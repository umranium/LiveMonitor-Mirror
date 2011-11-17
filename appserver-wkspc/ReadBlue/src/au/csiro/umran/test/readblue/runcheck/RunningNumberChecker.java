package au.csiro.umran.test.readblue.runcheck;

import au.csiro.umran.test.readblue.ByteUtils;
import au.csiro.umran.test.readblue.blueparser.AbstractChainedMessageListener;
import au.csiro.umran.test.readblue.blueparser.OnMessageListener;

public class RunningNumberChecker extends AbstractChainedMessageListener {
	
	private byte[] currentNumber;
	private int lengthOfRunningNumber;
	private boolean numberInitialized;
	
	public RunningNumberChecker(OnMessageListener onMessageListener, int lengthOfRunningNumber) {
		super(onMessageListener);
		this.currentNumber = new byte[lengthOfRunningNumber];
		this.lengthOfRunningNumber = lengthOfRunningNumber;
		this.numberInitialized = false;
	}
	
	private void incrRunningNumber()
	{
		int val;
		for (int i=0; i<lengthOfRunningNumber; ++i) {
			val = (currentNumber[i] & 0xFF);
			++val;
			currentNumber[i] = (byte)(val & 0xFF);
			if (val<=0xFF) {
				break;
			}
		}
	}
	
	@Override
	public void onMessage(byte[] message, int length) {
		if (length<lengthOfRunningNumber) {
			throw new RuntimeException("Message is shorter than the expected running number! ("+length+"<"+lengthOfRunningNumber+")");
		}
		
		if (!numberInitialized) {
			for (int i=1; i<=lengthOfRunningNumber; ++i) {
				currentNumber[lengthOfRunningNumber-i] = message[length-i];
			}
			numberInitialized = true;
		} else {
			incrRunningNumber();
			
			// check if running number is as expected
			for (int i=1; i<=lengthOfRunningNumber; ++i) {
				if (currentNumber[lengthOfRunningNumber-i] != message[length-i]) {
					throw new RuntimeException("Unexpected Running Number. Found:["+
								ByteUtils.bytesToString(message, length-lengthOfRunningNumber, lengthOfRunningNumber)+
								"], Expected:["+
								ByteUtils.bytesToString(currentNumber, 0, lengthOfRunningNumber)+"]");
				}
			}
		}
		
		super.onMessage(message, length);
	}

}
