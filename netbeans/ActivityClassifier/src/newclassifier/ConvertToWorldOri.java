/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package newclassifier;

import activityclassifier.Activity;
import activityclassifier.Common;
import activityclassifier.utils.ActivityIO;
import activityclassifier.utils.FeatureExtractor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author abd01c
 */
public class ConvertToWorldOri {
    
    private static final int WINDOW_SIZE = 128;
    
    public static void main(String[] args) throws FileNotFoundException {
        FeatureExtractor extractor = new FeatureExtractor(WINDOW_SIZE);
        List<Activity> activities = ActivityIO.read(new Scanner(Common.CLEAN_SQL_DATA));
        PrintStream printStream = new PrintStream(new File("sql-data.csv"));
        for (int indexActivity=0; indexActivity<activities.size(); ++indexActivity) {
            Activity activity = activities.get(indexActivity);
            for (int windowStart=0; windowStart+WINDOW_SIZE<=activity.data.length; windowStart+=WINDOW_SIZE) {
                float features[] = extractor.extractUnrotated(activity.data, windowStart);
                if (features!=null) {
                    for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
                        printStream.print(features[f]);
                        printStream.print(",");
                    }
                    printStream.print("Subject"+(indexActivity+1));
                    printStream.print(",");
                    printStream.println(activity.name);
                }
            }
        }
        printStream.close();
    }
}
