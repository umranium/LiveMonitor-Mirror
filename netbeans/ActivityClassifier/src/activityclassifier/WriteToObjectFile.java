/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier;


import activityclassifier.utils.StringComparator;
import activityclassifier.utils.ActivityIO;
import activityclassifier.utils.FeatureExtractor;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
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
public class WriteToObjectFile {

    private static final int WINDOW_SIZE = 128;

    public static void main( String[] args ) {
        try {
            FeatureExtractor featureExtractor = new FeatureExtractor(WINDOW_SIZE);
            KnnClassifier classifier = new KnnClassifier(FeatureExtractor.NUM_FEATURES, 3);

            List<Activity> activities = ActivityIO.read(new Scanner(Common.CLEAN_SQL_DATA));
            System.out.println("Activities Loaded: "+activities.size());

            Map<String,List<ExtractedFeature>> extracted = new TreeMap(new StringComparator(true));

            for (Activity activity:activities) {
//                if (activity.name.contains("STATIONARY")) continue;
//                System.out.println("Activity: "+activity.name);

                for (int windowStart=0; windowStart+WINDOW_SIZE<=activity.data.length; windowStart+=WINDOW_SIZE) {

                    float features[] = featureExtractor.extractUnrotated(activity.data, windowStart);
//                    System.out.println("features="+Arrays.toString(features));

                    if (features!=null) {
                        if (!extracted.containsKey(activity.name))
                            extracted.put(activity.name, new ArrayList<ExtractedFeature>());

                        ExtractedFeature extractedFeature = new ExtractedFeature(activity.name, Arrays.copyOf(features, features.length));

                        if (!Filters.shouldFilter(extractedFeature)) {
                            extracted.get(activity.name).add(extractedFeature);
                            if (extractedFeature.activityName.contains("WALKING") && extractedFeature.features[FeatureExtractor.FEATURE_HOR_RANGE]<1.5f) {
                                throw new RuntimeException("Filter not working!");
                            }
                        }
                    }
                }
            }
            
            if (extracted.isEmpty()) {
                System.out.println("ERROR: No data to write to object file. All activities appears to have been filtered.");
            } else {
                System.out.println(extracted.size()+" activities found and being written to object file.");

                Map<String,Integer> counters = new TreeMap<String, Integer>(new StringComparator(false));
                Map<Float[], Object[]> outputMap = new IdentityHashMap<Float[], Object[]>();

                int index = 0;

                for (String activity:extracted.keySet()) {

                    List<ExtractedFeature> extractedFeatures = extracted.get(activity);

                    for (ExtractedFeature feature:extractedFeatures) {
                        Float[] data = new Float[feature.features.length];
                        for (int i=0; i<feature.features.length; ++i)
                            data[i] = feature.features[i];
                        outputMap.put(data, new Object[] {activity, (Integer)index++});

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

                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Common.OUTPUT_OBJECT_MODEL));
                out.writeObject(outputMap);
                out.close();
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

}
