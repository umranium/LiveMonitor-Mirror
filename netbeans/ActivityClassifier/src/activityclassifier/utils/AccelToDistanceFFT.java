/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.utils;

/**
 *
 * @author Umran
 */
public class AccelToDistanceFFT {

    private int size;
    private double[] accel;
    private double[] time;
    private Complex[] fftAccel;
    private Complex[] fftDivOmega;
    private Complex[] invFFT;

    public AccelToDistanceFFT( int size, double[] accel, double[] time, double f ) {
        this.size = size;
        this.accel = accel;
        this.time = time;
        this.fftAccel = computeFFT();

        double omega = 2.0 * Math.PI * f;
        Complex negOmegaSqr = new Complex(-(omega * omega), 0.0);

        fftDivOmega = new Complex[size];
        for (int i=0; i<size; ++i)
            fftDivOmega[i] = this.fftAccel[i].divides(negOmegaSqr);

        invFFT = FT.ifft(fftDivOmega);
    }

    private double computeMean() {
        double mean = 0.0;

        for (int i=0; i<size; ++i)
            mean += accel[i];

        mean /= size;

        return mean;
    }

    private Complex[] computeFFT()
    {
        double mean = computeMean();

        Complex[] compAccel = new Complex[size];

        //  convert to complex
        for (int i=0; i<size; ++i)
            compAccel[i] = new Complex(accel[i] - mean, 0.0);
        
        return FT.fft(compAccel);
    }

    public Complex[] getFftAccel() {
        return fftAccel;
    }

    public Complex[] getDist() {
        return invFFT;
    }
    

}
