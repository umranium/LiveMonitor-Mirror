/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier;


import activityclassifier.utils.StringComparator;
import activityclassifier.utils.ActivityIO;
import activityclassifier.utils.FeatureExtractor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *
 * @author integ
 */
public class WriteToTextFile {

    private static final int WINDOW_SIZE = 128;

    public static void main( String[] args ) {
        try {
            FeatureExtractor featureExtractor = new FeatureExtractor(WINDOW_SIZE);
            KnnClassifier classifier = new KnnClassifier(FeatureExtractor.NUM_FEATURES, 3);

            List<Activity> activities = ActivityIO.read(new Scanner(Common.CLEAN_SQL_DATA));
            System.out.println("Activities Loaded: "+activities.size());

            Map<String,List<ExtractedFeature>> extracted = new TreeMap(new StringComparator(true));

            for (Activity activity:activities) {
                for (int windowStart=0; windowStart+WINDOW_SIZE<=activity.data.length; windowStart+=WINDOW_SIZE) {

                    float features[] = featureExtractor.extractUnrotated(activity.data, windowStart);

                    if (features!=null) {
                        if (!extracted.containsKey(activity.name))
                            extracted.put(activity.name, new ArrayList<ExtractedFeature>());

                        ExtractedFeature extractedFeature = new ExtractedFeature(activity.name, Arrays.copyOf(features, features.length));

                        //if (!Filters.shouldFilter(extractedFeature))
                        {
                            extracted.get(activity.name).add(extractedFeature);
                        }
                    }
                }
            }
            
            if (extracted.isEmpty()) {
                System.out.println("ERROR: No data to write to file. All activities appears to have been filtered.");
            } else {
                System.out.println(extracted.size()+" activities found and being written to object file.");

                Map<String,Integer> counters = new TreeMap<String, Integer>(new StringComparator(false));

                int index = 0;

                PrintStream out = new PrintStream(Common.OUTPUT_TEXT_FILE);

                out.print("Activity\t");
                for (int i=0; i<FeatureExtractor.FEATURE_NAMES.length; ++i) {
                    out.print(FeatureExtractor.FEATURE_NAMES[i]+"\t");
                }
                out.println();

                for (String activity:extracted.keySet()) {

                    List<ExtractedFeature> extractedFeatures = extracted.get(activity);

                    for (ExtractedFeature feature:extractedFeatures) {
                        out.print(activity+"\t");
                        for (int i=0; i<FeatureExtractor.FEATURE_NAMES.length; ++i) {
                            out.print(feature.features[i]+"\t");
                        }
                        out.println();

                        if (!counters.containsKey(activity))
                            counters.put(activity, 0);
                        int count = counters.get(activity);
                        count++;
                        counters.put(activity, count);
                    }
                }

                for (String s:counters.keySet()) {
                    System.out.println("activity: '"+s+"' = "+counters.get(s)+" samples");
                }

                out.close();
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

}
