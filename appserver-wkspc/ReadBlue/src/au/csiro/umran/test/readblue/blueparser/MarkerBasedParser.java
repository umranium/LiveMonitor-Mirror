package au.csiro.umran.test.readblue.blueparser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import android.util.Log;
import au.csiro.umran.test.readblue.ByteUtils;
import au.csiro.umran.test.readblue.Constants;
import au.csiro.umran.test.readblue.DeviceConnection;
import au.csiro.umran.test.readblue.ParsedMsg;
import au.csiro.umran.test.readblue.ReadBlueServiceBinder;
import au.csiro.umran.test.readblue.utils.CircularByteBuffer;
import au.csiro.umran.test.readblue.utils.TwoWayBlockingQueue;

/**
 * 
 * A parser that makes use of a circular buffer, to read bytes from an input stream,
 * searching for a particular series of bytes (i.e. a marker). Initially, the parser
 * gathers input and determines the length of the messages and offset to the first
 * message based on the locations of the markers that appear in the input. 
 * Once the length of the message is determined, the length is used to parse the input
 * stream. 
 * 
 * @author abd01c
 */
public class MarkerBasedParser implements Parser {
	
	private static final int INITIAL_INPUT_BUFFER_SIZE = 1024;
	//	avoid expanding buffers by 1, use larger increments
	private static final int BUFFER_INCREMENTS = 64;
	private static final int RUNNING_NUMBER_LENGTH = 2;
	
	private String tag;
	
	private byte[] marker;
	private ReadBlueServiceBinder binder;
	private DeviceConnection deviceConnection;
	private String deviceName;
	private InputStream inputStream;
	private CircularByteBuffer inputBuffer;
	
	private TwoWayBlockingQueue<ParsedMsg> outputQueue;
	private LengthOffsetFinder lengthOffsetFinder;
	private PacketVerifier runningNumberChecker;
	
	private long latestReadTimestamp;
	
	private boolean firstRun;
	private boolean quit;
	
	public MarkerBasedParser(byte[] marker, ReadBlueServiceBinder binder, DeviceConnection deviceConnection, InputStream inputStream, TwoWayBlockingQueue<ParsedMsg> outputQueue) {
		this.tag = Constants.TAG + "-" + deviceConnection.getConnectableDevice().getDevice().getName().replaceAll("\\W+", "_");
		
		this.marker = marker;
		this.binder = binder;
		this.deviceConnection = deviceConnection;
		this.deviceName = deviceConnection.getConnectableDevice().getDevice().getName().replaceAll("\\s+", "_");
		this.inputStream = inputStream;
		this.inputBuffer = new CircularByteBuffer(INITIAL_INPUT_BUFFER_SIZE, BUFFER_INCREMENTS);
		this.outputQueue = outputQueue;
		this.lengthOffsetFinder = new LengthOffsetFinder(this.inputBuffer, this.marker);
		this.runningNumberChecker = new PacketVerifier(this.marker, RUNNING_NUMBER_LENGTH);
		
		firstRun = true;
		quit = false;
	}
	
	@Override
	public void quit() {
		this.quit = true;
	}
	
	@Override
	public void process() throws IOException
	{
		if (firstRun) {
			firstRun = false;
			resetStream();
		}
		
		readFromStream();
		
		if (inputBuffer.getContentLength()>0) {
			//Log.d(Constants.TAG, "Input Contents:"+bufferContents2Str());
			
			// if offset & length hasn't been confirmed yet..
			if (!lengthOffsetFinder.isMessageLengthOffsetConfirmed()) {
				//Log.d(Constants.TAG, "length and offset not confirmed yet");
				
				//	find offset and location
				lengthOffsetFinder.process();
				
				//	if confirmed, move read location by offset
				if (lengthOffsetFinder.isMessageLengthOffsetConfirmed()) {
					inputBuffer.advanceReadLocation(lengthOffsetFinder.getMessageOffset());
				}
			}
			
			//	if length has been confirmed, parse
			if (lengthOffsetFinder.isMessageLengthOffsetConfirmed()) {
				int msgLen = lengthOffsetFinder.getMessageLength();
				
				try {
					while (inputBuffer.getContentLength()>=msgLen && !quit) {
						ParsedMsg msg = null;
						try {
							msg = outputQueue.takeEmptyInstance();
							if (msgLen>msg.msg.length) {
								throw new IOException("Message bucket (length="+msg.msg.length+") too small to contain parsed message (length="+msgLen+")");
							}
							msg.msgLen = msgLen;
							msg.time = latestReadTimestamp;
							inputBuffer.copyOut(msg.msg, msgLen);
							verifyOutputMessage(msg.msg, msgLen);
							inputBuffer.advanceReadLocation(msgLen);
							outputQueue.returnFilledInstance(msg);
							//Log.d(Constants.TAG, "msg parsed");
							msg = null;
						} catch (InterruptedException e) {
							// ignore
						} finally {
							if (msg!=null) {
								try {
									outputQueue.returnEmptyInstance(msg);
								} catch (InterruptedException e) {
									// ignore
								}
							}
						}
					}
				} catch (PacketVerificationException e) {
					byte[] contents = inputBuffer.copyOut();
					String errmsg = e.getMessage()+"\nInput Buffer Contents: "+ByteUtils.bytesToString(contents, 0, contents.length);
					binder.addMessage(errmsg);
					binder.playSoundAlert();
					Log.w(Constants.TAG, errmsg, e);
					binder.addMessage("Connection from "+this.deviceConnection.getConnectableDevice().getDevice().getName()+" lost.");
					boolean isCalibrating = this.deviceConnection.isCalibrating();
					this.deviceConnection.close();
					binder.addMessage("Attempting to reconnect to "+this.deviceConnection.getConnectableDevice().getDevice().getName());
					this.deviceConnection.connect(true,isCalibrating);
				} 
			}
		}
	}
	
//	private String bufferContents2Str()
//	{
//		return bufferContents2Str(inputBuffer);
//	}
//	
//	private static String bufferContents2Str(CircularByteBuffer inputBuffer)
//	{
//		byte[] v = inputBuffer.copyOut();
//		return inputBuffer.getContentLength()+": "+ByteUtils.bytesToString(v, 0, v.length);
//	}
	
	private void readFromStream() throws IOException {
		//	get number of bytes readable from the stream
		int available = inputStream.available();
		
		if (available==0 && latestReadTimestamp>0 && System.currentTimeMillis()-latestReadTimestamp>3000L) {
			Log.e(Constants.TAG, "Not receiving any data from:" +deviceName);
		}
		
		//	make sure we have enough space to write bytes into the buffer
		if (available>0)
			inputBuffer.ensureWritableSpace(available);
		//	read bytes and write to input stream,
		//		since the buffer is a circular buffer, and we need to write contiguous bytes,
		//		we need to do it in multiple calls to InputStream.read(byte[] buffer, int offset, int length)
		while (available>0 && !quit) {
			int writtenLength = inputStream.read(
					inputBuffer.getArray(),
					inputBuffer.getWriteLocation(),
					Math.min(available, inputBuffer.getContigiousWritableLength())
					);
			if (writtenLength>0) {
				latestReadTimestamp = System.currentTimeMillis();
			}
			inputBuffer.advanceWriteLocation(writtenLength);
			available -= writtenLength;
		}
	}
	
	@Override
	public long getLastestReadTime() {
		return latestReadTimestamp;
	}
	
	private void verifyOutputMessage(byte[] msg, int length) throws PacketVerificationException {
		runningNumberChecker.check(msg, length);
	}
	
	private void resetStream() {
		try {
			while (this.inputStream.available()>0)
				this.inputStream.read();
		} catch (IOException e) {
			Log.e(Constants.TAG, "Error while discarding initial stream contents", e);
		}
		latestReadTimestamp = 0;
	}
	
	private static class LengthOffsetFinder {
		
		private static final int REQ_MARKERS = 10;
		
		private CircularByteBuffer inputBuffer;
		private byte[] marker;
		private ArrayList<Integer> markerIndexes;
		private boolean messageLengthOffsetConfirmed;
		private int messageLength;
		private int messageOffset;
		
		public LengthOffsetFinder(CircularByteBuffer inputBuffer, byte[] marker) {
			this.inputBuffer = inputBuffer;
			this.marker = marker;
			this.markerIndexes = new ArrayList<Integer>(REQ_MARKERS);
			this.messageLengthOffsetConfirmed = false;
		}
		
		void process() {
			//Log.d(Constants.TAG, "marker indexes: "+markerIndexes);
			if (markerIndexes.isEmpty()) {
				while (inputBuffer.getContentLength()>=marker.length && markerIndexes.isEmpty()) {
					//	move to a position where the first byte is the same as that of the marker, or end of contents is reached
					while (inputBuffer.getContentLength()>0 && inputBuffer.get(0)!=marker[0]) {
						inputBuffer.advanceReadLocation(1);
					}
					
					//	if we have enough content to check for one marker (also meaning that our first byte is the same as that of the marker)
					if (inputBuffer.getContentLength()>=marker.length) {
						if (isMarkerAt(0)) {
							// we've found the first marker
							markerIndexes.add(0);
						} else {
							// otherwise, first byte wasn't really first byte of marker, throw away
							inputBuffer.advanceReadLocation(1);
						}
					}
				}
			} else {
				//	the index of the byte to start checking for markers,
				//		given by the index of the first byte after the last marker 
				int markerByteCheckStart = markerIndexes.get(markerIndexes.size()-1)+marker.length-1;
				int available = inputBuffer.getContentLength(); 
				
				for (int i=markerByteCheckStart; i<(available-marker.length+1); ++i) {
					if (inputBuffer.get(i)==marker[0] && isMarkerAt(i)) {
						markerIndexes.add(i);
						i += marker.length - 1;
					}
				}
				
				if (markerIndexes.size()>=REQ_MARKERS) {
					determineMsgLenOffset();
				}
			}
		}
		
		public boolean isMessageLengthOffsetConfirmed() {
			return messageLengthOffsetConfirmed;
		}
		
		public int getMessageLength() {
			return messageLength;
		}
		
		public int getMessageOffset() {
			return messageOffset;
		}
		
		/**
		 * Checks whether the bytes found at the location inputBufferStart of the input buffer,
		 * match those of the input buffer.
		 * Note:
		 * 1) First byte is assumed to have already been checked, hence isn't checked
		 * 2) No check for whether there are enough bytes in the input buffer for a full check of the marker
		 * 		is done. This is assumed to have been already done.
		 */
		boolean isMarkerAt(int inputBufferStart)
		{
			boolean isMarker = true;
			for (int j=1; j<marker.length; ++j) {
				if (inputBuffer.get(inputBufferStart+j)!=marker[j]) {
					isMarker = false;
					break;
				}
			}
			return isMarker;
		}
		
	    void determineMsgLenOffset() {
	        //Log.d(Constants.TAG, "Determining Msg Length & Offset");

	        int lastMarkerLoc = markerIndexes.get(markerIndexes.size() - 1);
	        boolean setupChecked[][] = new boolean[lastMarkerLoc + 1][lastMarkerLoc];
	        HashSet<Integer> markerLocations = new HashSet<Integer>(markerIndexes);

	        ArrayList<OffsetLengthDetails> details = new ArrayList<OffsetLengthDetails>(markerIndexes.size());

	        for (int firstIndex = 0; firstIndex < markerIndexes.size() - 2; ++firstIndex) {
	            for (int secondIndex = firstIndex + 1; secondIndex < markerIndexes.size() - 1; ++secondIndex) {
	                int firstItem = markerIndexes.get(firstIndex);
	                int secondItem = markerIndexes.get(secondIndex);
	                int dist = secondItem - firstItem;

	                if (!setupChecked[firstItem][dist]) {
	                    setupChecked[secondItem][dist] = true;

	                    int count = 0;
	                    for (int loc = secondItem + dist; loc < lastMarkerLoc; loc+=dist) {
	                        setupChecked[loc][dist] = true;
	                        
	                        if (markerLocations.contains(loc)) {
	                            //	found another marker (good!!!)
	                            ++count;
	                        } else {
	                            //	found a missing location (means this isn't a valid setup)
	                            count = -1;
	                            break;
	                        }
	                    }

	                    if (count > 0) {
	                        details.add(new OffsetLengthDetails(firstItem, dist, count));
	                    }
	                }
	            }
	        }

	        if (!details.isEmpty()) {
	            Collections.sort(details);

	            //Log.d(Constants.TAG, details.toString());

	            if ((details.size() == 1 && details.get(0).count >= REQ_MARKERS)
	                    || details.get(0).count - details.get(1).count >= REQ_MARKERS) {
	                messageLengthOffsetConfirmed = true;
	                messageOffset = details.get(0).offset;
	                messageLength = details.get(0).length;
	            }
	        }
	    }
		
	}
	
	private static class OffsetLengthDetails implements Comparable<OffsetLengthDetails> {
		int offset;
		int length;
		int count;
		
		public OffsetLengthDetails(int offset, int length, int count) {
			this.offset = offset;
			this.length = length;
			this.count = count;
		}

		@Override
		public int compareTo(OffsetLengthDetails another) {
			return -(this.count-another.count);	// sort, larger first
		}
		
		@Override
		public String toString() {
			return "{off:"+offset+", len="+length+", count="+count+"}";
		}
	}
	

}
