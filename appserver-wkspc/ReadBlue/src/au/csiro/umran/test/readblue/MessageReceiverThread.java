package au.csiro.umran.test.readblue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.text.InputFilter.AllCaps;
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
	
	private String deviceName;
	private DeviceConnection connection;
	private TwoWayBlockingQueue<ParsedMsg> queue;
	private ReadBlueServiceBinder binder;
	private Writer writerThread;
	private boolean doCalibration;
	private FrequencyDeterminer frequencyDeterminer;
	private Calibrator calibrator;
	private String marker;
	
	public MessageReceiverThread(
			Context context, 
			DeviceConnection connection, 
			TwoWayBlockingQueue<ParsedMsg> queue,
			ReadBlueServiceBinder binder, 
			Writer writerThread, 
			boolean doCalibration, 
			String marker)
	{
		super(context, "MessageReceiverThread:"+connection.getConnectableDevice().getDevice().getName());
		this.deviceName = connection.getConnectableDevice().getDevice().getName().replaceAll("\\W+", "_");
		this.connection = connection;
		this.queue = queue;
		this.binder = binder;
		this.writerThread = writerThread;
		this.doCalibration = doCalibration;
		if (this.doCalibration)
			this.frequencyDeterminer = new FrequencyDeterminer();
		else
			this.frequencyDeterminer = null;
		this.calibrator = null;
		this.marker = marker;
		this.setPriority(MIN_PRIORITY);
		this.start();
	}
	
	public String getMarker() {
		return marker;
	}
	
	public void setMarker(String marker) {
		this.marker = marker;
	}
	
	@Override
	public void doAction() {
		if (this.queue.peekFilledInstance()!=null) {
			ParsedMsg parsedMsg = null;
			try {
				parsedMsg = this.queue.takeFilledInstance();
				
//				binder.addMessage(ByteUtils.bytesToString(parsedMsg.msg, 0, parsedMsg.msgLen));
//				Log.d(Constants.TAG+"_"+deviceName, ByteUtils.bytesToString(parsedMsg.msg, 0, parsedMsg.msgLen)+","+marker);
				if (doCalibration) {
					if (!frequencyDeterminer.isFreqDetermined()) {
						frequencyDeterminer.feed(parsedMsg);
						
						if (frequencyDeterminer.isFreqDetermined()) {
							calibrator = new Calibrator(9, 4, 5, 6, frequencyDeterminer.getPickedFrequency());
						}
					}
					
					if (calibrator!=null) {
						calibrator.feed(parsedMsg);
					}
				} else {
					writerThread.writeToFile(parsedMsg, marker);
				}
				
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
	
	public class FrequencyDeterminer {
		
		boolean freqDetermined;
		long firstMsgTime;
		int numOfMsgs;
		double calculatedFrequency;
		int pickedFrequency;
		
		public FrequencyDeterminer() {
			this.freqDetermined = false;
			this.firstMsgTime = 0;
			this.numOfMsgs = 0;
			this.calculatedFrequency = 0.0;
			this.pickedFrequency = 0;
		}
		
		public boolean isFreqDetermined() {
			return freqDetermined;
		}
		
		public double getCalculatedFrequency() {
			return calculatedFrequency;
		}
		
		public int getPickedFrequency() {
			return pickedFrequency;
		}
		
		public void feed(ParsedMsg msg) {
			if (freqDetermined)
				return;
			
			if (firstMsgTime==0) {
				firstMsgTime = msg.time;
			}
			
			++numOfMsgs;
			
			long duration = msg.time-firstMsgTime;
			if (duration>=Constants.MIN_TIME_FOR_FREQ_DET) {
				calculatedFrequency = numOfMsgs * 1000.0 / (double)duration;
				Log.i(Constants.TAG, "calc. freq. = "+calculatedFrequency+", numOfMsgs="+numOfMsgs+", duration="+duration);
				
				for (int freq:Constants.POSIBLE_DEVICE_FREQUENCIES) {
					double diff = Math.abs(calculatedFrequency - freq);
					if (diff<25.0) {
						pickedFrequency = freq;
						break;
					}
				}
				
				if (pickedFrequency!=0) {
					freqDetermined = true;
					binder.addMessage(connection.getConnectableDevice().getDevice().getName()+" calc freq="+calculatedFrequency+", actual freq="+pickedFrequency);
				}
			}
		}
	}
	
	public class Side {
		boolean calculated;
		int meanGravValue;
		int sumGravValue;
		int countSamples;
	}
	
	public static final int NUM_AXIS = 3;
	public static final int AXIS_X = 0;
	public static final int AXIS_Y = 1;
	public static final int AXIS_Z = 2;
	
	
	public class Data {
		long time;
		byte[] values;
		
		public Data(long time, byte x, byte y, byte z) {
			super();
			this.time = time;
			this.values = new byte[NUM_AXIS];
			this.values[AXIS_X] = x;
			this.values[AXIS_Y] = y;
			this.values[AXIS_Z] = z;
		}
	}
	
	public class Calibrator {
		
		int msgLen;
		final int[] sourceAxisIndexes = new int[NUM_AXIS];
		int windowLength;
		ArrayDeque<Data> sensorData;
		boolean error = false;
		boolean previouslyStationary = false;
		final double[] maxSumCount = new double[NUM_AXIS];
		final String[] descrAtMaxModeCount = new String[NUM_AXIS];
		
		final CalcModeUtil axisStats[] = new CalcModeUtil[] {
				new CalcModeUtil(),
				new CalcModeUtil(),
				new CalcModeUtil()
		};
		
		public Calibrator(int msgLen, int xIndex, int yIndex, int zIndex, int sensorFrequency) {
			this.msgLen = msgLen;
			sourceAxisIndexes[AXIS_X] = xIndex;
			sourceAxisIndexes[AXIS_Y] = yIndex;
			sourceAxisIndexes[AXIS_Z] = zIndex;
			this.windowLength = sensorFrequency*Constants.DURATION_CALIBARATION/1000;
			sensorData = new ArrayDeque<MessageReceiverThread.Data>(this.windowLength+1);
			connection.setMarker("Calibrating");
		}
		
		public boolean hasError() {
			return error;
		}
		
		public void feed(ParsedMsg msg) {
			if (msg.msgLen!=msgLen) {
				String errmsg = "Unexpected message length for calibrator. Got:"+msg.msgLen+", expected:"+msgLen;
				Log.e(Constants.TAG, errmsg);
				binder.addMessage(errmsg);
				error = true;
				return;
			}
			
			Data newDt = new Data(msg.time, 
					msg.msg[sourceAxisIndexes[AXIS_X]],
					msg.msg[sourceAxisIndexes[AXIS_Y]],
					msg.msg[sourceAxisIndexes[AXIS_Z]]
					);
			sensorData.addLast(newDt);
			
			for (int axis=0; axis<NUM_AXIS; ++axis) {
				this.axisStats[axis].include(newDt.values[axis]);
			}
			
			while (sensorData.size()>windowLength) {
				Data oldDt = sensorData.removeFirst();
				
				for (int axis=0; axis<NUM_AXIS; ++axis) {
					this.axisStats[axis].exclude(oldDt.values[axis]);
				}
			}
			
			boolean isStationary = false;
			
			if (sensorData.size()==windowLength) {
				for (int axis=0; axis<NUM_AXIS; ++axis) {
					this.axisStats[axis].computeMode();
				}
				
				boolean allStationary = true;
				String descr = "";
				for (int axis=0; axis<NUM_AXIS; ++axis) {
					descr += " "+axis+":"+this.axisStats[axis].description;
					if (this.axisStats[axis].sumCounts<windowLength*0.95) {
						allStationary = false;
						break;
					}
				}
				
				if (allStationary) {
					isStationary = true;
				}

//				Log.d(Constants.TAG+"_"+deviceName, (isStationary?"S:":"X:")+descr);
			}
			
			if (isStationary && !previouslyStationary) {
				binder.vibrate();
				for (int axis=0; axis<NUM_AXIS; ++axis) {
					maxSumCount[axis] = Integer.MIN_VALUE;
					descrAtMaxModeCount[axis] = "";
				}
				connection.setMarker("Stationary");
			} else
				if (previouslyStationary && !isStationary) {
					binder.vibrate();
					String imsg = "";
					for (int axis=0; axis<NUM_AXIS; ++axis) {
						if (axis!=0) {
							imsg += ",";
						}
						imsg += this.descrAtMaxModeCount[axis];
					}
					Log.i(Constants.TAG, "Stationary period found: "+imsg);
					try {
						writerThread.writeToFile(imsg,marker);
					} catch (IOException e) {
						Log.d(Constants.TAG, "Error while writing stationary period data to file", e);
						binder.addMessage(e.getMessage());
					}
					connection.setMarker("Not Stationary");
					
				}
			
			if (isStationary) {
				for (int axis=0; axis<NUM_AXIS; ++axis) {
					if (this.axisStats[axis].sumCounts>maxSumCount[axis]) {
						maxSumCount[axis] = this.axisStats[axis].sumCounts;
						descrAtMaxModeCount[axis] = this.axisStats[axis].description;
					}
				}
			}
			
			previouslyStationary = isStationary;
		}
	}
	
//	public class CalcMeanSdUtil {
//		double sum;
//		double sumSqr;
//		int count;
//		double mean;
//		double sd;
//		String description;
//		
//		public CalcMeanSdUtil() {
//			reset();
//		}
//		
//		public void reset() {
//			count = 0;
//			sum = 0;
//			sumSqr = 0;
//		}
//		
//		public void include(byte value) {
//			int currentVal = ByteUtils.unsignedByteToInt(value);
//			++count;
//			sum += currentVal;
//			sumSqr += currentVal*currentVal;
//		}
//		
//		public void exclude(byte value) {
//			int currentVal = ByteUtils.unsignedByteToInt(value);
//			--count;
//			sum -= currentVal;
//			sumSqr -= currentVal*currentVal;
//		}
//		
//		public void computeMeanSd() {
//			mean = sum / count;
//			sd = sumSqr / count - mean*mean;
//			description = String.format("%.2f(%.2f)", mean, sd);
//		}
//		
//		public double getMean() {
//			return mean;
//		}
//		
//		public double getSd() {
//			return sd;
//		}
//		
//	}
	
	public class ModeContainer {
		int value;
		int count;
		
		public ModeContainer(int value) {
			this(value, 0);
		}
		
		public ModeContainer(int value, int count) {
			this.value = value;
			this.count = count;
		}
	}
	
	public final Comparator<ModeContainer> modeContainerDescComparator = new Comparator<ModeContainer>() {

		@Override
		public int compare(ModeContainer lhs, ModeContainer rhs) {
			return -(lhs.count-rhs.count);
		}
		
	};
	
	public class CalcModeUtil {
		HashMap<Integer,ModeContainer> valueMap;
		List<ModeContainer> containers;
		double mode;
		int sumCounts;
		String description;
		
		public CalcModeUtil() {
			reset();
		}
		
		public void reset() {
			this.mode = 0;
			this.valueMap = new HashMap<Integer, ModeContainer>(256);
			this.containers = new ArrayList<MessageReceiverThread.ModeContainer>(256);
		}
		
		public void include(byte value) {
			int currentVal = ByteUtils.unsignedByteToInt(value);
			ModeContainer container = get(currentVal);
			++container.count;
		}
		
		public void exclude(byte value) {
			int currentVal = ByteUtils.unsignedByteToInt(value);
			if (valueMap.containsKey(currentVal)) {
				ModeContainer container = get(currentVal);
				--container.count;
			}
		}
		
		private ModeContainer get(int value) {
			//int currentVal = ByteUtils.unsignedByteToInt(value);
			if (valueMap.containsKey(value))
				return valueMap.get(value);
			else {
				ModeContainer container = new ModeContainer(value);
				valueMap.put(value, container);
				containers.add(container);
				return container;
			}
		}
		
		public void computeMode() {
			Collections.sort(containers, modeContainerDescComparator);
			
			Integer prevValue = null;
			int itemCount = 0;
			int sumCount = 0;
			double sumMode = 0;
			String description = "";
			boolean donePicking = false;
			for (ModeContainer cont:containers)
				if (cont.count>0) {
					int currentVal = cont.value;//ByteUtils.unsignedByteToInt(cont.value);
					if (prevValue==null) {
						prevValue = currentVal;
					} else {
						if (Math.abs(currentVal-prevValue)>1) {
							donePicking = true;
						}
					}
					
					++itemCount;
					if (itemCount>2) {
						donePicking = true;
					}
					
					if (!donePicking) {
						sumCount += cont.count;
						sumMode += currentVal*cont.count;
					}
					
					if (!description.isEmpty())
						description += ",";
					
					description += currentVal+"("+cont.count+")"+(donePicking?"":"*");
				}
			
			this.sumCounts = sumCount;
			this.mode = sumMode / sumCount;
			this.description = this.mode+",\""+description+"\"";
		}
		
	}
	
}