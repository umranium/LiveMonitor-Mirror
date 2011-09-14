package au.csiro.livemonitor.service;

public interface InternalServiceMessageHandler {

	void onSystemMessage(String msg);
	void onCriticalError(String msg);
	void requestLaunchMyTracks();
	
}
