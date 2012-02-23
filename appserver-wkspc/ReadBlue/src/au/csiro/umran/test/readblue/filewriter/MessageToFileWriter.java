package au.csiro.umran.test.readblue.filewriter;

import java.io.File;
import java.io.IOException;

import au.csiro.umran.test.readblue.blueparser.AbstractChainedMessageListener;
import au.csiro.umran.test.readblue.blueparser.OnMessageListener;

public class MessageToFileWriter extends AbstractChainedMessageListener {
	
	private WriterThread writerThread;
	
	public MessageToFileWriter(OnMessageListener onMessageListener, File outputFile) throws IOException {
		super(onMessageListener);
		this.writerThread = new WriterThread(outputFile);
	}
	
	public void close() throws IOException {
		this.writerThread.quit();
	}

	@Override
	public void onMessage(long timeStamp, byte[] message, int length) {
		WriteMessage msg = null;
		try {
			msg = this.writerThread.getQueue().takeEmptyInstance();
			msg.assign(timeStamp, message, length);
			this.writerThread.getQueue().returnFilledInstance(msg);
			msg = null;
		} catch (InterruptedException e) {
			// ignore
		} finally {
			if (msg!=null) {
				try {
					this.writerThread.getQueue().returnEmptyInstance(msg);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		
		super.onMessage(timeStamp, message, length);
	}
	
}
