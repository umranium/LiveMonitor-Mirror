package au.urremote.bridge.service.filters.median;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.location.Location;

public class MedianFilter {
	
	private class Loc {
		Location loc;
		boolean tainted;
		boolean outputAlready;
		
		public Loc(Location loc) {
			this.loc = loc;
			this.tainted = false;
			this.outputAlready = false;
		}
	}
	
	private class Speed {
		double speed;
		Loc src1, src2;
		
		public Speed(double speed, Loc src1, Loc src2) {
			this.speed = speed;
			this.src1 = src1;
			this.src2 = src2;
		}
	}
		
	private final int numSpeedsComparison;
	private final int middle;
	private ArrayList<Speed> speeds;
	private ArrayList<Speed> sortedSpeeds;
	private ArrayList<Loc> locations;
	private FilterOut filterOut;
	private int lastCheck;
	
	private Comparator<Speed> sortBySpeed = new Comparator<Speed>() {

		@Override
		public int compare(Speed object1, Speed object2) {
			return Double.compare(object1.speed, object2.speed);
		}
		
	};
	
	public MedianFilter(int numSpeedsComparison, FilterOut filterOut) {
		//	make sure we have odd number of speed comparisons
		if (numSpeedsComparison%2==0) ++numSpeedsComparison;
		
		this.numSpeedsComparison = numSpeedsComparison;
		this.middle = numSpeedsComparison/2+1;
		this.speeds = new ArrayList<Speed>(numSpeedsComparison);
		this.sortedSpeeds = new ArrayList<Speed>(numSpeedsComparison);
		this.locations = new ArrayList<Loc>(numSpeedsComparison+1);
		this.filterOut = filterOut;
		this.lastCheck = 0;
	}
	
	public void add(Location gpsLoc) {
		Loc loc = new Loc(gpsLoc);
		if (locations.size()>0) {
			Loc lastLoc = locations.get(locations.size()-1); 
			double dist = Math.abs(loc.loc.distanceTo(lastLoc.loc)) / 1000.0;//in km
			double time = (double)(loc.loc.getTime() - lastLoc.loc.getTime()) / 3600.0;// in hrs
			Speed sp = new Speed(dist/time, lastLoc, loc);
			speeds.add(sp);
			sortedSpeeds.add(sp);
		}
		locations.add(loc);
		
		if (speeds.size()>numSpeedsComparison) {
			locations.remove(0);
			Speed sp = speeds.remove(0);
			sortedSpeeds.remove(sp);
		}
		
		if (speeds.size()==numSpeedsComparison) {
			Collections.sort(sortedSpeeds, sortBySpeed);
			Speed medianSpeed = sortedSpeeds.get(middle);
			Speed sp = speeds.get(middle);
			if (sp!=medianSpeed) {
				sp.src1.tainted = true;
				sp.src2.tainted = true;
			}

			for (int i=lastCheck; i<middle; ++i) {
				Loc l = locations.get(i);
				if (!l.tainted && !l.outputAlready) {
					filterOut.receive(l.loc);
					l.outputAlready = true;
				}
			}
			lastCheck = middle - 1;
		}
	}

}
