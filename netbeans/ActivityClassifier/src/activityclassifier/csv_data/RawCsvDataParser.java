/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.csv_data;

import activityclassifier.Activity;
import activityclassifier.utils.CsvReader;
import activityclassifier.utils.Extractor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 *
 * @author oka
 */
public class RawCsvDataParser {

    private static final int COL_TIME = 0;
    private static final int COL_X = 1;
    private static final int COL_Y = 2;
    private static final int COL_Z = 3;

    private File f;
    private String activityName;
    private CsvReader reader;

    public RawCsvDataParser(String activityName, File f) throws FileNotFoundException {
        this.f = f;
        this.activityName = activityName;
        this.reader = new CsvReader(',', f);
    }

    public List<Activity> scan() throws IOException {
        ArrayList<Activity> activitys = new ArrayList<Activity>();
        ArrayList<Long> timeStamps = new ArrayList<Long>(128);
        ArrayList<float[]> readings = new ArrayList<float[]>(128);
        ArrayList<String> temp = new ArrayList<String>();
        int count = 0;

        if (this.reader.readRow(temp)) {    //  read header
            timeStamps.clear();
            readings.clear();

            while (this.reader.readRow(temp)) { //  read data
                long time = Long.parseLong(temp.get(COL_TIME));
                float x = Float.parseFloat(temp.get(COL_X));
                float y = Float.parseFloat(temp.get(COL_Y));
                float z = Float.parseFloat(temp.get(COL_Z));

                timeStamps.add(time);
                readings.add(new float[]{
                    x, y, z
                });

                if (readings.size()>=128) {
                    Long[] times = timeStamps.toArray(new Long[timeStamps.size()]);
                    float[][] dt = readings.toArray(new float[readings.size()][]);
                    activitys.add(new Activity(activityName+"{"+f.getName()+":"+count+"}", times, dt));
//                    activitys.add(new Activity(activityName, times, dt));
                    System.out.println("\tactivity added");
                    
                    count += readings.size();
                    timeStamps.clear();
                    readings.clear();
                }
            }
        }
        
        return activitys;
    }

}
