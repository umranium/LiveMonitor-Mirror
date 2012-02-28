package au.csiro.umran.test.readblue.filewriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;
import au.csiro.umran.test.readblue.Constants;
import au.csiro.umran.test.readblue.DeviceConnection;
import au.csiro.umran.test.readblue.QuitableThread;

public class WriterThread extends QuitableThread {

	private static final int QUEUE_CAPACITY = 3600;

	private WriteQueue queue;
	
	private DeviceConnection connection;
	private File outputFile;
	private BufferedWriter writer;
	
	public WriterThread(DeviceConnection connection, File outputFile) throws IOException {
		super("WriterThread:"+outputFile.getName());
		this.connection = connection;
		this.queue = new WriteQueue(QUEUE_CAPACITY);
		this.outputFile = outputFile;
		this.writer = new BufferedWriter(new FileWriter(outputFile));
		this.setPriority(Thread.MIN_PRIORITY);
		this.start();
	}

	@Override
	public void doQuit() {
	}

	@Override
	public void doAction() {
		WriteMessage msg = null;
		try {
			msg = this.queue.takeFilledInstance();
			writeToFile(msg);
			this.queue.returnEmptyInstance(msg);
			msg = null;
//			Log.d(Constants.TAG, "written to file");
		} catch (InterruptedException e) {
			// ignore
		} catch (IOException e) {
			//throw new RuntimeException("Error while writting message to file: "+outputFile, e);
			Log.e(Constants.TAG, "Error while writing data to file: "+outputFile, e);
			quit();
			connection.close();
		} finally {
			if (msg!=null) {
				try {
					this.queue.returnEmptyInstance(msg);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	@Override
	public void doFinalize() {
		try {
			this.writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Error while closing file: "+outputFile, e);
		}
	}
	
	public WriteQueue getQueue() {
		return queue;
	}

	private void writeToFile(WriteMessage msg) throws IOException {
		this.writer.append(Long.toString(msg.timeStamp));
		for (int i=0; i<msg.msgLen; ++i) {
			this.writer.append(',');
			this.writer.append(Integer.toString(msg.message[i] & 0xFF));
		}
		this.writer.append('\n');
	}
	
}
