/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.utils;

import activityclassifier.utils.CalcStatistics;
import activityclassifier.utils.RotateSamplesToVerticalHorizontal;
import java.util.Arrays;

/**
 *
 * @author oka
 */
public class AllFeatureExtractor {

    public static final int NUM_FEATURES = 8;
    public static final String FEATURE_NAMES[] = new String[] {
        "HOR RANGE",
        "VER RANGE",
        "HOR MEAN",
        "VER MEAN",
        "HOR STD DEV",
        "VER STD DEV",
        "HOR ENERGY",
        "VER ENERGY"
    };

    public static final int FEATURE_HOR_RANGE       = 0;
    public static final int FEATURE_VER_RANGE       = 1;
    public static final int FEATURE_HOR_MEAN        = 2;
    public static final int FEATURE_VER_MEAN        = 3;
    public static final int FEATURE_HOR_STDDEV      = 4;
    public static final int FEATURE_VER_STDDEV      = 5;
    public static final int FEATURE_HOR_ENERGY      = 6;
    public static final int FEATURE_VER_ENERGY      = 7;

    private int windowSize;
    private RotateSamplesToVerticalHorizontal rotate;
    private float[][] samples;
    private float[][] twoDimSamples;
    private CalcStatistics sampleStats;
    private float[] features;

    public AllFeatureExtractor(int windowSize) {
        this.windowSize = windowSize;

        this.rotate = new RotateSamplesToVerticalHorizontal();
        this.samples = new float[windowSize][3];
        this.twoDimSamples = new float[windowSize][2];
        this.sampleStats = new CalcStatistics(2);
        this.features = new float[NUM_FEATURES];
    }

    synchronized
    public float[] extractUnrotated(float[][] input, int windowStart)
    {
        if (windowStart+windowSize>input.length) {
            System.out.println("WARNING: attempting to extract features past " +
                    "the end of samples (windowStart="+windowStart+", size="+samples.length+")");
            return null;
        }

        for (int j=0; j<windowSize; ++j) {
            samples[j][0] = input[windowStart+j][0];
            samples[j][1] = input[windowStart+j][1];
            samples[j][2] = input[windowStart+j][2];
        }

        if (!rotate.rotateToWorldCoordinates(samples)) {
//            System.out.println("WARNING: Unable to rotate samples)");
            return null;
        }

        float gravity = rotate.getGravity();

        for (int j=0; j<windowSize; ++j) {
            twoDimSamples[j][0] = (float)Math.sqrt(
                    samples[j][0]*samples[j][0] +
                    samples[j][1]*samples[j][1]);
//            twoDimSamples[j][1] = samples[j][2] - gravity;
            twoDimSamples[j][1] = samples[j][2];
        }

        return internExtract();
    }

    synchronized
    public float[] extractRotated(float[][] input, int windowStart)
    {
        if (windowStart+windowSize>input.length) {
            System.out.println("WARNING: attempting to extract features past " +
                    "the end of samples (windowStart="+windowStart+", size="+samples.length+")");
            return null;
        }

        for (int j=0; j<windowSize; ++j) {
            twoDimSamples[j][0] = (float)Math.sqrt(
                    input[j][0]*input[j][0] +
                    input[j][1]*input[j][1]);
            twoDimSamples[j][1] = input[j][2];
        }

        return internExtract();
    }

    private float[] internExtract() {
        sampleStats.assign(twoDimSamples, windowSize);

        float[] min = sampleStats.getMin();
        float[] max = sampleStats.getMax();
        float[] mean = sampleStats.getMean();
        float[] stddev = sampleStats.getStandardDeviation();

        float[] energy = new float[2];
        for (int i=1; i<windowSize; ++i) {
            float val;

            val = (twoDimSamples[i][0]-twoDimSamples[i-1][0]);
            energy[0] += Math.hypot(val, 0.05);

            val = (twoDimSamples[i][1]-twoDimSamples[i-1][1]);
            energy[1] += Math.hypot(val, 0.05);
        }


        features[FEATURE_HOR_RANGE] = max[0] - min[0];
        features[FEATURE_VER_RANGE] = max[1] - min[1];
        features[FEATURE_HOR_MEAN] = mean[0];
        features[FEATURE_VER_MEAN] = mean[1];
        features[FEATURE_HOR_STDDEV] = stddev[0];
        features[FEATURE_VER_STDDEV] = stddev[1];
        features[FEATURE_HOR_ENERGY] = energy[0];
        features[FEATURE_VER_ENERGY] = energy[1];
        
//        System.out.println("min="+Arrays.toString(min));
//        System.out.println("max="+Arrays.toString(max));

        return features;
    }

}
