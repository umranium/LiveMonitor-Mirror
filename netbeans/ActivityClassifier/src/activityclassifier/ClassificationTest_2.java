/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier;

import activityclassifier.utils.StringComparator;
import activityclassifier.utils.ActivityIO;
import activityclassifier.utils.CalcStatistics;
import activityclassifier.utils.FeatureExtractor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author integ
 */
public class ClassificationTest_2 {

    private static final int WINDOW_SIZE = 128;

    private static Set<String> avoidTrainingWith = new TreeSet<String>(new StringComparator(false)) {
        {
//            this.add("CLASSIFIED/TRAVELLING");
        }
    };


    private static double simulate(int k, boolean normalize, Map<String,List<ExtractedFeature>> extracted, int numberOfTimes)
    {
        Map<String,Integer> activityIndexes = new TreeMap(new StringComparator(true));
        List<String> activityNames = new ArrayList<String>(extracted.keySet());
        for (int i=0; i<activityNames.size(); ++i) {
            activityIndexes.put(activityNames.get(i), i);
        }
                //activityIndexes.put("CLASS/UNKNOWN",4);


        int activityOverallCount[] = new int[extracted.keySet().size()];
        for (String activity:activityNames) {
            activityOverallCount[activityIndexes.get(activity)] = extracted.get(activity).size();
        }

        int confusionMatrix[][] = new int[extracted.keySet().size()][extracted.keySet().size()];
        int expectedTotals[] = new int[extracted.keySet().size()];
        int obtainedTotals[] = new int[extracted.keySet().size()];
        int successCount = 0;
        int overallCount = 0;
        int trainingCounts[] = new int[extracted.keySet().size()];
        int testingCounts[] = new int[extracted.keySet().size()];
                int [] numberOfUnknowforEachClass = new int[3];


        List<ExtractedFeature> extractedFeatures = new ArrayList<ExtractedFeature>();
        for (String activityName:extracted.keySet()) {
            for (ExtractedFeature feature:extracted.get(activityName)) {
                if (!Filters.shouldFilter(feature))
                    extractedFeatures.add(feature);
            }
        }
        int totalExtracted = extractedFeatures.size();

        System.out.println("total extracted: "+totalExtracted);

        List<Integer> availableForPicking = new ArrayList<Integer>();
        for (int i=0; i<totalExtracted; ++i)
            if (!avoidTrainingWith.contains(extractedFeatures.get(i).activityName)) {
                availableForPicking.add(i);
            } else {
                if (!extractedFeatures.get(i).activityName.toLowerCase().contains("travelling")) {
                    System.out.println("bloddy hell!");
                }
            }

        System.out.println("total usable : "+availableForPicking.size());

        int trainingNumber = availableForPicking.size() - availableForPicking.size() / 4;

        if (trainingNumber==0) {
            throw new RuntimeException("ERROR: zero number of items for training");
        }
        if (totalExtracted-trainingNumber==0) {
            throw new RuntimeException("ERROR: zero number of items for testing");
        }

        for (int iteration=0; iteration<numberOfTimes; ++iteration) {
            KnnClassifier classifier = new KnnClassifier(FeatureExtractor.NUM_FEATURES, k);

            List<ExtractedFeature> trainingSet = new ArrayList<ExtractedFeature>();
            List<ExtractedFeature> testingSet = new ArrayList<ExtractedFeature>();

//            System.out.println(totalExtracted+" features extracted for activity '"+activityName+"'");

            HashSet<Integer> picked = new HashSet<Integer>();
            for (int i=0; i<trainingNumber; ++i) {
                int pick;
                do {
                    int index = (int)(Math.random()*availableForPicking.size());
                    pick = availableForPicking.get(index);
                } while (picked.contains(pick));
                picked.add(pick);
            }

            for (int i=0; i<totalExtracted; ++i) {
                if (picked.contains(i)) {
                    trainingSet.add(extractedFeatures.get(i));
                    ++trainingCounts[activityIndexes.get(extractedFeatures.get(i).activityName)];
                } else {
                    testingSet.add(extractedFeatures.get(i));
                    ++testingCounts[activityIndexes.get(extractedFeatures.get(i).activityName)];
                }
            }

//            {   //  Statistics
//                System.out.println("Statistics:");
//                System.out.println("=======");
//
//                Map<String,List<ExtractedFeature>> activityFeatures = new TreeMap<String, List<ExtractedFeature>>(new StringComparator(false));
//                for (ExtractedFeature f:trainingSet) {
//                    List<ExtractedFeature> list = activityFeatures.get(f.activityName);
//                    if (list==null) {
//                        list = new ArrayList<ExtractedFeature>();
//                        activityFeatures.put(f.activityName, list);
//                    }
//                    list.add(f);
//                }
//
//                for (String activity:activityFeatures.keySet()) {
//                    System.out.println("Activity: "+activity);
//                    System.out.println("\t"+Arrays.toString(FeatureExtractor.FEATURE_NAMES));
//
//                    List<ExtractedFeature> features = activityFeatures.get(activity);
//
//                    float[][] data = new float[features.size()][FeatureExtractor.NUM_FEATURES];
//
//                    for (int i=0; i<features.size(); ++i) {
//                        ExtractedFeature ex = features.get(i);
//                        for (int f=0; f<FeatureExtractor.NUM_FEATURES; ++f) {
//                            data[i][f] = ex.features[f];
//                        }
//                    }
//
//                    CalcStatistics cs  = new CalcStatistics(FeatureExtractor.NUM_FEATURES);
//                    cs.assign(data, features.size());
//
//                    System.out.println("\tMean: "+Arrays.toString(cs.getMean()));
//                    System.out.println("\tSD: "+Arrays.toString(cs.getStandardDeviation()));
//                }
//            }
            
            //System.out.println("Training:");
            for (ExtractedFeature feature:trainingSet)
                classifier.addClassification(feature.features, feature.activityName);

            //System.out.println("Testing:");
            for (ExtractedFeature feature:testingSet) {
                String classification = classifier.getBestClassification(feature.features, feature.activityName, normalize);

                int expected = activityIndexes.get(feature.activityName);
                int obtained = activityIndexes.get(classification);

    //                System.out.println("\t"+classification);
                                if (obtained<4)
                {
                ++confusionMatrix[expected][obtained];
                ++expectedTotals[expected];
                ++obtainedTotals[obtained];
                }
                else
                {
                    ++numberOfUnknowforEachClass[expected];
                }

                if (classification.equalsIgnoreCase(feature.activityName)){// || classification.equalsIgnoreCase("CLASS/UNKNOWN")) {
                    ++successCount;
                }

                ++overallCount;
            }
        }

        System.out.println("Activities: "+activityNames);
        System.out.println("Training  : "+Arrays.toString(trainingCounts));
        System.out.println("Testing   : "+Arrays.toString(testingCounts));
        System.out.println("Total     : "+Arrays.toString(activityOverallCount));

        System.out.println("Confusion Matrix:");
        System.out.println(String.format("%25s", "Expected")+"\tObtained");
        { //  header
            System.out.print(String.format("%25s", "")+"\t");
            for (int obtained=0; obtained<activityNames.size(); ++obtained) {
                System.out.print(String.format("%25s", activityNames.get(obtained))+"\t");
            }
            System.out.println();
        }
        for (int expected=0; expected<activityNames.size(); ++expected) {
            System.out.print(String.format("%25s", activityNames.get(expected))+"\t");
            for (int obtained=0; obtained<activityNames.size(); ++obtained) {
                System.out.print(String.format("%24d%%", Math.round(100.0*confusionMatrix[expected][obtained]/overallCount))+"\t");
            }
            System.out.print(String.format("%24d%%", Math.round(100.0*expectedTotals[expected]/overallCount))+"\t");
            System.out.println();

        }
        { //  footer
            System.out.print(String.format("%25s", "")+"\t");
            for (int obtained=0; obtained<activityNames.size(); ++obtained) {
                System.out.print(String.format("%24d%%", Math.round(100.0*obtainedTotals[obtained]/overallCount))+"\t");
            }
            System.out.println();
        }

//        for (int i = 0; i<activityNames.size(); i++)
//        {
//            double ratio = Math.round(100.0 * numberOfUnknowforEachClass[i]/overallCount);
//            System.out.print(String.format("Unknowns ratio for class %s = ",activityNames.get(i)) + Double.toString(ratio) + "%\n");
//        }
        double percentSuccess = 100.0*successCount/overallCount;
//        System.out.println("\tmatched "+success+" out of "+testingSet.size()+
//                " ("+percentSuccess+"%)");

        return percentSuccess;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            FeatureExtractor featureExtractor = new FeatureExtractor(WINDOW_SIZE);

            List<Activity> activities = ActivityIO.read(new Scanner(Common.CLEAN_SQL_DATA_PADDLING));
//            System.out.println("Activities Loaded: "+activities.size());

            Map<String,List<ExtractedFeature>> extracted = new TreeMap(new StringComparator(true));

            for (Activity activity:activities) {
//                if (activity.name.contains("TRAVELLING")) continue;

                for (int windowStart=0; windowStart+WINDOW_SIZE<=activity.data.length; windowStart+=WINDOW_SIZE) {

                    float features[] = featureExtractor.extractUnrotated(activity.data, windowStart);

                    if (features!=null) {
                        if (!extracted.containsKey(activity.name))
                            extracted.put(activity.name, new ArrayList<ExtractedFeature>());
                        extracted.get(activity.name).add(
                                new ExtractedFeature(activity.name, Arrays.copyOf(features, features.length)));
                    }
                }
            }

            for (int k=1; k<=1; ++k)
            {
                System.out.println("Using K="+k);
                double result = simulate(k, false, extracted, 500);
                System.out.println("average success rate="+result+"%");
            }

            //double result = simulate(10, false, extracted, 500);
            //System.out.println("average success rate="+result+"%");

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

}

