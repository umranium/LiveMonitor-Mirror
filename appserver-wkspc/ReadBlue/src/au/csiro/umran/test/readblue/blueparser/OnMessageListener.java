package au.csiro.umran.test.readblue.blueparser;

public interface OnMessageListener {
	
	public void onMessage(long timeStamp, byte[] message, int length);

}
