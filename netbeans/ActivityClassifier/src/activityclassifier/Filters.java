/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier;

import activityclassifier.utils.StringComparator;
import activityclassifier.utils.FeatureExtractor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Umran
 */
public class Filters {

    private static final Map<String,Map<Integer,float[][]>> activityFeatureFilters = new TreeMap<String,Map<Integer,float[][]>>(new StringComparator(false));

    static {
        Map<Integer,float[][]> stationaryFilters = new HashMap<Integer, float[][]>(FeatureExtractor.NUM_FEATURES);
        Map<Integer,float[][]> travellingFilters = new HashMap<Integer, float[][]>(FeatureExtractor.NUM_FEATURES);
        Map<Integer,float[][]> walkingFilters = new HashMap<Integer, float[][]>(FeatureExtractor.NUM_FEATURES);

        //******************* STATIONARY **************************************************//
        stationaryFilters.put(FeatureExtractor.FEATURE_HOR_RANGE,
                new float[][] {
                    { 0.0f, 1.5f },
                }
        );
        stationaryFilters.put(FeatureExtractor.FEATURE_VER_RANGE,
                new float[][] {
                    { 0.0f, 1.2f },
                }
        );
        stationaryFilters.put(FeatureExtractor.FEATURE_HOR_MEAN,
                new float[][] {
                    { 0.0f, 1.2f },
                }
        );
        stationaryFilters.put(FeatureExtractor.FEATURE_VER_MEAN,
                new float[][] {
                    { 9.4f, 10.3f },
                }
        );
        stationaryFilters.put(FeatureExtractor.FEATURE_HOR_STDDEV,
                new float[][] {
                    { 0.0f, 0.6f },
                }
        );
        stationaryFilters.put(FeatureExtractor.FEATURE_VER_STDDEV,
                new float[][] {
                    { 0.0f, 0.4f },
                }
        );
        //******************* TRAVELLING **************************************************//
        travellingFilters.put(FeatureExtractor.FEATURE_HOR_RANGE,
                new float[][] {
                    { 0.5f, 4.1f },
                }
        );
        travellingFilters.put(FeatureExtractor.FEATURE_VER_RANGE,
                new float[][] {
                    { 0.1f, 7.5f },
                }
        );
        travellingFilters.put(FeatureExtractor.FEATURE_HOR_MEAN,
                new float[][] {
                    { 1.2f, 2.3f },
                }
        );
        travellingFilters.put(FeatureExtractor.FEATURE_VER_MEAN,
                new float[][] {
                    { 9.1f, 10.3f },
                }
        );
        travellingFilters.put(FeatureExtractor.FEATURE_HOR_STDDEV,
                new float[][] {
                    { 0.1f, 1.0f },
                }
        );
        travellingFilters.put(FeatureExtractor.FEATURE_VER_STDDEV,
                new float[][] {
                    { 0.4f, 2.1f },
                }
        );

        //******************* WALKING **************************************************//
        walkingFilters.put(FeatureExtractor.FEATURE_HOR_RANGE,
                new float[][] {
                    { 1.5f, 15.0f },
                }
        );
        walkingFilters.put(FeatureExtractor.FEATURE_VER_RANGE,
                new float[][] {
                    { 2.5f, 23.5f },
                }
        );
        walkingFilters.put(FeatureExtractor.FEATURE_HOR_MEAN,
                new float[][] {
                    { 0.4f, 5.5f },
                }
        );
        walkingFilters.put(FeatureExtractor.FEATURE_VER_MEAN,
                new float[][] {
                    { 8.0f, 10.8f },
                }
        );
        walkingFilters.put(FeatureExtractor.FEATURE_HOR_STDDEV,
                new float[][] {
                    { 0.2f, 3.5f },
                }
        );
        walkingFilters.put(FeatureExtractor.FEATURE_VER_STDDEV,
                new float[][] {
                    { 0.0f, 5.5f },
                }
        );

        activityFeatureFilters.put("CLASSIFIED/STATIONARY", stationaryFilters);
        activityFeatureFilters.put("CLASSIFIED/TRAVELLING", travellingFilters);
        activityFeatureFilters.put("CLASSIFIED/WALKING", walkingFilters);
        activityFeatureFilters.put("CLASSIFIED/PADDLING", walkingFilters);
    }

    public static boolean shouldFilter(ExtractedFeature sample) {
        Map<Integer,float[][]> featureFilters = activityFeatureFilters.get(sample.activityName);

        if (featureFilters==null) {
            throw new RuntimeException("Unknown Activity: '"+sample.activityName+"'");
        }

        boolean allFound = true;

        for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
            float[][] filters = featureFilters.get(f);

            if (filters==null) {
                throw new RuntimeException("Feature "+FeatureExtractor.FEATURE_NAMES[f]+"("+f+") for activity '"+sample.activityName+"' not found");
            }

            boolean found = false;

            for (float[] filter:filters) {
                if (sample.features[f]>=filter[0] && sample.features[f]<=filter[1]) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                allFound = false;
                break;
            }
        }

        return !allFound;
    }


}
