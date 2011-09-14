package au.csiro.blueparser;

public interface OnMessageListener {
	
	public void onMessage(byte[] message, int length);

}
