/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier;

import activityclassifier.utils.StringComparator;
import activityclassifier.utils.CalcStatistics;
import activityclassifier.utils.FeatureExtractor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author oka
 */
public class KnnClassifier {

    private static final int STAT_MIN   = 0;
    private static final int STAT_MAX   = 1;
    private static final int STAT_MEAN  = 2;
    private static final int STAT_SD    = 3;

    private class Classification {
        public final float[] features;
        public final String activity;

        public Classification(float[] features, String activity) {
            this.features = Arrays.copyOf(features, featureCount);
            this.activity = activity;
        }
    }

    private class ClassificationDist {
        final Classification source;
        final float distance;

        public ClassificationDist( Classification source, float distance ) {
            this.source = source;
            this.distance = distance;
        }

    }

    private final int featureCount;
    private final int k;
    private List<Classification> classifications;
    private Map<String,float[][]> meanStats;

    public KnnClassifier(int featureCount, int k) {
        this.featureCount = featureCount;
        this.k = k;
        this.classifications = new ArrayList<Classification>();
        this.meanStats = new TreeMap<String, float[][]>(new StringComparator(false));
    }

    private void computeMeanStats() {
        meanStats.clear();

        Map<String,List<float[]>> groups = new TreeMap<String,List<float[]>>(new StringComparator(false));

        for (Classification cl:classifications) {
            if (!groups.containsKey(cl.activity))
                groups.put(cl.activity, new ArrayList<float[]>());

            groups.get(cl.activity).add(cl.features);
        }

        CalcStatistics cal = new CalcStatistics(FeatureExtractor.NUM_FEATURES);

        for (String activity:groups.keySet()) {
            List<float[]> data = groups.get(activity);

            float[][] array = new float[data.size()][];

            array = data.toArray(array);

            cal.assign(array, array.length);

            float[][] stats = new float[][] {
                Arrays.copyOf(cal.getMin(), FeatureExtractor.NUM_FEATURES),
                Arrays.copyOf(cal.getMax(), FeatureExtractor.NUM_FEATURES),
                Arrays.copyOf(cal.getMean(), FeatureExtractor.NUM_FEATURES),
                Arrays.copyOf(cal.getStandardDeviation(), FeatureExtractor.NUM_FEATURES),
            };
            
            for (int i=0; i<stats[STAT_SD].length; ++i) {
                //  avoid zero in standard dev.
                if (stats[STAT_SD][i]==0.0f) {
                    stats[STAT_SD][i] = Float.MIN_VALUE;
                }
            }

            meanStats.put(activity, stats);

//            System.out.println("\t"+activity+":\n\t"+Arrays.toString(stats[0])+"\n\t"+Arrays.toString(stats[1]));
        }

    }
    
    public void addClassification(float[] features, String classification)
    {
        classifications.add(new Classification(features, classification));
    }

    public String getBestClassification(float[] features, String expected, boolean normalize)
    {
        if (normalize && meanStats.isEmpty()) {
            computeMeanStats();
        }

        float val, dist;

        int numClassifications = classifications.size();

        List<ClassificationDist> temps = new ArrayList<ClassificationDist>(numClassifications);
        float[][] stats = null;

        for (int j=0; j<numClassifications; ++j) {
            Classification c = classifications.get(j);

            if (normalize) {
                stats = meanStats.get(c.activity);
            }

            dist = 0.0f;
            for (int i=0; i<featureCount; ++i) {
                val = c.features[i]-features[i];

                if (normalize) {
                    val = val * (c.features[i]-stats[STAT_MEAN][i])/stats[STAT_SD][i];
//                    val = val / (stats[STAT_MAX][i]-stats[STAT_MIN][i]);
                }

                dist += val*val;
            }
            dist = (float)Math.sqrt(dist);

            temps.add(new ClassificationDist(c, dist));
        }

        Collections.sort(temps, new Comparator<ClassificationDist>() {
            public int compare( ClassificationDist o1, ClassificationDist o2 ) {
                return Float.compare(o1.distance, o2.distance);
            }
        });

        Map<String,Integer> counters = new TreeMap<String, Integer>(new StringComparator(false));
        ClassificationDist bestClassification = null;
        int bestCount = 0;
        int totalCount = 0;

        for (ClassificationDist temp:temps) {
            if (!counters.containsKey(temp.source.activity))
                counters.put(temp.source.activity, 0);

            int countClassification = counters.get(temp.source.activity);
            ++countClassification;
            counters.put(temp.source.activity, countClassification);

            if (countClassification>bestCount) {
                bestClassification = temp;
                bestCount = countClassification;
            }

            ++totalCount;
            if (totalCount==k) {
                break;
            }
        }

        if (bestClassification!=null) {
//            if (!foundClassification.source.activity.equals(expected)) {
//                System.out.println(
//                        "features: "+Arrays.toString(features)+"" +
//                        "\n\texpected: "+expected+", found: "+foundClassification.source.activity);
//                for (ClassificationDist temp:temps) {
//                    System.out.println("\t"+temp.source.activity+" : "+Arrays.toString(temp.source.features)+" = "+temp.distance);
//                }
//            }
            
            return bestClassification.source.activity;
        }
        else
            return "";
    }

}
