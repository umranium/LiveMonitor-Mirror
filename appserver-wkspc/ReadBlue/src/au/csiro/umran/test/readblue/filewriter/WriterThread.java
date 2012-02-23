package au.csiro.umran.test.readblue.filewriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;
import au.csiro.umran.test.readblue.Constants;

/**
 * A low priority thread, responsible for writing data to file.
 * 
 * It is set to low priority so that the sampling/data receiving thread
 * can gain higher priority. On the other hand, this thread is equipped with
 * a large queue buffer, so that it can remain inactive for long periods of time.
 * 
 * @author Umran
 */
public class WriterThread extends Thread {
	
	private static final int QUEUE_CAPACITY = 3600;

	private WriteQueue queue;
	
	private File outputFile;
	private BufferedWriter writer;
	
	private boolean isRunning;
	
	private final Object waitPendingExit = new Object();
	
	public WriterThread(File outputFile) throws IOException {
		super("WriterThread:"+outputFile.getName());
		this.queue = new WriteQueue(QUEUE_CAPACITY);
		this.outputFile = outputFile;
		this.writer = new BufferedWriter(new FileWriter(outputFile));
		this.isRunning = true;
		this.setPriority(Thread.MIN_PRIORITY);
		this.start();
	}
	
	public WriteQueue getQueue() {
		return queue;
	}
	
	public void quit() {
		this.interrupt();
		isRunning = false;
		
		if (this.isAlive()) {
			// wait for thread to exit
			synchronized (waitPendingExit) {
				try {
					waitPendingExit.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	private void writeToFile(WriteMessage msg) throws IOException {
		this.writer.append(Long.toString(msg.timeStamp));
		for (int i=0; i<msg.msgLen; ++i) {
			this.writer.append(',');
			this.writer.append(Integer.toString(msg.message[i] & 0xFF));
		}
		this.writer.append('\n');
	}
	
	@Override
	public void run() {
		WriteMessage msg = null;
		while (isRunning) {
			try {
				msg = this.queue.takeFilledInstance();
				writeToFile(msg);
				this.queue.returnEmptyInstance(msg);
				msg = null;
//				Log.d(Constants.TAG, "written to file");
			} catch (InterruptedException e) {
				// ignore
			} catch (IOException e) {
				isRunning = false;
				throw new RuntimeException("Error while writting message to file: "+outputFile, e);
			} finally {
				try {
					this.writer.close();
				} catch (IOException e) {
					Log.e(Constants.TAG, "Error while closing file: "+outputFile, e);
				}
				
				if (msg!=null) {
					try {
						this.queue.returnEmptyInstance(msg);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		}

		synchronized (waitPendingExit) {
			waitPendingExit.notify();
		}
	}
	
}
