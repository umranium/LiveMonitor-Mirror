/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.sql_data;

import activityclassifier.Activity;
import activityclassifier.Common;
import activityclassifier.utils.ActivityIO;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author oka
 */
public class CleanSqlData {

    private static final boolean GENERATE_FILTERS = false;

    private static final String[][] ACTIVITY_FILTERS = new String[][] {
        new String[] {",kirserg", null },
        new String[] {"2", null },
        new String[] {"5dui", null },
        new String[] {"990309", null },
        new String[] {"a", null },
        new String[] {"A5@359", null },
        new String[] {"aaa", null },
        new String[] {"accer", null },
        new String[] {"argh1", null },
        new String[] {"being lazy", null },
        new String[] {"bus, in hand", "CLASSIFIED/TRAVELLING" },
        new String[] {"bus, pocket", "CLASSIFIED/TRAVELLING" },
        new String[] {"cee", null },
        new String[] {"chain", null },
        new String[] {"CLASSIFIED/IDLE/SITTING", "CLASSIFIED/STATIONARY" },
        new String[] {"CLASSIFIED/UNKNOWN", null },
        new String[] {"CLASSIFIED/VEHICLE/BUS", "CLASSIFIED/TRAVELLING" },
        new String[] {"CLASSIFIED/WALKING/STAIRS/DOWN", "CLASSIFIED/WALKING" },
        new String[] {"CLASSIFIED/WALKING/STAIRS/UP", "CLASSIFIED/WALKING" },
        new String[] {"cos", null },
        new String[] {"ded", null },
        new String[] {"dejan", null },
        new String[] {"dipset", null },
        new String[] {"down stairs", "CLASSIFIED/WALKING" },
        new String[] {"drive", "CLASSIFIED/TRAVELLING" },
        new String[] {"driving, various speeds, mild bends", "CLASSIFIED/TRAVELLING" },
        new String[] {"driving. straight road, 50mph", "CLASSIFIED/TRAVELLING" },
        new String[] {"filip", null },
        new String[] {"g", null },
        new String[] {"ghost", null },
        new String[] {"gps", null },
        new String[] {"hanh", null },
        new String[] {"hey", null },
        new String[] {"holding a phone with my feet whilst balancing on a chair", null },
        new String[] {"james", null },
        new String[] {"kira", null },
        new String[] {"la", null },
        new String[] {"lamine", null },
        new String[] {"light", null },
        new String[] {"marco", null },
        new String[] {"Mattias", null },
        new String[] {"MD87 > Dataforce dance", null },
        new String[] {"meep", null },
        new String[] {"meh", null },
        new String[] {"moo", null },
        new String[] {"new", null },
        new String[] {"packing", null },
        new String[] {"passanger in car phone on lap", "CLASSIFIED/TRAVELLING" },
        new String[] {"pasys@arcor.de", null },
        new String[] {"phone stationary", null },
        new String[] {"putting phone on table or desk", null },
        new String[] {"q", null },
        new String[] {"roger1", null },
        new String[] {"s", null },
        new String[] {"seba", null },
        new String[] {"sensor", null },
        new String[] {"shaking phone pointlessly.", null },
        new String[] {"sheila vazquez", null },
        new String[] {"sitting", "CLASSIFIED/STATIONARY" },
        new String[] {"slog", null },
        new String[] {"sobota ranek", null },
        new String[] {"standing", "CLASSIFIED/STATIONARY" },
        new String[] {"standing still, phone in left inside jacket pocket", "CLASSIFIED/STATIONARY" },
        new String[] {"style", null },
        new String[] {"tesssst", null },
        new String[] {"test", null },
        new String[] {"test 1", null },
        new String[] {"test 2", null },
        new String[] {"test001", null },
        new String[] {"test01", null },
        new String[] {"tsssss", null },
        new String[] {"various activities for you to classify", null },
        new String[] {"w", null },
        new String[] {"walking", "CLASSIFIED/WALKING" },
        new String[] {"walking up stairs", "CLASSIFIED/WALKING" },
        new String[] {"walking with phone attached to belt", "CLASSIFIED/WALKING" },
        new String[] {"walking with phone in inside jacket pocket", "CLASSIFIED/WALKING" },
        new String[] {"walking with phone in left inside jacket pocket", "CLASSIFIED/WALKING" },
        new String[] {"walkingtest", "CLASSIFIED/WALKING" },
        new String[] {"yest", null },
        new String[] {"z", null },
        new String[] {"Zaid", null },
        new String[] {"zozoka", null },
    };

//    private static final String[][] ACTIVITY_FILTERS = new String[][] {
//        new String[] {",kirserg", null },
//        new String[] {"2", null },
//        new String[] {"5dui", null },
//        new String[] {"990309", null },
//        new String[] {"a", null },
//        new String[] {"A5@359", null },
//        new String[] {"aaa", null },
//        new String[] {"accer", null },
//        new String[] {"argh1", null },
//        new String[] {"being lazy", null },
//        new String[] {"bus, in hand", null },
//        new String[] {"bus, pocket", null },
//        new String[] {"cee", null },
//        new String[] {"chain", null },
//        new String[] {"CLASSIFIED/IDLE/SITTING", "CLASSIFIED/STATIONARY" },
//        new String[] {"CLASSIFIED/UNKNOWN", null },
//        new String[] {"CLASSIFIED/VEHICLE/BUS", "CLASSIFIED/TRAVELLING" },
//        new String[] {"CLASSIFIED/WALKING/STAIRS/DOWN", "CLASSIFIED/WALKING" },
//        new String[] {"CLASSIFIED/WALKING/STAIRS/UP", "CLASSIFIED/WALKING" },
//        new String[] {"cos", null },
//        new String[] {"ded", null },
//        new String[] {"dejan", null },
//        new String[] {"dipset", null },
//        new String[] {"down stairs", null },
//        new String[] {"drive", null },
//        new String[] {"driving, various speeds, mild bends", null },
//        new String[] {"driving. straight road, 50mph", null },
//        new String[] {"filip", null },
//        new String[] {"g", null },
//        new String[] {"ghost", null },
//        new String[] {"gps", null },
//        new String[] {"hanh", null },
//        new String[] {"hey", null },
//        new String[] {"holding a phone with my feet whilst balancing on a chair", null },
//        new String[] {"james", null },
//        new String[] {"kira", null },
//        new String[] {"la", null },
//        new String[] {"lamine", null },
//        new String[] {"light", null },
//        new String[] {"marco", null },
//        new String[] {"Mattias", null },
//        new String[] {"MD87 > Dataforce dance", null },
//        new String[] {"meep", null },
//        new String[] {"meh", null },
//        new String[] {"moo", null },
//        new String[] {"new", null },
//        new String[] {"packing", null },
//        new String[] {"passanger in car phone on lap", null },
//        new String[] {"pasys@arcor.de", null },
//        new String[] {"phone stationary", null },
//        new String[] {"putting phone on table or desk", null },
//        new String[] {"q", null },
//        new String[] {"roger1", null },
//        new String[] {"s", null },
//        new String[] {"seba", null },
//        new String[] {"sensor", null },
//        new String[] {"shaking phone pointlessly.", null },
//        new String[] {"sheila vazquez", null },
//        new String[] {"sitting", null },
//        new String[] {"slog", null },
//        new String[] {"sobota ranek", null },
//        new String[] {"standing", null },
//        new String[] {"standing still, phone in left inside jacket pocket", null },
//        new String[] {"style", null },
//        new String[] {"tesssst", null },
//        new String[] {"test", null },
//        new String[] {"test 1", null },
//        new String[] {"test 2", null },
//        new String[] {"test001", null },
//        new String[] {"test01", null },
//        new String[] {"tsssss", null },
//        new String[] {"various activities for you to classify", null },
//        new String[] {"w", null },
//        new String[] {"walking", null },
//        new String[] {"walking up stairs", null },
//        new String[] {"walking with phone attached to belt", null },
//        new String[] {"walking with phone in inside jacket pocket", null },
//        new String[] {"walking with phone in left inside jacket pocket", null },
//        new String[] {"walkingtest", null },
//        new String[] {"yest", null },
//        new String[] {"z", null },
//        new String[] {"Zaid", null },
//        new String[] {"zozoka", null },
//    };
    
    private static final Map<String,String> ACTIVITY_FILTER_MAP;

    static {
        Map<String,String> filter = new TreeMap<String, String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });

        for (String[] x:ACTIVITY_FILTERS) {
            String currentActivity = x[0];
            String newActivity = x[1];

            filter.put(currentActivity, newActivity);
        }

        ACTIVITY_FILTER_MAP = filter;
    }


    private static void showAllActivityNames(List<Activity> activities) {
        Set<String> activityNames = new TreeSet<String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });

        for (Activity activity:activities) {
            activityNames.add(activity.name);
        }

        for (String activityName:activityNames) {
            System.out.println("\t"+activityName);
        }
    }

    private static void generateCode(List<Activity> activities) {
        Set<String> activityNames = new TreeSet<String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });

        for (Activity activity:activities) {
            activityNames.add(activity.name);
        }

        for (String activityName:activityNames) {
            String classification = null;
            if (activityName.toLowerCase().contains("walking"))
                classification = "CLASSIFIED/WALKING";
            else
            if (activityName.toLowerCase().contains("stairs"))
                classification = "CLASSIFIED/WALKING";
            else
            if (activityName.toLowerCase().contains("bus"))
                classification = "CLASSIFIED/TRAVELLING";
            else
            if (activityName.toLowerCase().contains("car"))
                classification = "CLASSIFIED/TRAVELLING";
            else
            if (activityName.toLowerCase().contains("driv"))
                classification = "CLASSIFIED/TRAVELLING";
            else
            if (activityName.toLowerCase().contains("sitting"))
                classification = "CLASSIFIED/STATIONARY";
            else
            if (activityName.toLowerCase().contains("standing"))
                classification = "CLASSIFIED/STATIONARY";

            if (classification!=null)
                System.out.println("\tnew String[] {\""+activityName+"\", \""+classification+"\" },");
            else
                System.out.println("\tnew String[] {\""+activityName+"\", null },");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            List<Activity> activities = ActivityIO.read(new Scanner(Common.DIRTY_SQL_DATA));

            if (GENERATE_FILTERS) {
                generateCode(activities);
            } else {
                System.out.println("Current Activity Names:");
                showAllActivityNames(activities);

                ArrayList<Activity> filteredActivities = new ArrayList<Activity>(activities.size());

                for (Activity activity:activities) {
                    String currentName = activity.name;

                    if (ACTIVITY_FILTER_MAP.containsKey(currentName)) {
                        String newName = ACTIVITY_FILTER_MAP.get(currentName);

                        if (newName!=null) {
                            System.out.println("'"+currentName+"' >> '"+newName+"'");
                            filteredActivities.add(new Activity(newName, activity));
                        } else {
                            System.out.println("'"+currentName+"' filtered out.");
                        }
                    } else {
                        throw new RuntimeException("'"+currentName+"' missing in filters. Please update filters.");
                    }
                }

                System.out.println("Saving clean sql data to "+Common.CLEAN_SQL_DATA);
                PrintStream out = new PrintStream(Common.CLEAN_SQL_DATA);
                ActivityIO.write(out, filteredActivities);
                out.close();
                System.out.println("Activities Written: "+filteredActivities.size());
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

}
