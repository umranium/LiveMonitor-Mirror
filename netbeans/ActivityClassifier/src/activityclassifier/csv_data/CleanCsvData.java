/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.csv_data;

import activityclassifier.Activity;
import activityclassifier.Common;
import activityclassifier.utils.ActivityIO;
import activityclassifier.utils.FeatureExtractor;
import activityclassifier.utils.RotateSamplesToVerticalHorizontal;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *
 * @author oka
 */
public class CleanCsvData {

    private static final int WINDOW_SIZE = 128;

    private static final int NUM_OF_STATS = 3;
    private static final int STAT_COUNT = 0;
    private static final int STAT_MEAN = 1;
    private static final int STAT_STDDEV = 2;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            List<Activity> activities = ActivityIO.read(new Scanner(Common.DIRTY_CSV_DATA));
            RotateSamplesToVerticalHorizontal rstvh = new RotateSamplesToVerticalHorizontal();
            FeatureExtractor extractor = new FeatureExtractor(WINDOW_SIZE);
            List<Activity> filteredActivities = new ArrayList<Activity>();

            float[][] threeDimSamples = new float[WINDOW_SIZE][3];

            Map<String,float[][]> activityStats = new TreeMap<String, float[][]>();

            int initialWindows = 0;
            int finalWindows = 0;

            //  get the sum so that we can compute the mean
            for (Activity activity:activities) {
                if (!activityStats.containsKey(activity.name)) {
                    activityStats.put(activity.name, new float[NUM_OF_STATS][FeatureExtractor.NUM_FEATURES]);
                }

                float[][] stats = activityStats.get(activity.name);

                int rowCount = activity.data.length - activity.data.length%WINDOW_SIZE;
                
                for (int windowStart=0; windowStart+WINDOW_SIZE<=rowCount; windowStart+=WINDOW_SIZE) {

                    ++initialWindows;

                    for (int i=0; i<WINDOW_SIZE; ++i) {
                        threeDimSamples[i][0] = activity.data[i+windowStart][0];
                        threeDimSamples[i][1] = activity.data[i+windowStart][1];
                        threeDimSamples[i][2] = activity.data[i+windowStart][2];
                    }

                    float[] features = extractor.extractUnrotated(threeDimSamples, 0);

                    if (features!=null) {
                        for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
                            stats[STAT_COUNT][f] += 1.0f;
                            stats[STAT_MEAN][f] += features[f];
                        }
                    }
                }
            }

            //  compute the mean
            for (String activity:activityStats.keySet()) {
                float[][] stats = activityStats.get(activity);

                for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
                    stats[STAT_MEAN][f] = stats[STAT_MEAN][f] / stats[STAT_COUNT][f];
                }
            }

            //  get variance so that we can compute the std dev
            for (Activity activity:activities) {
                float[][] stats = activityStats.get(activity.name);

                int rowCount = activity.data.length - activity.data.length%WINDOW_SIZE;

                for (int windowStart=0; windowStart+WINDOW_SIZE<=rowCount; windowStart+=WINDOW_SIZE) {

                    for (int i=0; i<WINDOW_SIZE; ++i) {
                        threeDimSamples[i][0] = activity.data[i+windowStart][0];
                        threeDimSamples[i][1] = activity.data[i+windowStart][1];
                        threeDimSamples[i][2] = activity.data[i+windowStart][2];
                    }

                    float[] features = extractor.extractUnrotated(threeDimSamples, 0);
                    if (features!=null) {

                        for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
                            float x = features[f] - stats[STAT_MEAN][f];
                            stats[STAT_STDDEV][f] += x*x;
                        }
                    }
                }
            }

            //  compute the std dev
            for (String activity:activityStats.keySet()) {
                float[][] stats = activityStats.get(activity);

                for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
                    stats[STAT_STDDEV][f] = (float)Math.sqrt(stats[STAT_STDDEV][f] / stats[STAT_COUNT][f]);
                }
            }

            //  filter any windows heigher or lower than 1 std dev from mean
            for (Activity activity:activities) {
                int rowCount = activity.data.length - activity.data.length%WINDOW_SIZE;
                float[][] stats = activityStats.get(activity.name);

                for (int windowStart=0; windowStart+WINDOW_SIZE<=rowCount; windowStart+=WINDOW_SIZE) {

                    for (int i=0; i<WINDOW_SIZE; ++i) {
                        threeDimSamples[i][0] = activity.data[i+windowStart][0];
                        threeDimSamples[i][1] = activity.data[i+windowStart][1];
                        threeDimSamples[i][2] = activity.data[i+windowStart][2];
                    }

                    float[] features = extractor.extractUnrotated(threeDimSamples, 0);
                    if (features!=null) {

                        boolean invalid = false;
                        for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
                            if (    features[f]<stats[STAT_MEAN][f]-stats[STAT_STDDEV][f] ||
                                    features[f]>stats[STAT_MEAN][f]+stats[STAT_STDDEV][f]   )
                            {
                                invalid = true;
                                break;
                            }
                        }

                        if (!invalid) {
                            ++finalWindows;
                            filteredActivities.add(
                                    new Activity(activity.name,
                                                Arrays.copyOfRange(activity.time, windowStart, windowStart+WINDOW_SIZE),
                                                Arrays.copyOfRange(activity.data, windowStart, windowStart+WINDOW_SIZE)
                                                ));
                        }
                    }
                }
            }


            System.out.println("Saving clean sql data to "+Common.CLEAN_CSV_DATA);
            System.out.println(initialWindows+" windows filtered to "+finalWindows);
            PrintStream out = new PrintStream(Common.CLEAN_CSV_DATA);
            ActivityIO.write(out, filteredActivities);
            out.close();
            System.out.println("Activities Written: "+filteredActivities.size());

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

}
