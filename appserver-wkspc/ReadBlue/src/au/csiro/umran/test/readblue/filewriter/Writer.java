package au.csiro.umran.test.readblue.filewriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;
import au.csiro.umran.test.readblue.Constants;
import au.csiro.umran.test.readblue.DeviceConnection;
import au.csiro.umran.test.readblue.ParsedMsg;
import au.csiro.umran.test.readblue.ParsedMsgQueue;
import au.csiro.umran.test.readblue.QuitableThread;

public class Writer {

	private DeviceConnection connection;
	private File outputFile;
	private BufferedWriter writer;
	
	public Writer(DeviceConnection connection, File outputFile) throws IOException {
		this.connection = connection;
		this.outputFile = outputFile;
		this.writer = new BufferedWriter(new FileWriter(outputFile));
	}


	public void close() {
		try {
			this.writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Error while closing file: "+outputFile, e);
		}
	}
	
	public void writeToFile(ParsedMsg msg) throws IOException {
		this.writer.append(Long.toString(msg.time));
		for (int i=0; i<msg.msgLen; ++i) {
			this.writer.append(',');
			this.writer.append(Integer.toString(msg.msg[i] & 0xFF));
		}
		this.writer.append('\n');
	}
	
}
