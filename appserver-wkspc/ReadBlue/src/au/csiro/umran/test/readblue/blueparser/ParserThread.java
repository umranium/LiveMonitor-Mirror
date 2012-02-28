package au.csiro.umran.test.readblue.blueparser;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;
import au.csiro.umran.test.readblue.Constants;
import au.csiro.umran.test.readblue.DeviceConnection;
import au.csiro.umran.test.readblue.QuitableThread;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

/**
 * High priority thread dedicated to parsing input
 * 
 * @author Umran
 */
public class ParserThread extends QuitableThread {
	
	private DeviceConnection deviceConnection;
	private Parser parser;
	
	public ParserThread(String name, byte[] marker, DeviceConnection deviceConnection, InputStream inputStream, TwoWayBlockingQueue<ParsedMsg> outputQueue) {
		super(name);
		this.deviceConnection = deviceConnection;
		this.parser = new MarkerBasedParser(marker, deviceConnection, inputStream, outputQueue);
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
			quit();
			this.deviceConnection.close();
		}
	}
	
	@Override
	public void doFinalize() {
	}

}