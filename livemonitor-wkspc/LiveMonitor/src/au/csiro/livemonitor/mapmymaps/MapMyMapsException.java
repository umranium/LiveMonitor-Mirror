package au.csiro.livemonitor.mapmymaps;

public class MapMyMapsException extends Exception {

	public MapMyMapsException() {
	}

	public MapMyMapsException(String detailMessage) {
		super(detailMessage);
	}

	public MapMyMapsException(Throwable throwable) {
		super(throwable);
	}

	public MapMyMapsException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
