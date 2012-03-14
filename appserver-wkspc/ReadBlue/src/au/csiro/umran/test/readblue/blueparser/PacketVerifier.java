package au.csiro.umran.test.readblue.blueparser;

import au.csiro.umran.test.readblue.ByteUtils;

public class PacketVerifier {
	
	private byte[] marker;
	private byte[] currentNumber;
	private int lengthOfRunningNumber;
	private boolean numberInitialized;
	
	public PacketVerifier(byte[] marker, int lengthOfRunningNumber) {
		this.marker = marker;
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
	
	public void check(byte[] message, int length) throws PacketVerificationException
	{
		if (length<lengthOfRunningNumber) {
			throw new PacketVerificationException("Message is shorter than the expected running number! ("+length+"<"+lengthOfRunningNumber+")");
		}
		
		for (int i=0; i<marker.length; ++i) {
			if (message[i]!=marker[i]) {
				throw new PacketVerificationException("Invalid start of message: "+ByteUtils.bytesToString(message, 0, length)+", expected marker "+ByteUtils.bytesToString(marker, 0, marker.length));
			}
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
					throw new PacketVerificationException("Unexpected Running Number. Found:["+
								ByteUtils.bytesToString(message, length-lengthOfRunningNumber, lengthOfRunningNumber)+
								"], Expected:["+
								ByteUtils.bytesToString(currentNumber, 0, lengthOfRunningNumber)+"]");
				}
			}
		}
	}
}