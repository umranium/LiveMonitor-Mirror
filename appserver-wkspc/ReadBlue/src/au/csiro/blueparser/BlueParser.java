package au.csiro.blueparser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 
 * A parser that makes use of a circular buffer, to read bytes from an input stream,
 * searching for a particular series of bytes (i.e. a marker).
 * 
 * @author abd01c
 */
public class BlueParser {
	
	//	avoid expanding buffers by 1, use larger increments
	private static final int BUFFER_INCREMENTS = 64;
	
	private byte[] marker;
	private InputStream inputStream;
	private int inputBufferSize;
	private byte[] inputBuffer;
	private int inputWriteLocation;
	private int inputReadLocation;
	
	private int outputBufferSize;
	private byte[] outputBuffer;
	private int outputWriteLocation;
	
	private boolean includeMarkerInOutput;
	private OnMessageListener onMessageListener;
	
	private boolean priorMarkerHasBeenFound;
	
	public BlueParser(byte[] marker, InputStream inputStream, int inputBufferSize, int outputBufferSize, boolean includeMarkerInOutput, OnMessageListener onMessageListener) {
		this.marker = marker;
		this.inputStream = inputStream;
		this.inputBufferSize = inputBufferSize;
		this.outputBufferSize = outputBufferSize;
		this.inputBuffer = new byte[this.inputBufferSize];
		this.outputBuffer = new byte[this.outputBufferSize];
	
		this.inputReadLocation = 0;
		this.inputWriteLocation = 0;
		this.outputWriteLocation = 0;
		this.priorMarkerHasBeenFound = false;

		this.includeMarkerInOutput = includeMarkerInOutput;
		this.onMessageListener = onMessageListener;
		if (includeMarkerInOutput) {
			for (int i=0; i<marker.length; ++i)
				outputBuffer[i] = marker[i];
			this.outputWriteLocation = this.marker.length;
		}
	}
	
	public void process() throws IOException
	{
		int totalRead = 0;
		while (totalRead<marker.length) {
			int maxLen; 
			if (inputReadLocation>inputWriteLocation) {
				//	actual empty bytes = inputReadLocation - inputWriteLocation
				//	but we don't want to reach a point where inputWriteLocation==inputReadLocation
				maxLen = inputReadLocation - inputWriteLocation - 1;
				if (maxLen==0) {
					//	Actually, this case means that
					//	we've filled the buffer, without finding the next
					//	marker. Hence, we either need to expand the input buffer
					//	or we need to give up and say that the input isn't
					//	the message we are expecting, or a combination of this.
					expandInputBuffer(inputBufferSize+BUFFER_INCREMENTS);
				}
			} else {
				maxLen = inputBufferSize - inputWriteLocation;
			}
			
			int readCount = inputStream.read(inputBuffer, inputWriteLocation, maxLen);
			totalRead += readCount;
			inputWriteLocation += readCount;
			if (inputWriteLocation>=inputBufferSize)
				inputWriteLocation -= inputBufferSize;
		}
		
		while (inputWriteLocation!=inputReadLocation) {
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
				
				if (markerFoundIncomplete) {
					//	don't go any further until when we get more bytes
					break;
				}
			}
			
			if (markerFound) {
				//	check if this msg, was started with a marker
				if (priorMarkerHasBeenFound) {
					onMessageListener.onMessage(outputBuffer, outputWriteLocation);
				}
				if (includeMarkerInOutput)
					outputWriteLocation = marker.length;
				else
					outputWriteLocation = 0;
				inputReadLocation += marker.length;
				if (inputReadLocation>=inputBufferSize)
					inputReadLocation -= inputBufferSize;
				priorMarkerHasBeenFound = true;
			}
			else {
				if (priorMarkerHasBeenFound) {
					outputBuffer[outputWriteLocation] = inputBuffer[inputReadLocation];
					++outputWriteLocation;
					if (outputWriteLocation>=outputBufferSize) {
						//	need to expand output buffer
						expandOutputBuffer(outputWriteLocation);
					}
				}
				++inputReadLocation;
				if (inputReadLocation>=inputBufferSize)
					inputReadLocation -= inputBufferSize;
			}
		}
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
