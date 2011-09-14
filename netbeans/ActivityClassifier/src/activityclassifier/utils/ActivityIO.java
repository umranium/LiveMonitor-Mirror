/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.utils;

import activityclassifier.Activity;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 *
 * @author oka
 */
public class ActivityIO {

    private static final Extractor timeXYZEx = new Extractor(Pattern.compile("([0-9]+):(-?[.0-9]+),(-?[.0-9]+),(-?[.0-9]+)"));

    public static void write(PrintStream out, List<Activity> activities) {
        for (Activity activity:activities) {
            out.println(activity.name);
            out.println(activity.data.length);
            for (int row=0; row<activity.data.length; ++row) {
                out.println(String.format(
                        "%d:%.7f,%.7f,%.7f",
                        (long)activity.time[row],
                        activity.data[row][0],
                        activity.data[row][1],
                        activity.data[row][2]
                        ));
            }
            out.println();
        }
    }

    public static List<Activity> read(Scanner scan) {
        ArrayList<Activity> activitys = new ArrayList<Activity>();
        ArrayList<Long> timeStamps = new ArrayList<Long>(128);
        ArrayList<float[]> readings = new ArrayList<float[]>(128);
        ArrayList<String> temp = new ArrayList<String>();

        while (scan.hasNextLine()) {
            String line = scan.nextLine().trim();

            if (line.isEmpty())
                continue;

            String activityName = line;
            int isPad = activityName.compareTo("CLASSIFIED/PADDLING");

            if (scan.hasNextLine()) {
                line = scan.nextLine().trim();

                timeStamps.clear();
                readings.clear();

                int numOfRows = Integer.parseInt(line);
                for (int row=0; row<numOfRows; ++row) {
                    if (scan.hasNextLine()) {
                        line = scan.nextLine().trim();
                        
                        if (line.isEmpty())
                            continue;

                        String[] timeXYZ = timeXYZEx.extractMany(line, temp);

                        if (timeXYZ.length>0) {
                            String s_t = timeXYZ[0];
                            String s_x = timeXYZ[1];
                            String s_y = timeXYZ[2];
                            String s_z = timeXYZ[3];

                            long l_t = Long.parseLong(s_t);
                            float f_x = Float.parseFloat(s_x);
                            float f_y = Float.parseFloat(s_y);
                            float f_z = Float.parseFloat(s_z);

                            timeStamps.add(l_t);
                            readings.add(new float[]{
                                f_x, f_y, f_z
                            });
                            //System.out.println("\tt="+t+" x="+x+" y="+y+" z="+z+" a="+a+" b="+b+" c="+c);
                        }

                    } else {
                        break;
                    }
                }

                if (readings.size()>=128) {
                    Long[] times = timeStamps.toArray(new Long[timeStamps.size()]);
                    float[][] dt = readings.toArray(new float[readings.size()][]);
                    activitys.add(new Activity(activityName, times, dt));
//                    System.out.println("\tactivity added");
                } else {
//                    System.out.println("\tonly "+readings.size()+" readings found.");
                }

            }
            
        }

        return activitys;
    }

}
