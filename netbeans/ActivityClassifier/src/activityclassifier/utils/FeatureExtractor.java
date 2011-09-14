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
public class FeatureExtractor {

    public static final int NUM_FEATURES = 6;
    public static final String FEATURE_NAMES[] = new String[] {
        "HOR RANGE",
        "VER RANGE",
        "HOR MEAN",
        "VER MEAN",
        "HOR STD DEV",
        "VER STD DEV"
    };

    public static final int FEATURE_HOR_RANGE       = 0;
    public static final int FEATURE_VER_RANGE       = 1;
    public static final int FEATURE_HOR_MEAN        = 2;
    public static final int FEATURE_VER_MEAN        = 3;
    public static final int FEATURE_HOR_STDDEV      = 4;
    public static final int FEATURE_VER_STDDEV      = 5;

    private int windowSize;
    private RotateSamplesToVerticalHorizontal rotate;
    private float[][] samples;
    private float[][] twoDimSamples;
    private CalcStatistics twoDimSampleStats;
    private CalcStatistics threeDimSampleStats;
    private float[] features;

    public FeatureExtractor(int windowSize) {
        this.windowSize = windowSize;

        this.rotate = new RotateSamplesToVerticalHorizontal();
        this.samples = new float[windowSize][3];
        this.twoDimSamples = new float[windowSize][2];
        this.twoDimSampleStats = new CalcStatistics(2);
        this.threeDimSampleStats = new CalcStatistics(3);
        this.features = new float[NUM_FEATURES];
    }
    
//    synchronized
//    public float[] extractUnrotated(float[][] input, int windowStart)
//    {
//        if (windowStart+windowSize>input.length) {
//            System.out.println("WARNING: attempting to extract features past " +
//                    "the end of samples (windowStart="+windowStart+", size="+samples.length+")");
//            return null;
//        }
//
//        for (int j=0; j<windowSize; ++j) {
//            samples[j][0] = input[windowStart+j][0];
//            samples[j][1] = input[windowStart+j][1];
//            samples[j][2] = input[windowStart+j][2];
//        }
//
//        threeDimSampleStats.assign(samples, windowSize);
//
//        float[] min = threeDimSampleStats.getMin();
//        float[] max = threeDimSampleStats.getMax();
//        float[] mean = threeDimSampleStats.getMean();
//        float[] stddev = threeDimSampleStats.getStandardDeviation();
//
//        features[FEATURE_HOR_RANGE] = max[0] - min[0];  //x
//        features[FEATURE_VER_RANGE] = max[1] - min[1];  //y
//        features[FEATURE_HOR_MEAN] = mean[0];           //x
//        features[FEATURE_VER_MEAN] = mean[1];           //y
//        features[FEATURE_HOR_STDDEV] = stddev[0];       //x
//        features[FEATURE_VER_STDDEV] = stddev[1];       //y
//
////        System.out.println("min="+Arrays.toString(min));
////        System.out.println("max="+Arrays.toString(max));
//
//        return features;
//    }
    
    
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

    private float[] internExtract() {
        twoDimSampleStats.assign(twoDimSamples, windowSize);

        float[] min = twoDimSampleStats.getMin();
        float[] max = twoDimSampleStats.getMax();
        float[] mean = twoDimSampleStats.getMean();
        float[] stddev = twoDimSampleStats.getStandardDeviation();

        features[FEATURE_HOR_RANGE] = max[0] - min[0];
        features[FEATURE_VER_RANGE] = max[1] - min[1];
        features[FEATURE_HOR_MEAN] = mean[0];
        features[FEATURE_VER_MEAN] = mean[1];
        features[FEATURE_HOR_STDDEV] = stddev[0];
        features[FEATURE_VER_STDDEV] = stddev[1];

//        System.out.println("min="+Arrays.toString(min));
//        System.out.println("max="+Arrays.toString(max));

        return features;
    }
    
}
