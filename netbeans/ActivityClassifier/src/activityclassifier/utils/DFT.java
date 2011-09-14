/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package activityclassifier.utils;

import java.util.Arrays;

/**
 *
 * @author Umran
 */
public class DFT
{

    // An example of performing the discrete Fourier transform
    // with function f(x)=x(1-x) with x in [0,1]. The
    // inverse transform is also performed for comparison.
    public static void main( String argv[] ) {
        final int n = 128, m = 8;

        double x[] = new double[n];
        double fr[] = new double[n];
        double fi[] = new double[n];
        double gr[] = new double[n];
        double gi[] = new double[n];
        double h = 1.0 / (n - 1);
        double f0 = 1 / Math.sqrt(n);

        // Assign the data and perform the transform
        for ( int i = 0; i < n; ++i ) {
            x[i] = h * i;
            fr[i] = x[i] * (1 - x[i]);
            fi[i] = 0;
        }

        dft(fr, fi, gr, gi);

        // Perform the inverse Fourier transform
        for ( int i = 0; i < n; ++i ) {
            gr[i] = f0 * gr[i];
            gi[i] = -f0 * gi[i];
            fr[i] = fi[i] = 0;
        }

        dft(gr, gi, fr, fi);

        for ( int i = 0; i < n; ++i ) {
            fr[i] = f0 * fr[i];
            fi[i] = -f0 * fi[i];
        }

        // Output the result in every m data steps
        for ( int i = 0; i < n; i += m ) {
            System.out.println(x[i] + " " + fr[i] + " " + fi[i]);
        }
    }

    // Method to perform the discrete Forier transform. Here
    // fr[] and fi[] are the real and imaginary parts of the
    // data with the correponding outputs gr[] and gi[].
    public static void dft( double fr[], double fi[],
                            double gr[], double gi[] ) {
        int n = fr.length;
        double x = 2 * Math.PI / n;

        for ( int i = 0; i < n; ++i ) {
            for ( int j = 0; j < n; ++j ) {
                double q = x * j * i;
                gr[i] += fr[j] * Math.cos(q) + fi[j] * Math.sin(q);
                gi[i] += fi[j] * Math.cos(q) - fr[j] * Math.sin(q);
            }
        }
    }

    // Method to perform the fast Foruier transform. Here
    // fr[] and fi[] are the real and imaginary parts of
    // both the input and output data.
    public static void fft( double fr[], double fi[], int m ) {
        int n = fr.length;
        int nh = n / 2;
        int np = (int) Math.pow(2, m);
        
        // Stop the program if the indices do not match
        if ( np != n ) {
            throw new RuntimeException("Index mismtch detected np="+np+", n="+n);
        }
        
        // Rearrange the data to the bit-reversed order
        int k = 1;
        for ( int i = 0; i < n - 1; ++i ) {
            if ( i < k - 1 ) {
                double f1 = fr[k - 1];
                double f2 = fi[k - 1];
                fr[k - 1] = fr[i];
                fi[k - 1] = fi[i];
                fr[i] = f1;
                fi[i] = f2;
            }
            int j = nh;
            while ( j < k ) {
                k -= j;
                j /= 2;
            }
            k += j;
        }

        // Sum up the reordered data at all levels
        k = 1;
        for ( int i = 0; i < m; ++i ) {
            double w = 0;
            int j = k;
            k = 2 * j;
            for ( int p = 0; p < j; ++p ) {
                double u = Math.cos(w);
                double v = -Math.sin(w);
                w += Math.PI / j;
                for ( int q = p; q < n; q += k ) {
                    int r = q + j;
                    double f1 = fr[r] * u - fi[r] * v;
                    double f2 = fr[r] * v + fi[r] * u;
                    fr[r] = fr[q] - f1;
                    fr[q] += f1;
                    fi[r] = fi[q] - f2;
                    fi[q] += f2;
                }
            }
        }
    }

    public static void ifft( double fr[], double fi[], int m )
    {
        int n = fr.length;
        double f0 = 1 / Math.sqrt(n);

        // Perform the inverse Fourier transform
        for ( int i = 0; i < n; ++i ) {
            fr[i] = f0 * fr[i];
            fi[i] = -f0 * fi[i];
        }

        fft(fr, fi, m);

        for ( int i = 0; i < n; ++i ) {
            fr[i] = f0 * fr[i];
            fi[i] = -f0 * fi[i];
        }
    }

    public static double[][] computePowerSpectrum( double fr[], double fi[], double[] frequency )
    {
        int n = fr.length;
        double a;

        double[] power = new double[n/2];

        for (int j=0; j<n/2; ++j) {
            a = Math.hypot(fr[j], fi[j]);

            a /= n;

            power[j] = a*a*2.0;
        }

        return new double[][]{ Arrays.copyOf(frequency, n/2), power };
    }

    public static double[][] computeGroupedPowerSpectrum( double[][] powerSpectrum, double[][] groups )
    {
        int n = powerSpectrum[0].length;
        double[] meanGroups = new double[groups.length];
        double[] countGroups = new double[groups.length];
        double[] meanSpectrum = new double[n];

        for (int j=0; j<n; ++j) {
            double freq = powerSpectrum[0][j];
            double val = powerSpectrum[1][j];

            for (int g=0; g<groups.length; ++g) {
                if (freq>=groups[g][0] && freq<=groups[g][1]) {
                    meanGroups[g] += val;
                    countGroups[g] += 1.0;
                }
            }
        }

        for (int g=0; g<groups.length; ++g) {
            meanGroups[g] /= countGroups[g];
        }

        for (int j=0; j<n; ++j) {
            double freq = powerSpectrum[0][j];
            double val = 0.0;

            for (int g=0; g<groups.length; ++g) {
                if (freq>=groups[g][0] && freq<=groups[g][1]) {
                    val = meanGroups[g];
                    break;
                }
            }

            meanSpectrum[j] = val;
        }

        return new double[][] { powerSpectrum[0], meanSpectrum };
    }

    public static void clearUpperHalf( double fr[], double fi[] )
    {
        int n = fr.length;

        for (int j=n/2; j<n; ++j) {
            fr[j] = 0.0;
            fi[j] = 0.0;
        }
    }


    public static void divide( double fr[], double fi[], double[] value )
    {
        int n = fr.length;
        double r, i;
        double valueReceprical;

        for (int j=0; j<n; ++j) {

            if (value[j]==0.0)
                valueReceprical = Double.MAX_VALUE;
            else
                valueReceprical = value[j] / (value[j]*value[j]);

            r = fr[j] * valueReceprical;
            i = fi[j] * valueReceprical;

            fr[j] = r;
            fi[j] = i;
        }

    }

    public static void multiply( double fr[], double fi[], double[] value )
    {
        int n = fr.length;
        double r, i;

        for (int j=0; j<n; ++j) {

            r = fr[j] * value[j];
            i = fi[j] * value[j];

            fr[j] = r;
            fi[j] = i;
        }

    }

    public static int nextPowerOfTwo( int actualSamples )
    {
        int nextPowerOfTwo = 0;

        while (actualSamples>0) {
            actualSamples >>= 1;
            nextPowerOfTwo++;
        }

        return nextPowerOfTwo;
    }
    
}
