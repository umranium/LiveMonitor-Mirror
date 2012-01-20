package com.urremote.bridge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.util.Log;

import com.urremote.bridge.common.Constants;
import com.urremote.bridge.mapmymaps.ActivityType;
import com.urremote.bridge.mapmymaps.MapMyMapsException;
import com.urremote.bridge.mapmymaps.MapMyTracksInterfaceApi;

public class LatencyTestThread extends Thread {
	
	public static final int TEST_COUNTS = 20;
	
	boolean shouldRun = true;
	
	public LatencyTestThread() {
		super("LatencyTestThread");
	}
	
	public void quit() {
		shouldRun = false;
	}
	
	@Override
	public void run() {
        MapMyTracksInterfaceApi api = new MapMyTracksInterfaceApi(0, "umran", "?password123");
        List<Location> locations = new ArrayList<Location>();
        
        Location here = new Location("gps");
        
        //-35.245894,149.12473
        here.setTime(System.currentTimeMillis());
        here.setLatitude(-35.245894);
        here.setLongitude(149.12473);
        
        locations.add(here);
        
        ArrayList<Long> latency = new ArrayList<Long>();
        
        try {
            for (int attempt=1; attempt<=TEST_COUNTS && shouldRun; ++attempt) {
                long start = System.currentTimeMillis();
                try {
                	long latestStartAttemptTime = api.getServerTime();
                	Log.d(Constants.TAG, "Attempting to start new activity, time="+latestStartAttemptTime);
                    long activityId = api.startActivity(
                    		"activityABDE",
                    		",test",
                    		true,
                    		ActivityType.CYCLING,
                    		locations);
                    Log.d(Constants.TAG, "SUCCESS: Activity ID = "+activityId);
                    /*
                    List<ActivityDetails> activities = api.getActivities("umran");
                    boolean found = false;
                    for (ActivityDetails ad:activities) {
                        if (ad.id==activityId) {
                            found = true;
                            break;
                        }
                    }
                    Log.d(Constants.TAG, "Activity Found = "+found);
                    */
                } catch (MapMyMapsException ex) {
                	Log.e(Constants.TAG, "Error", ex);
                } catch (IOException ex) {
                	Log.e(Constants.TAG, "Error", ex);
                }
                long stop = System.currentTimeMillis();

                latency.add(stop-start);

                Log.d(Constants.TAG, "\tIT TOOK: "+(stop-start));
            }

            Log.d(Constants.TAG, "Durations:");
            for (Long lat:latency) {
            	Log.d(Constants.TAG, lat.toString());
            }
        } catch (Exception e) {
        	Log.e(Constants.TAG, "Error", e);
        }
	}
	
}