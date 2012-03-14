package au.csiro.umran.test.readblue.blueparser;

public class PacketVerificationException extends Exception {

	public PacketVerificationException() {
		super();
	}

	public PacketVerificationException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public PacketVerificationException(String detailMessage) {
		super(detailMessage);
	}

	public PacketVerificationException(Throwable throwable) {
		super(throwable);
	}
	
}