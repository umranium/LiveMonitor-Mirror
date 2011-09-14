package activityclassifier.utils;


import java.util.Arrays;

/**
 * 
 * An object of class RotateSamplesToVerticalHorizontal will rotate a set 
 * of sampled accelerometer data to vertical axis Z dominant horizontal 
 * axis Y and minor horizontal axis Y.  Numbers are passed in a 3D array 
 * of X,Y,Z. Methods are provided to return: 
 * The rotated array.
 *  
 * @author Ken Taylor
 */
public class RotateSamplesToVerticalHorizontal {

    // number of dimensions
    public final static int DIM = 3;
    private final static int X_AXIS = 0;
    private final static int Y_AXIS = 1;
    private final static int Z_AXIS = 2;

    //	gravity vector in use
    private float[] gravityVec = new float[DIM];
    //	derived horizontal vector
    private float[] horizontalVec = new float[DIM];
    //	computed rotation matrix
    private float[] rotationMat = new float[DIM * DIM];
    //	a temporary vector to hold values while doing matrix multiplication
    private float[] tempVec = new float[DIM];

    public float[] getRotationMat() {
        return rotationMat;
    }

    public float[] getGravityVec() {
        return gravityVec;
    }

    public float getGravity() {
        return calcMag(gravityVec);
    }

    /**
     * Rotates the accelerometer samples to world coordinates,
     * using a gravity vector derived from the same samples.
     *
     * Note that, a horizontal vector is then derived from the
     * gravity vector, which is used to rotate the samples to the
     * world coordinates. This makes the final samples' direction-less
     * and hence only the magnitude of the horizontal component should
     * be used.
     *
     * Use {@link #rotateToWorldCoordinates(float[], float[])}
     * in order to keep the direction of the horizontal component
     * of the sampled vectors relative to magnetic north.
     *
     * @param samples
     * The samples to convert to world coordinates. The array will
     * contain the world coordinates upon return. Unless the
     * function returns false, which means that the samples haven't
     * been altered.
     *
     * @return
     * Returns false if the function is unable to compute the rotation
     * matrix and hence unable to change the samples to world coordinates.
     */
    public synchronized boolean rotateToWorldCoordinates(float[][] samples) {
        computeMeanVector(samples, gravityVec);

//        System.out.print("\tgravity vec:"+Arrays.toString(gravityVec));

//        System.out.println("Gravity Vec="+vec2str(gravityVec)+" = "+calcMag(gravityVec));
//		Log.v("TEST", "Gravity Vec="+vec2str(gravityVec)+" = "+calcMag(gravityVec));

        convertToHorVec(gravityVec, horizontalVec);


//		Log.v("TEST", "Hor Vec="+vec2str(horizontalVec)+" = "+calcMag(horizontalVec));

        return internRotateToWorldCoordinates(samples, gravityVec, horizontalVec);
    }

    /**
     *
     * Rotates the accelerometer samples to world coordinates,
     * using a gravity vector derived from the same samples,
     * and the given geo-magnetic vector.
     *
     * Note that, the geo-magnetic vector is used as the horizontal
     * component to compute the rotation matrix, which is used to
     * rotate the samples to world coordinates. This makes the
     * final samples' direction relative to magnetic-north.
     *
     * Use {@link #rotateToWorldCoordinates(float[])}
     * if the horizontal direction is not required, or if a
     * geo-magnetic vector can not be obtained.
     *
     * @param samples
     * The samples to convert to world coordinates. The array will
     * contain the world coordinates upon return. Unless the
     * function returns false, which means that the samples haven't
     * been altered.
     *
     * @param geoMagSamples
     * Geo-magnetic vector to use in obtaining the rotation matrix.
     *
     * @return
     * Returns false if the function is unable to compute the rotation
     * matrix and hence unable to change the samples to world coordinates.
     *
     */
    public synchronized boolean rotateToWorldCoordinates(float[][] samples, float[][] geoMagSamples) {
        computeMeanVector(samples, gravityVec);

        computeMeanVector(geoMagSamples, horizontalVec);

        return internRotateToWorldCoordinates(samples, gravityVec, horizontalVec);
    }

    /**
     * Computes the inclination matrix <b>I</b> as well as the rotation
     * matrix <b>R</b> transforming a vector from the
     * device coordinate system to the world's coordinate system which is
     * defined as a direct orthonormal basis, where:
     *
     * <li>X is defined as the vector product <b>Y.Z</b> (It is tangential to
     * the ground at the device's current location and roughly points East).</li>
     * <li>Y is tangential to the ground at the device's current location and
     * points towards the magnetic North Pole.</li>
     * <li>Z points towards the sky and is perpendicular to the ground.</li>
     * <p>
     * <hr>
     * <p>By definition:
     * <p>[0 0 g] = <b>R</b> * <b>gravity</b> (g = magnitude of gravity)
     * <p>[0 m 0] = <b>I</b> * <b>R</b> * <b>geomagnetic</b>
     * (m = magnitude of geomagnetic field)
     * <p><b>R</b> is the identity matrix when the device is aligned with the
     * world's coordinate system, that is, when the device's X axis points
     * toward East, the Y axis points to the North Pole and the device is facing
     * the sky.
     *
     * <p><b>I</b> is a rotation matrix transforming the geomagnetic
     * vector into the same coordinate space as gravity (the world's coordinate
     * space). <b>I</b> is a simple rotation around the X axis.
     * The inclination angle in radians can be computed with
     * {@link #getInclination}.
     * <hr>
     *
     * <p> Each matrix is returned either as a 3x3 or 4x4 row-major matrix
     * depending on the length of the passed array:
     * <p><u>If the array length is 16:</u>
     * <pre>
     *   /  M[ 0]   M[ 1]   M[ 2]   M[ 3]  \
     *   |  M[ 4]   M[ 5]   M[ 6]   M[ 7]  |
     *   |  M[ 8]   M[ 9]   M[10]   M[11]  |
     *   \  M[12]   M[13]   M[14]   M[15]  /
     *</pre>
     * This matrix is ready to be used by OpenGL ES's
     * {@link javax.microedition.khronos.opengles.GL10#glLoadMatrixf(float[], int)
     * glLoadMatrixf(float[], int)}.
     * <p>Note that because OpenGL matrices are column-major matrices you must
     * transpose the matrix before using it. However, since the matrix is a
     * rotation matrix, its transpose is also its inverse, conveniently, it is
     * often the inverse of the rotation that is needed for rendering; it can
     * therefore be used with OpenGL ES directly.
     * <p>
     * Also note that the returned matrices always have this form:
     * <pre>
     *   /  M[ 0]   M[ 1]   M[ 2]   0  \
     *   |  M[ 4]   M[ 5]   M[ 6]   0  |
     *   |  M[ 8]   M[ 9]   M[10]   0  |
     *   \      0       0       0   1  /
     *</pre>
     * <p><u>If the array length is 9:</u>
     * <pre>
     *   /  M[ 0]   M[ 1]   M[ 2]  \
     *   |  M[ 3]   M[ 4]   M[ 5]  |
     *   \  M[ 6]   M[ 7]   M[ 8]  /
     *</pre>
     *
     * <hr>
     * <p>The inverse of each matrix can be computed easily by taking its
     * transpose.
     *
     * <p>The matrices returned by this function are meaningful only when the
     * device is not free-falling and it is not close to the magnetic north.
     * If the device is accelerating, or placed into a strong magnetic field,
     * the returned matrices may be inaccurate.
     *
     * @param R is an array of 9 floats holding the rotation matrix <b>R</b>
     * when this function returns. R can be null.<p>
     * @param I is an array of 9 floats holding the rotation matrix <b>I</b>
     * when this function returns. I can be null.<p>
     * @param gravity is an array of 3 floats containing the gravity vector
     * expressed in the device's coordinate. You can simply use the
     * {@link android.hardware.SensorEvent#values values}
     * returned by a {@link android.hardware.SensorEvent SensorEvent} of a
     * {@link android.hardware.Sensor Sensor} of type
     * {@link android.hardware.Sensor#TYPE_ACCELEROMETER TYPE_ACCELEROMETER}.<p>
     * @param geomagnetic is an array of 3 floats containing the geomagnetic
     * vector expressed in the device's coordinate. You can simply use the
     * {@link android.hardware.SensorEvent#values values}
     * returned by a {@link android.hardware.SensorEvent SensorEvent} of a
     * {@link android.hardware.Sensor Sensor} of type
     * {@link android.hardware.Sensor#TYPE_MAGNETIC_FIELD TYPE_MAGNETIC_FIELD}.
     * @return
     *   true on success<p>
     *   false on failure (for instance, if the device is in free fall).
     *   On failure the output matrices are not modified.
     */

    private static boolean getRotationMatrix(float[] R, float[] I,
            float[] gravity, float[] geomagnetic) {
        // TODO: move this to native code for efficiency
        float Ax = gravity[0];
        float Ay = gravity[1];
        float Az = gravity[2];
        final float Ex = geomagnetic[0];
        final float Ey = geomagnetic[1];
        final float Ez = geomagnetic[2];
        float Hx = Ey*Az - Ez*Ay;
        float Hy = Ez*Ax - Ex*Az;
        float Hz = Ex*Ay - Ey*Ax;
        final float normH = (float)Math.sqrt(Hx*Hx + Hy*Hy + Hz*Hz);
        if (normH < 0.1f) {
            // device is close to free fall (or in space?), or close to
            // magnetic north pole. Typical values are  > 100.
            return false;
        }
        final float invH = 1.0f / normH;
        Hx *= invH;
        Hy *= invH;
        Hz *= invH;
        final float invA = 1.0f / (float)Math.sqrt(Ax*Ax + Ay*Ay + Az*Az);
        Ax *= invA;
        Ay *= invA;
        Az *= invA;
        final float Mx = Ay*Hz - Az*Hy;
        final float My = Az*Hx - Ax*Hz;
        final float Mz = Ax*Hy - Ay*Hx;
        if (R != null) {
            if (R.length == 9) {
                R[0] = Hx;     R[1] = Hy;     R[2] = Hz;
                R[3] = Mx;     R[4] = My;     R[5] = Mz;
                R[6] = Ax;     R[7] = Ay;     R[8] = Az;
            } else if (R.length == 16) {
                R[0]  = Hx;    R[1]  = Hy;    R[2]  = Hz;   R[3]  = 0;
                R[4]  = Mx;    R[5]  = My;    R[6]  = Mz;   R[7]  = 0;
                R[8]  = Ax;    R[9]  = Ay;    R[10] = Az;   R[11] = 0;
                R[12] = 0;     R[13] = 0;     R[14] = 0;    R[15] = 1;
            }
        }
        if (I != null) {
            // compute the inclination matrix by projecting the geomagnetic
            // vector onto the Z (gravity) and X (horizontal component
            // of geomagnetic vector) axes.
            final float invE = 1.0f / (float)Math.sqrt(Ex*Ex + Ey*Ey + Ez*Ez);
            final float c = (Ex*Mx + Ey*My + Ez*Mz) * invE;
            final float s = (Ex*Ax + Ey*Ay + Ez*Az) * invE;
            if (I.length == 9) {
                I[0] = 1;     I[1] = 0;     I[2] = 0;
                I[3] = 0;     I[4] = c;     I[5] = s;
                I[6] = 0;     I[7] =-s;     I[8] = c;
            } else if (I.length == 16) {
                I[0] = 1;     I[1] = 0;     I[2] = 0;
                I[4] = 0;     I[5] = c;     I[6] = s;
                I[8] = 0;     I[9] =-s;     I[10]= c;
                I[3] = I[7] = I[11] = I[12] = I[13] = I[14] = 0;
                I[15] = 1;
            }
        }
        return true;
    }
    
    private boolean internRotateToWorldCoordinates(float[][] samples, float[] gravityVec, float[] horVec) {
//		Log.v("TEST", "Hor Vec="+vec2str(horizontalVec)+" = "+calcMag(horizontalVec));



        if (!getRotationMatrix(rotationMat, null, gravityVec, horVec)) {
            Vector3f g = new Vector3f(gravityVec[0], gravityVec[1], gravityVec[2]);
            g = g.normalize();
            Vector3f h = new Vector3f(horizontalVec[0], horizontalVec[1], horizontalVec[2]);
            h = h.normalize();

            float angle = 180.0f * g.angleBetween(h) / (float)Math.PI;
            System.out.println("hor vec almost par to gravity! gravity vec:"+Arrays.toString(gravityVec)+", "+Arrays.toString(horizontalVec)+" angle="+angle);
            //	sometimes fails, according to the api
            return false;
        }

//        float gravity = calcMag(gravityVec);
//        if (Math.abs(gravity-9.81)>9.81) {
//            System.out.println("\t\ttoo far from gravity "+gravity);
//            return false;
//        }

        //	apply to current samples
        applyRotation(samples);
        /*
        CalcStatistics st = new CalcStatistics(samples, samples.length/3);
        float[] min = st.getMin();
        float[] max = st.getMax();
        float[] mean = st.getMean();
        float[] sd = st.getStandardDeviation();
        if (	Math.abs(max[0]-min[0])>1.0 ||
        Math.abs(max[1]-min[1])>1.0 ||
        Math.abs(max[2]-min[2])>1.0	) {
        for (int i=0; i<samples.length; i+=DIM) {
        Log.v("TEST", "sample="+vec2str(samples,i));
        }
        Log.v("TEST", "min="+vec2str(min)+", max="+vec2str(max)+", mean="+vec2str(mean)+", s.d.="+vec2str(sd));
        }
         */
        return true;
    }

    private static float calcMag(float[] vec) {
        double mag = 0.0f;
        for (int i = 0; i < DIM; ++i) {
            mag += vec[i] * vec[i];
        }
        return (float) Math.sqrt(mag);
    }

    /**
     * Applies the current rotation matrix to the samples
     *
     * @param samples
     * samples to apply the current rotation matrix to
     * index:		[ 0 ][ 1 ][ 2 ][ 3 ][ 4 ][ 5 ]...
     * dimension:   [ x ][ y ][ z ][ x ][ y ][ z ]...
     *
     * rotation matrix:
     * [ 0 ][ 1 ][ 2 ]
     * [ 3 ][ 4 ][ 5 ]
     * [ 6 ][ 7 ][ 8 ]
     *
     */
    private void applyRotation(float[][] samples) {
        
        for (int s = 0; s < samples.length; ++s) {
            for (int d = 0; d < DIM; ++d) {
                tempVec[d] = 0.0f;

                for (int k = 0; k < DIM; ++k) {
                    tempVec[d] += rotationMat[(d * 3) + k] * samples[s][k];
                }
            }

            for (int d = 0; d < DIM; ++d) {
                samples[s][d] = tempVec[d];
            }
        }
    }

    /**
     *
     * @param samples
     * samples to compute the mean vector of, should be in the form:
     * index:		[ 0 ][ 1 ][ 2 ][ 3 ][ 4 ][ 5 ]...
     * dimension:   [ x ][ y ][ z ][ x ][ y ][ z ]...
     *
     * @param outVec
     * an array of 3 floats to save the final mean vector in
     */
    private static void computeMeanVector(float[][] samples, float[] outVec) {
        for (int d = 0; d < DIM; ++d) {
            outVec[d] = 0.0f;
        }

        //	find the total and number of samples (each having x, y, and z)
        int count = 0;
        for (int s = 0; s < samples.length; ++s) {
            for (int d = 0; d < DIM; ++d) {
                outVec[d] += samples[s][d];
            }
            ++count;
        }

        //	convert total to the mean
        for (int d = 0; d < DIM; ++d) {
            outVec[d] /= count;
        }

    }

    private static float[] normalizedGravity = new float[3];

    /**
     *
     * Source: <a>http://stackoverflow.com/questions/2096474/given-a-surface-normal-find-rotation-for-3d-plane</a>
     *
     * @param inGravityVec
     * the gravity vector(3 dimensions) to convert to a horizontal vector
     *
     * @param outVec
     * an array of 3 floats to save the final horizontal vector in
     */
    synchronized private static void convertToHorVec(float[] inGravityVec, float[] outVec) {
        for (int i = 0; i < 3; ++i) {
            normalizedGravity[i] = inGravityVec[i];
            outVec[i] = 0.0f;
        }

        //	normalize gravity vector
        CalcStatistics.normalize(3, normalizedGravity);

        //	get the minimum axis
        int imin = 0;
        for (int i = 0; i < 3; ++i) {
            if (normalizedGravity[i] < normalizedGravity[imin]) {
                imin = i;
            }
        }

        //	get the scaling value
        float dt = normalizedGravity[imin];

        //	subtract the scaled value from the unit vector of the minimum axis
        outVec[imin] = 1;
        for (int i = 0; i < 3; i++) {
            outVec[i] -= dt * normalizedGravity[i];
        }

        // now normalize the vector being returned
        CalcStatistics.normalize(3, outVec);
    }

    /**
     *
     * @param vector
     * The 3 dimensional vector to find the minimum value
     *
     * @return
     * The index of the minimum value in the vector
     */
    private static int findIndexOfSmallest(float[] vector) {
        int index = -1;
        float value = Float.MAX_VALUE;
        float temp;
        for (int d = 0; d < DIM; ++d) {
            temp = vector[d];
            if (temp < 0.0f) {
                temp = -temp;
            }

            if (temp < value) {
                value = temp;
                index = d;
            }
        }
        return index;
    }

    /**
     *
     * Returns the index of the largest value in the vector
     *
     * @param vector
     * The 3 dimensional vector to find the minimum value
     *
     * @return
     * The index of the minimum value in the vector
     */
    private static int findIndexOfLargest(float[] vector) {
            int index = -1;
            float value = Float.NEGATIVE_INFINITY;
            float temp;
            for (int d=0; d< DIM; ++d) {
                    temp = vector[d];
                    if (temp<0.0f)
                            temp = -temp;

                    if (temp>value) {
                            value = temp;
                            index = d;
                    }
            }
            return index;
    }

    private static String vec2str(float[] vec) {
        return String.format("{x=% 3.2f, y=% 3.2f, z=% 3.2f}", vec[X_AXIS], vec[Y_AXIS], vec[Z_AXIS]);
    }

    private static String vec2str(float[] vec, int start) {
        return String.format("{x=% 3.2f, y=% 3.2f, z=% 3.2f}", vec[start + X_AXIS], vec[start + Y_AXIS], vec[start + Z_AXIS]);
    }

}
