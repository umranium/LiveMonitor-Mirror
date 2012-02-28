package au.csiro.umran.test.readblue;

import android.util.Log;
import au.csiro.umran.test.readblue.blueparser.ParsedMsg;
import au.csiro.umran.test.readblue.filewriter.WriteMessage;
import au.csiro.umran.test.readblue.filewriter.WriterThread;
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
	
	private TwoWayBlockingQueue<ParsedMsg> queue;
	private ReadBlueServiceBinder binder;
	private WriterThread writerThread;
	
	public MessageReceiverThread(TwoWayBlockingQueue<ParsedMsg> queue, ReadBlueServiceBinder binder, WriterThread writerThread) {
		super("MessageReceiverThread");
		this.queue = queue;
		this.binder = binder;
		this.writerThread = writerThread;
		this.setPriority(MIN_PRIORITY);
		this.start();
	}
	
	@Override
	public void doAction() {
		ParsedMsg parsedMsg = this.queue.peekFilledInstance();
		if (parsedMsg!=null) {
			try {
				binder.addMessage(ByteUtils.bytesToString(parsedMsg.msg, 0, parsedMsg.msgLen));
				
				WriteMessage writeMsg = null;
				try {
					writeMsg = this.writerThread.getQueue().takeEmptyInstance();
					writeMsg.assign(parsedMsg.time, parsedMsg.msg, parsedMsg.msgLen);
					this.writerThread.getQueue().returnFilledInstance(writeMsg);
					writeMsg = null;
				} catch (InterruptedException e) {
					// ignore
				} finally {
					if (parsedMsg!=null) {
						try {
							this.writerThread.getQueue().returnEmptyInstance(writeMsg);
						} catch (InterruptedException e) {
							// ignore
						}
					}
				}
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