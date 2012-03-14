package au.csiro.umran.test.readblue;

import java.io.IOException;

import android.content.Context;
import android.util.Log;
import au.csiro.umran.test.readblue.filewriter.Writer;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

/**
 * Low priority thread dedicated to processing parsed messages.
 * 
 * Currently displays the messages onto the screen using the {@link ReadBlueServiceBinder#addMessage(String)} method,
 * and writes them to file. 
 * 
 * @author Umran
 */
class MessageReceiverThread extends QuitableThread {
	
	private DeviceConnection connection;
	private TwoWayBlockingQueue<ParsedMsg> queue;
	private ReadBlueServiceBinder binder;
	private Writer writerThread;
	
	public MessageReceiverThread(Context context, DeviceConnection connection, TwoWayBlockingQueue<ParsedMsg> queue, ReadBlueServiceBinder binder, Writer writerThread) {
		super(context, "MessageReceiverThread:"+connection.getConnectableDevice().getDevice().getName());
		this.connection = connection;
		this.queue = queue;
		this.binder = binder;
		this.writerThread = writerThread;
		this.setPriority(MIN_PRIORITY);
		this.start();
	}
	
	@Override
	public void doAction() {
		if (this.queue.peekFilledInstance()!=null) {
			ParsedMsg parsedMsg = null;
			try {
				parsedMsg = this.queue.takeFilledInstance();
				//binder.addMessage(ByteUtils.bytesToString(parsedMsg.msg, 0, parsedMsg.msgLen));
				writerThread.writeToFile(parsedMsg);
				this.queue.returnEmptyInstance(parsedMsg);
				parsedMsg = null;
			} catch (IOException e) {
				Log.e(Constants.TAG, "Error while writing to device data to file: "+connection.getConnectableDevice().getDevice().getName(), e);
				quit();
			} catch (InterruptedException e) {
				// ignore
			} finally {
				if (parsedMsg!=null) {
					try {
						this.queue.returnEmptyInstance(parsedMsg);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		}
	}
	
	@Override
	public void doFinalize() {
	}
	
	@Override
	public void doQuit() {
	}
	
}