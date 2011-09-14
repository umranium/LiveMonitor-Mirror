package activityclassifier.utils;

/**
 * An object of class CalcStatistics can be used to compute several simple
 * statistics for a set of numbers. Numbers are passed in a 3D array of X,Y,Z.
 * Methods are provided to return the following statistics for the set of
 * numbers that have been entered: The number of items, the sum of the items,
 * the average, the standard deviation, the maximum, the minimum and
 * VerticalAccel. The vertical acceleration would normally equal gravity. If
 * higher than gravity then device was accelerated during the sampling interval
 * most likely in a car. If it is less than gravity then device was rotated
 * during sampling.
 * 
 * @author Ken Taylor
 */
public class CalcStatistics {

    /**
     * Number of dimensions
     */
    private final int dimensions;
    /**
     * Number of numbers in the array.
     */
    private int count;
    /**
     * The sum of all the items in the array.
     */
    private double sum[];
    /**
     * The sum of the squares of all the items.
     */
    private double sum_sqr[];
    /**
     * Largest item seen.
     */
    private float max[];
    /**
     * Smallest item seen.
     */
    private float min[];
    /**
     * The mean of the array data.
     */
    private float mean[];

    /**
     * ArrayIn is a 3D array i.e. X,Y,Z of a number of samples Summarise array
     * passed in
     *
     * @param arrayIn
     *            array passed in
     * @param samples
     *            sample size of one dimension
     */
    public CalcStatistics(int dimensions) {
        this.dimensions = dimensions;

        this.sum = new double[dimensions];
        this.sum_sqr = new double[dimensions];
        this.min = new float[dimensions];
        this.max = new float[dimensions];
        this.mean = new float[dimensions];
    }

    public void assign(float[][] arrayIn, int start, int samples) {
        count = samples;

        for (int i = 0; i < dimensions; ++i) {
            sum[i] = 0.0;
            sum_sqr[i] = 0.0;
            min[i] = Float.POSITIVE_INFINITY;
            max[i] = Float.NEGATIVE_INFINITY;
            mean[i] = 0.0f;
        }

        // step through array in groups of 3
        for (int i = 0; i < samples; ++i) {

            for (int j = 0; j < dimensions; j++) {
                float val = arrayIn[start + i][j];
                sum[j] += val;
                sum_sqr[j] += val * val;
                if (val > max[j]) {
                    max[j] = val;
                }
                if (val < min[j]) {
                    min[j] = val;
                }
            }
        }
        for (int j = 0; j < dimensions; j++) {
            mean[j] = (float) (sum[j] / samples);
        }

    }

    public void assign(float[][] arrayIn, int samples) {
        assign(arrayIn, 0, samples);
    }

    /**
     *
     * @return number of items in array as passed in.
     */
    public int getCount() {
        return count;
    }

    /**
     *
     * @return the sum of all the items that have been entered.
     */
    public double[] getSum() {
        return sum;
    }

    /**
     *
     * @return average of all the items that have been entered. Value is
     *         Float.NaN if count == 0.
     */
    public float[] getMean() {
        return mean;
    }

    /**
     *
     * @return
     */
    public float getVerticalAccel() {
        float verticalAccel = 0;
        for (int j = 0; j < dimensions; j++) {
            verticalAccel += mean[j] * mean[j];
        }
        verticalAccel = (float) Math.sqrt(verticalAccel);
        return verticalAccel;
    }

    /**
     *
     * @return standard deviation of all the items that have been entered. Value
     *         will be Double.NaN if count == 0.
     */
    public float[] getStandardDeviation() {
        float standardDeviation[] = new float[dimensions];
        for (int j = 0; j < dimensions; j++) {
            standardDeviation[j] = (float) Math.sqrt(sum_sqr[j] / count - mean[j] * mean[j]);
        }
        return standardDeviation;
    }

    /**
     *
     * @return the smallest item that has been entered. Value will be - infinity
     *         if no items in array.
     */
    public float[] getMin() {
        return min;
    }

    /**
     * 
     * @return the largest item that has been entered. Value will be -infinity
     *         if no items have been entered.
     */
    public float[] getMax() {
        return max;
    }

    /**
     * Computes the magnitude of a vector.
     *
     * @param vec
     * A vector of dimensions as given when the instance is constructed
     * using {@link #CalcStatistics(int)}
     *
     * @return
     * the magnitude of the vector
     */
    public float calcMag(float[] vec) {
        return calcMag(dimensions, vec);
    }

    /**
     * Creates a vector of the dimensions given when this instance is
     * initialised.
     *
     * @return
     * A vector of given dimensions
     */
    public float[] createVector() {
        return new float[dimensions];
    }

    public static float calcMag(int dimensions, float[] vec) {
        double mag = 0.0f;
        for (int i = 0; i < dimensions; ++i) {
            mag += vec[i] * vec[i];
        }
        return (float) Math.sqrt(mag);
    }

    public static void normalize(int dimensions, float[] vec) {
        float length = calcMag(dimensions, vec);
        if (length != 0) {
            for (int i = 0; i < dimensions; ++i) {
                vec[i] /= length;
            }
        }
    }
}
