/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.sql_data;

import activityclassifier.Activity;
import activityclassifier.utils.Extractor;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 *
 * @author oka
 */
public class RawSqlDataParser {

    private static final Extractor headerEx = new Extractor(Pattern.compile("\\('([^']*)'"));
    private static final Extractor activityEx = new Extractor(Pattern.compile("ACTIVITY: ([^\\\\]*)\\\\n"));

    private static final Extractor dataEx = new Extractor(Pattern.compile(", '([^']*)'\\)"));
    private static final Extractor recordEx = new Extractor(Pattern.compile("(?:([^\\\\]*)\\\\n)|(?:(.*)$)"));
    private static final Extractor timeXYZABCEx = new Extractor(Pattern.compile("([0-9]+):(-?[.0-9]+),(-?[.0-9]+),(-?[.0-9]+),(-?[.0-9]+),(-?[.0-9]+),(-?[.0-9]+)"));

    private Scanner scanner;

    public RawSqlDataParser(File f) throws FileNotFoundException {
        this.scanner = new Scanner(f);
    }

    public List<Activity> scan() {
        ArrayList<Activity> activitys = new ArrayList<Activity>();
        ArrayList<Long> timeStamps = new ArrayList<Long>(128);
        ArrayList<float[]> readings = new ArrayList<float[]>(128);
        ArrayList<String> temp = new ArrayList<String>();

        while (this.scanner.hasNextLine()) {
            String line = this.scanner.nextLine();
            if (!line.isEmpty()) {
                String header = headerEx.extractSingle(line);
                String activityName = activityEx.extractSingle(header);

                if (activityName.isEmpty())
                    continue;

                String data = dataEx.extractSingle(line);

                if (data.isEmpty())
                    continue;

                System.out.println("activity='"+activityName+"'");

                String[] records = recordEx.extractMany(data, temp);

                timeStamps.clear();
                readings.clear();

                for (String record:records) {
                    String[] timeXYZABC = timeXYZABCEx.extractMany(record, temp);

                    if (timeXYZABC.length>0) {
                        String s_t = timeXYZABC[0];
                        String s_x = timeXYZABC[1];
                        String s_y = timeXYZABC[2];
                        String s_z = timeXYZABC[3];
                        String s_a = timeXYZABC[4];
                        String s_b = timeXYZABC[5];
                        String s_c = timeXYZABC[6];

                        long l_t = Long.parseLong(s_t);
                        float f_x = Float.parseFloat(s_x);
                        float f_y = Float.parseFloat(s_y);
                        float f_z = Float.parseFloat(s_z);
                        float f_a = Float.parseFloat(s_a);
                        float f_b = Float.parseFloat(s_b);
                        float f_c = Float.parseFloat(s_c);

                        timeStamps.add(l_t);
                        readings.add(new float[]{
                            f_x, f_y, f_z, f_a, f_b, f_c
                        });
                        //System.out.println("\tt="+t+" x="+x+" y="+y+" z="+z+" a="+a+" b="+b+" c="+c);
                    }
                }
                

                if (readings.size()>=128) {
                    Long[] times = timeStamps.toArray(new Long[timeStamps.size()]);
                    float[][] dt = readings.toArray(new float[readings.size()][]);
                    activitys.add(new Activity(activityName, times, dt));
                    System.out.println("\tactivity added");

                    timeStamps.clear();
                    readings.clear();
                } else {
                    System.out.println("\tonly "+readings.size()+" readings found.");
                }
            }
        }

        return activitys;
    }

}
