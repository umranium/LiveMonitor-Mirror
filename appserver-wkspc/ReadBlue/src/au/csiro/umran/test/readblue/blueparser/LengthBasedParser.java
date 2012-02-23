package au.csiro.umran.test.readblue.blueparser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import android.util.Log;
import au.csiro.umran.test.readblue.ByteUtils;
import au.csiro.umran.test.readblue.Constants;
import au.csiro.umran.test.readblue.DeviceConnection;

/**
 * 
 * A parser that makes use of a circular buffer, to read bytes from an input stream,
 * searching for a particular series of bytes (i.e. a marker).
 * 
 * @author abd01c
 */
public class LengthBasedParser implements Parser {
	
	//	avoid expanding buffers by 1, use larger increments
	private static final int BUFFER_INCREMENTS = 64;
	
	private byte[] marker;
	private int messageLength;
	private DeviceConnection deviceConnection;
	private String deviceName;
	private InputStream inputStream;
	private int inputBufferSize;
	private byte[] inputBuffer;
	private int inputWriteLocation;
	private int inputReadLocation;
	private int dataBytesInInputBuffer = 0;
	
	private int outputBufferSize;
	private byte[] outputBuffer;
	private int outputWriteLocation;
	
//	private boolean includeMarkerInOutput;
	private OnMessageListener onMessageListener;
	
	private boolean firstMarkerFound;
	private boolean priorMarkerHasBeenFound;
	
	private long latestReadTimestamp;
	
	private boolean quit;
	
	public LengthBasedParser(byte[] marker, int messageLength, DeviceConnection deviceConnection, InputStream inputStream, int inputBufferSize, int outputBufferSize, boolean includeMarkerInOutput, OnMessageListener onMessageListener) {
		this.marker = marker;
		this.messageLength = messageLength;
		this.deviceConnection = deviceConnection;
		this.deviceName = deviceConnection.getConnectableDevice().getDevice().getName().replaceAll("\\s+", "_");
		this.inputStream = inputStream;
		this.inputBufferSize = inputBufferSize;
		this.outputBufferSize = Math.max(outputBufferSize, messageLength);
		this.inputBuffer = new byte[this.inputBufferSize];
		this.outputBuffer = new byte[this.outputBufferSize];
	
		this.inputReadLocation = 0;
		this.inputWriteLocation = 0;
		this.outputWriteLocation = 0;
		this.firstMarkerFound = false;
		this.priorMarkerHasBeenFound = false;

//		this.includeMarkerInOutput = includeMarkerInOutput;
		this.onMessageListener = onMessageListener;
//		if (includeMarkerInOutput) {
//			for (int i=0; i<marker.length; ++i)
//				outputBuffer[i] = marker[i];
//			this.outputWriteLocation = this.marker.length;
//		}
		
		try {
			while (this.inputStream.available()>0)
				this.inputStream.read();
		} catch (IOException e) {
			Log.e(Constants.TAG, "Error while discarding initial stream contents", e);
		}
		latestReadTimestamp = 0;
		dataBytesInInputBuffer = 0;
		quit = false;
	}
	
	/* (non-Javadoc)
	 * @see au.csiro.umran.test.readblue.blueparser.Parser#quit()
	 */
	@Override
	public void quit() {
		this.quit = true;
	}
	
	/* (non-Javadoc)
	 * @see au.csiro.umran.test.readblue.blueparser.Parser#process()
	 */
	@Override
	public void process() throws IOException
	{
		if (dataBytesInInputBuffer==inputBufferSize) {
			expandInputBuffer(inputBufferSize+BUFFER_INCREMENTS);
		}
		
		do {
			int maxLen; // maximum length that can be written at this moment
			if (inputReadLocation>inputWriteLocation) {
				//	actual empty bytes = inputReadLocation - inputWriteLocation
				//	but we don't want to reach a point where inputWriteLocation==inputReadLocation
				maxLen = inputReadLocation - inputWriteLocation - 1;
				
//				if (maxLen==0) {
					//	Actually, this case means that
					//	we've filled the buffer, without finding the next
					//	marker. Hence, we either need to expand the input buffer
					//	or we need to give up and say that the input isn't
					//	the message we are expecting, or a combination of this.
//					expandInputBuffer(inputBufferSize+BUFFER_INCREMENTS);
//				}
				
			} else {
				//	we can only write till the end of the buffer
				//	the consequent write (i.e. from zero to the read-location) will have to occur  
				maxLen = inputBufferSize - inputWriteLocation;
			}
			
			int readCount = 0;
			if (inputStream.available()>0 /*|| dataBytesInInputBuffer<messageLength*/)
			{
				try {
//					Log.d(Constants.TAG, "writeLoc="+inputWriteLocation+" readLoc="+inputReadLocation+" len="+maxLen+" bufferLen="+inputBuffer.length);
					readCount = inputStream.read(inputBuffer, inputWriteLocation, maxLen);
				} catch (IOException e) {
					Log.e(Constants.TAG, "Error while reading from device input", e);
				}
			}
			if (readCount>0) {
				latestReadTimestamp = System.currentTimeMillis();
//				Log.d(deviceName, "Input: "+ByteUtils.bytesToString(inputBuffer, inputWriteLocation, readCount));
			} else
				if (readCount<0) {
					throw new IOException("End of stream reached!");
				}
			dataBytesInInputBuffer += readCount;
			inputWriteLocation += readCount;
			if (inputWriteLocation>=inputBufferSize)
				inputWriteLocation -= inputBufferSize;
			
		} while (!quit && dataBytesInInputBuffer<messageLength);
		
		if (firstMarkerFound) {
//			Log.d(Constants.TAG, "About to check and output data: len="+dataBytesInInputBuffer);
			while (dataBytesInInputBuffer>=messageLength) {
//				Log.d(Constants.TAG, "Checking for output data: len="+dataBytesInInputBuffer);
				outputWriteLocation = 0;
				for (int i=0; i<messageLength; ++i) {
					outputBuffer[outputWriteLocation] = inputBuffer[inputReadLocation];
					++outputWriteLocation;
					
					--dataBytesInInputBuffer;
					++inputReadLocation;
					if (inputReadLocation>=inputBufferSize)
						inputReadLocation -= inputBufferSize;
				}
				
//				Log.d(deviceName, "Output: "+ByteUtils.bytesToString(outputBuffer, 0, outputWriteLocation));
				onMessageListener.onMessage(latestReadTimestamp, outputBuffer, outputWriteLocation);
			}
		} else {
			//	this byte is the start of a marker
			boolean markerFound = false;
			// special condition, when the marker appears to be found, but is incomplete
			boolean markerFoundIncomplete = false;
			
			if (inputBuffer[inputReadLocation]==marker[0]) {
				markerFound = true;
				int readAheadLoc = inputReadLocation;
				for (int i=1; i<marker.length; ++i) {
					++readAheadLoc;
					if (readAheadLoc>=inputBufferSize)
						readAheadLoc -= inputBufferSize;
					
					if (readAheadLoc==inputWriteLocation) {
						//	we've reached the end of the written bytes
						//	but still haven't completed the marker
						markerFoundIncomplete = true;
						break;
					}
					
					//	this byte doesn't correspond to the marker byte
					if (inputBuffer[readAheadLoc]!=marker[i]) {
						markerFound = false;
						break;
					}
				}
			}
			
			if (!markerFoundIncomplete) {
				if (markerFound) {
					//	complete marker was found, use this location to extract record
					firstMarkerFound = true;
				} else {
					// no marker was found at this location, move forward by one
					--dataBytesInInputBuffer;
					++inputReadLocation;
					if (inputReadLocation>=inputBufferSize)
						inputReadLocation -= inputBufferSize;
				}
			}
		}
		
	}
	
	@Override
	public long getLastestReadTime() {
		return latestReadTimestamp;
	}
	
	//	since the output buffer is not a circular buffer,
	//		expanding it is quite straight forward
	//		just make a new copy of the old, with extra spaces
	private void expandOutputBuffer(int proposedNewSize)
	{
		int newSize = ((proposedNewSize/BUFFER_INCREMENTS)+1)*BUFFER_INCREMENTS;
		byte[] newBuffer = new byte[newSize];
		for (int i=0; i<outputWriteLocation; ++i)
			newBuffer[i] = outputBuffer[i];
		outputBuffer = newBuffer;
		outputBufferSize = newSize;
	}
	
	//	since the input buffer is a circular buffer,
	//		expanding it, isn't so straight forward.
	//		to simplify, we assume that the buffer will be
	//		expanding during a write operation, not a read operation.
	//		hence, the write location, needs to remain unchanged.
	private void expandInputBuffer(int proposedNewSize)
	{
		int newSize = ((proposedNewSize/BUFFER_INCREMENTS)+1)*BUFFER_INCREMENTS;
		byte[] newBuffer = new byte[newSize];
		if (inputReadLocation<=inputWriteLocation) {
			//	simple case, insertion point is after the read location, just add extra bytes at the end
			for (int i=inputReadLocation; i<inputWriteLocation; ++i) {
				newBuffer[i] = inputBuffer[i];
			}
			this.inputBuffer = newBuffer;
			this.inputBufferSize = newSize;
		} else {
			//	insertion point, before the read location
			int newReadLocation = newSize - (inputBufferSize - inputReadLocation);
			for (int i=0; i<inputWriteLocation; ++i) {
				newBuffer[i] = inputBuffer[i];
			}
			for (int i=inputReadLocation, j=newReadLocation; i<inputBufferSize; ++i, ++j) {
				newBuffer[j] = inputBuffer[i];
			}
			this.inputBuffer = newBuffer;
			this.inputBufferSize = newSize;
			this.inputReadLocation = newReadLocation;
		}
	}

}
