/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.utils;

/**
 *
 * @author oka
 */
public class OutlierAnalyser {
    private boolean countAbove, countBelow;
    private boolean countGroups;
    private double[] samples;
    private double maxAbove, maxBelow;
    private int size;
    private double mean;
    private double stddev;

    public OutlierAnalyser(boolean countAbove, boolean countBelow, boolean countGroups) {
        this.countAbove = countAbove;
        this.countBelow = countBelow;
        this.countGroups = countGroups;

        if (!countAbove && !countBelow) {
            throw new RuntimeException("Can't function without counting either above or below mean");
        }
    }
    
    public void assign(double[] input, int size) {
        this.samples = input;
        this.size = size;
        this.maxAbove = Double.NEGATIVE_INFINITY;
        this.maxBelow = Double.POSITIVE_INFINITY;

        mean = 0.0;
        for (int i=0; i<size; ++i) {
            mean += samples[i];

            if (samples[i]>=0.0 && samples[i]>this.maxAbove)
                this.maxAbove = samples[i];
            if (samples[i]<=0.0 && samples[i]<this.maxBelow)
                this.maxBelow = samples[i];
        }
        mean /= size;

        this.maxBelow = -this.maxBelow;

        stddev = 0.0;
        for (int i=0; i<size; ++i)
            stddev += (samples[i]-mean)*(samples[i]-mean);
        stddev /= size;
        stddev = Math.sqrt(stddev);
    }

    public double getMean() {
        return mean;
    }

    public double getStddev() {
        return stddev;
    }
    
    public int getCountBeyondMean(double deviation) {
        int count = 0;
        boolean prevHad = false;
        for (int i=0; i<size; ++i) {
            if (   (countBelow && samples[i]<mean-deviation) ||
                    (countAbove && samples[i]>mean+deviation)
                )
            {
                if (countGroups==false || prevHad==false) {
                    ++count;
                }
                prevHad = true;
            } else {
                prevHad = false;
            }
        }
        return count;
    }

    public int getCountBeyondStdDev(double magnitude) {
        return getCountBeyondMean(stddev*magnitude);
    }

    public double[] secludeBeyondMean(double deviation, double assignToOtherValues) {
        double[] values = new double[size];
        for (int i=0; i<size; ++i)
            if ((countBelow && samples[i]<mean-deviation) ||
                (countAbove && samples[i]>mean+deviation)) {
                values[i] = samples[i];
            } else {
                values[i] = assignToOtherValues;
            }
        return values;
    }

    public double[] secludeBeyondStdDev(double magnitude, double assignToOtherValues) {
        return secludeBeyondMean(stddev*magnitude, assignToOtherValues);
    }

    /**
     *
     * @param magLo
     * low magnitude starting point, high count
     *
     * @param magHi
     * high magnitude starting point, low count
     *
     * @param reqLoRange
     * @param reqHiRange
     * @return
     */
    public double getMagnitudeWith(double magLo, double magHi, int reqLoRange, int reqHiRange) {
        int     magLoCount = getCountBeyondStdDev(magLo),
                magHiCount = getCountBeyondStdDev(magHi);
        boolean found = false;
        
        if (reqHiRange<magLoCount || reqLoRange>magHiCount) {
            System.out.println("ERROR: Invalid search range: ["+reqLoRange+".."+reqHiRange+"], data has ["+magLoCount+".."+magHiCount+"]");
            return Double.NaN;
        }

        do {
            double magMid = (magLo+magHi)/2.0;
            int magMidCount = getCountBeyondStdDev(magMid);
            if (magMidCount<reqLoRange) {
                magHi = magMid;
                magHiCount = magMidCount;
            } else
                if (magMidCount>reqHiRange) {
                    magLo = magMid;
                    magLoCount = magMidCount;
                } else {
                    return magMid;
                }
        } while (!found);

        return Double.NaN;
    }

    public double getMagnitudeWith(int reqLoRange, int reqHiRange) {
        double magHi;

        if (countAbove && countBelow)
            magHi = Math.max(maxAbove,maxBelow);
        else
            if (countAbove)
                magHi = maxAbove;
            else
                if (countBelow)
                    magHi = maxBelow;
                else
                    magHi = 0.0;

        return getMagnitudeWith(0.0, magHi, reqLoRange, reqHiRange);
    }
}
