package au.csiro.umran.test.readblue.blueparser;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.util.Log;
import au.csiro.umran.test.readblue.Constants;
import au.csiro.umran.test.readblue.DeviceConnection;
import au.csiro.umran.test.readblue.ParsedMsg;
import au.csiro.umran.test.readblue.QuitableThread;
import au.csiro.umran.test.readblue.ReadBlueServiceBinder;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

/**
 * High priority thread dedicated to parsing input
 * 
 * @author Umran
 */
public class ParserThread extends QuitableThread {
	
	private ReadBlueServiceBinder binder;
	private DeviceConnection deviceConnection;
	private Parser parser;
	
	private String tag;
	
	public ParserThread(Context context, String name, byte[] marker, ReadBlueServiceBinder binder, DeviceConnection deviceConnection, InputStream inputStream, TwoWayBlockingQueue<ParsedMsg> outputQueue) {
		super(context, name);
		this.binder = binder;
		this.deviceConnection = deviceConnection;
		this.parser = new MarkerBasedParser(marker, binder, deviceConnection, inputStream, outputQueue);
		this.tag = Constants.TAG+"-"+deviceConnection.getConnectableDevice().getDevice().getName().replaceAll("\\W+", "_");
		this.setPriority(MAX_PRIORITY);
		this.start();
	}
	
	public Parser getParser() {
		return parser;
	}
	
	@Override
	public void doQuit() {
		this.parser.quit();
	}
	
	@Override
	public void doAction() {
		try {
			parser.process();
		} catch (IOException e) {
			Log.e(Constants.TAG, "Error while parsing input", e);
			binder.addMessage("Error while parsing input:\n"+e.getMessage());
			quit();
			this.deviceConnection.close();
		}
	}
	
	@Override
	public void doFinalize() {
		this.parser.quit();
	}

}