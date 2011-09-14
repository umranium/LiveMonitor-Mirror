/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.csv_data;

import activityclassifier.Activity;
import activityclassifier.Common;
import activityclassifier.utils.ActivityIO;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author oka
 */
public class ParseRawCsvData {


    public static void main(String[] args) {
        try {
            List<Activity> activities = new ArrayList<Activity>();
            /*
            {
                String activityName = "CLASSIFIED/WALKING/BREAST_POCKET";
                File activityCsvFile = new File("./walking/walking-breast-pocket.csv");
                RawCsvDataParser parseDataSql = new RawCsvDataParser(activityName, activityCsvFile);
                List<Activity> parsed = parseDataSql.scan();
                System.out.println(parsed.size()+" activities from "+activityName);
                activities.addAll(parsed);
            }
            
            {
                String activityName = "CLASSIFIED/WALKING/LEFT_TROUSER_POCKET";
                File activityCsvFile = new File("./walking/walking-left-trouser-pocket.csv");
                RawCsvDataParser parseDataSql = new RawCsvDataParser(activityName, activityCsvFile);
                List<Activity> parsed = parseDataSql.scan();
                System.out.println(parsed.size()+" activities from "+activityName);
                activities.addAll(parsed);
            }

            {
                String activityName = "CLASSIFIED/WALKING/RIGHT_TROUSER_POCKET";
                File activityCsvFile = new File("./walking/walking-right-trouser-pocket.csv");
                RawCsvDataParser parseDataSql = new RawCsvDataParser(activityName, activityCsvFile);
                List<Activity> parsed = parseDataSql.scan();
                System.out.println(parsed.size()+" activities from "+activityName);
                activities.addAll(parsed);
            }
             */
            {
                String activityName = "CLASSIFIED/WALKING/LEFT_TROUSER_POCKET";
                File activityCsvFile = new File("./walking/brief-left-pocket.csv");
                RawCsvDataParser parseDataSql = new RawCsvDataParser(activityName, activityCsvFile);
                List<Activity> parsed = parseDataSql.scan();
                System.out.println(parsed.size()+" activities from "+activityName);
                activities.addAll(parsed);
            }

            System.out.println("Saving dirty CSV data to "+Common.DIRTY_CSV_DATA);
            PrintStream ps = new PrintStream(Common.DIRTY_CSV_DATA);
            ActivityIO.write(ps, activities);
            ps.close();
            System.out.println("Activities Written: "+activities.size());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

}
