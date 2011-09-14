/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stepcounter;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.JPanel;

/**
 *
 * @author abd01c
 */
public class StepCounter {
	
	public final static String PATH_SD_CARD_APP_LOC = "-";
	public final static SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z");
	
	private final static String[] INPUT_FILE_NAMES = {
//			"2011_07_05_15_44_01_1000_GMT_10_00.txt",
//			"2011_07_05_15_44_31_1000_GMT_10_00.txt",
		
//			"2011_07_06_09_25_23_1000_GMT_10_00.txt",
//			"2011_07_06_09_25_53_1000_GMT_10_00.txt",
//			"2011_07_06_09_26_23_1000_GMT_10_00.txt",
//			"2011_07_06_09_26_53_1000_GMT_10_00.txt",
//			"2011_07_06_09_27_23_1000_GMT_10_00.txt",
//			"2011_07_06_09_27_54_1000_GMT_10_00.txt",
//			"2011_07_06_09_28_24_1000_GMT_10_00.txt",
//			"2011_07_06_09_28_54_1000_GMT_10_00.txt",
//			"2011_07_06_09_29_24_1000_GMT_10_00.txt",
//			"2011_07_06_09_29_54_1000_GMT_10_00.txt",
//			"2011_07_06_09_30_24_1000_GMT_10_00.txt",
//			"2011_07_06_09_30_54_1000_GMT_10_00.txt",
			
			"2011_07_06_09_31_24_1000_GMT_10_00.txt",
			
//			"2011_07_06_09_31_54_1000_GMT_10_00.txt",
//			"2011_07_06_09_32_25_1000_GMT_10_00.txt",
//			"2011_07_06_09_33_55_1000_GMT_10_00.txt",
//			"2011_07_06_09_34_25_1000_GMT_10_00.txt",
//			"2011_07_06_09_34_55_1000_GMT_10_00.txt",
//			"2011_07_06_09_35_25_1000_GMT_10_00.txt",
//			"2011_07_06_09_35_55_1000_GMT_10_00.txt",
//			"2011_07_06_09_36_25_1000_GMT_10_00.txt",
//			"2011_07_06_09_36_55_1000_GMT_10_00.txt",
//			"2011_07_06_09_37_26_1000_GMT_10_00.txt",
//			"2011_07_06_09_37_56_1000_GMT_10_00.txt",
//			"2011_07_06_09_38_26_1000_GMT_10_00.txt",
//			"2011_07_06_09_38_56_1000_GMT_10_00.txt",
//			"2011_07_06_09_39_26_1000_GMT_10_00.txt",
//			"2011_07_06_09_39_56_1000_GMT_10_00.txt",
//			"2011_07_06_09_40_26_1000_GMT_10_00.txt",
//			"2011_07_06_09_40_56_1000_GMT_10_00.txt",
//			"2011_07_06_09_41_26_1000_GMT_10_00.txt",
//			"2011_07_06_09_41_57_1000_GMT_10_00.txt",
//			"2011_07_06_09_42_27_1000_GMT_10_00.txt",
//			"2011_07_06_09_42_57_1000_GMT_10_00.txt",
//			"2011_07_06_09_43_27_1000_GMT_10_00.txt",
//			"2011_07_06_09_43_57_1000_GMT_10_00.txt",
//			"2011_07_06_09_44_27_1000_GMT_10_00.txt",
//			"2011_07_06_09_44_57_1000_GMT_10_00.txt",
//			"2011_07_06_09_45_27_1000_GMT_10_00.txt",
//			"2011_07_06_09_45_57_1000_GMT_10_00.txt",
//			"2011_07_06_09_46_27_1000_GMT_10_00.txt",
//			"2011_07_06_09_46_58_1000_GMT_10_00.txt",
//			"2011_07_06_09_47_28_1000_GMT_10_00.txt",
//			"2011_07_06_09_47_58_1000_GMT_10_00.txt",
//			"2011_07_06_09_48_28_1000_GMT_10_00.txt",
//			"2011_07_06_09_48_58_1000_GMT_10_00.txt",
//			"2011_07_06_09_49_28_1000_GMT_10_00.txt",
//			"2011_07_06_09_49_58_1000_GMT_10_00.txt",
//			"2011_07_06_09_50_28_1000_GMT_10_00.txt",
//			"2011_07_06_09_50_58_1000_GMT_10_00.txt",
		
		};
	
	private WalkingSpeedUtil walkingSpeedUtil = new WalkingSpeedUtil(PATH_SD_CARD_APP_LOC, DB_DATE_FORMAT); 
	private long[] timestamps;
	private float[][] accelData;
	private float[] dataMeans;
	private int count;

	public StepCounter(String fileName, boolean isLogOutput) throws FileNotFoundException {
		File inputFile = new File(fileName);

		Scanner scanner = new Scanner(inputFile);

		if (isLogOutput)
			this.parseLogOutput(scanner);
		else
			this.parseDataDump(scanner);
		
		walkingSpeedUtil.processData(
				timestamps,
				accelData,
				dataMeans,
				count
				);
	}
	
	private void parseLogOutput(Scanner scan) {
		
		ArrayList<double[]> list = new ArrayList<double[]>();
		
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) continue;
			
			String cols[] = line.split("\\s+");
			if (cols.length==4) {
				String fields[] = cols[3].split(",");
				double values[] = new double[fields.length];
				for (int i=0; i<fields.length; ++i) {
					values[i] = Double.parseDouble(fields[i]);
				}
				list.add(values);
			}
		}
		
		timestamps = new long[list.size()];
		accelData = new float[list.size()][3];
		dataMeans = new float[3];
		
		double sum = 0.0;
		
		for (int i=0; i<list.size(); ++i) {
			timestamps[i] = (long)list.get(i)[1];
			accelData[i][2] = (float)list.get(i)[2];
			sum += accelData[i][2];
		}
		
		//dataMeans[2] = (float)(sum / list.size());
		count = list.size();
	}
	
	private void parseDataDump(Scanner scan) {
		
		ArrayList<double[]> list = new ArrayList<double[]>();
		
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) continue;
			
			String fields[] = line.split(",");
			if (fields.length==3 && list.size()<256) {
				double values[] = new double[fields.length];
				for (int i=0; i<fields.length; ++i) {
					values[i] = Double.parseDouble(fields[i]);
				}
				list.add(values);
			}
			
			if (list.size()==256)
				break;
		}
		
		timestamps = new long[list.size()];
		accelData = new float[list.size()][3];
		dataMeans = new float[3];
		
		double sum = 0.0;
		
		for (int i=0; i<list.size(); ++i) {
			timestamps[i] = (long)list.get(i)[1];
			accelData[i][2] = (float)list.get(i)[2];
			sum += accelData[i][2];
		}
		
		//dataMeans[2] = (float)(sum / list.size());
		count = list.size();
	}
	
	private void copyToClipboard()
	{
		JPanel jPanel = new JPanel(); 
		
		Clipboard clipboard = jPanel.getToolkit().getSystemClipboard();
		StringBuilder builder = new StringBuilder();

		for (int i=0; i<count; ++i) {
			if (i!=0)
				builder.append("\n");
			builder.append(i).append("\t").append(timestamps[i]).append("\t").append(accelData[i][2]);
		}

		StringSelection data = new StringSelection(builder.toString());

		clipboard.setContents(data, data);
	}
	
	

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		try {
			Constants.OUTPUT_DEBUG_INFO = true;
			
			double stepsFound = 0.0;
			double heightFound = 0.0;
			double speedFound = 0.0;
			
			for (String path:INPUT_FILE_NAMES) {
				StepCounter stepCounter = new StepCounter(path, false);
				WalkingSpeedUtil wsu = stepCounter.walkingSpeedUtil;
				
				System.out.println(path+" >> "+wsu.getWalkingSpeed()+" ("+wsu.getMaxContigiousSteps()+"/"+wsu.getStepsFound()+")");
				
				if (wsu.getStepsFound()>1) {
					stepsFound += wsu.getStepsFound();
					heightFound += wsu.getWalkingHeight()*wsu.getStepsFound();
					speedFound += wsu.getWalkingSpeed()*wsu.getStepsFound();
				}
				
				stepCounter.copyToClipboard();
			}
			
			heightFound /= stepsFound;
			speedFound /= stepsFound;
			
			System.out.println();
			System.out.println("Total Steps   : "+stepsFound);
			System.out.println("Average hieght: "+heightFound);
			System.out.println("Average speed : "+speedFound);
			
		} catch (Exception e) {
		}
	}
}
