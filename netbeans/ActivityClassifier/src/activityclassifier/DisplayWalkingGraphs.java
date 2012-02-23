/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * WalkingGraphs.java
 *
 * Created on Jan 28, 2011, 3:31:25 PM
 */

package activityclassifier;

import activityclassifier.utils.AccelToDistanceFFT;
import activityclassifier.utils.ActivityIO;
import activityclassifier.utils.Complex;
import activityclassifier.utils.DFT;
import activityclassifier.utils.FeatureExtractor;
import activityclassifier.utils.OutlierAnalyser;
import activityclassifier.utils.RamerDouglasPeucker;
import activityclassifier.utils.RotateSamplesToVerticalHorizontal;
import java.awt.event.ActionEvent;
import java.util.Scanner;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author oka
 */
public class DisplayWalkingGraphs extends javax.swing.JFrame {

//    private static final File SOURCE_FILE = Common.CLEAN_SQL_DATA;
    private static final File SOURCE_FILE = Common.DIRTY_CSV_DATA;
    private static final int WINDOW_SIZE = 128;

    private static final double SAMPLING_FREQUENCY = 20.0;

    private static final float[] WALKING_STATISTIC_MEAN = new float[] {
        1.4209439f, 3.426552f, 2.2063925f, 10.097596f
    };

    private static final float[] WALKING_STATISTIC_STDDEV = new float[] {
        0.57872593f, 1.3638645f, 0.8698633f, 0.454567f
    };

    private static class DisplayData {
        final String heading;
        final DefaultXYDataset dataset;

        public DisplayData(String heading, DefaultXYDataset dataset) {
            this.heading = heading;
            this.dataset = dataset;
        }
    }

    private static double[] scale(double[] values, int size, double scale)
    {
        double[] res = new double[size];
        for (int i=0; i<size; ++i)
            res[i] = values[i]*scale;
        return res;
    }
    
    /*
    private static double[] accel2dist(double[] accel, double[] time, int size)
    {
        double t;
        double[] res = new double[size];
        double prevV = 0.0;
        
        res[0] = 0.0;
        for (int i=1; i<size; ++i) {
            t = (time[i]-time[i-1]);

            double newV = prevV + 0.5*(accel[i]+accel[i-1])*t;
            res[i] = res[i-1] + 0.5*(prevV + newV)*t;
            prevV = newV;
            
//            res[i] = res[i-1] + prevV*t + accel[i]*t*t*0.5;
//            prevV = prevV + accel[i]*t;

//            res[i] = res[i-1] + accel[i]*t*t*0.5;
        }

        return res;
    }
    */

    private static double[] removeMean(double[] input, int size)
    {
        double[] res = new double[size];
        double mean = 0.0;

        for (int i=0; i<size; ++i)
            mean += input[i];

        mean /= size;

        for (int i=0; i<size; ++i)
            res[i] = input[i] - mean;

        return res;
    }

    private static double[] integrate(double[] input, double[] time, int size)
    {
        double[] res = new double[size];
        double t;

        for (int i=1; i<size; ++i) {
            t = (time[i]-time[i-1]);

            res[i] = res[i-1] + 0.5*(input[i]+input[i-1])*t;
        }

        return res;
    }

    private static double[] accel2dist(double[] accel, double[] time, int size)
    {
        double[] vel = integrate(removeMean(accel, size), time, size);
        double[] dist = integrate(removeMean(vel, size), time, size);
        return dist;
    }

    private static double[] accel2distFFT(double[] accel, double[] time, int size)
    {
        double[] x = new double[size];

        for (int i=1; i<size; ++i) {
            double t = time[i] - time[i-1];
            double a = (accel[i] + accel[i-1]) * 0.5;
            x[i] = x[i-1] + 0.5*a*t*t;
        }

        return x;
    }
    
    /*
    private static double[] accel2distFFT(double[] accel, double[] time, int size)
    {
        int nextPowerOfTwo = DFT.nextPowerOfTwo(size);
        int newSize = 1 << nextPowerOfTwo;

        double[] real = Arrays.copyOf(accel, newSize);
        double[] img = new double[newSize];

        DFT.fft(real, img, nextPowerOfTwo);

        double[] omegas = new double[newSize];

        for (int i=0; i<newSize; ++i) {
            double f = SAMPLING_FREQUENCY * i / newSize;
            double o = 2.0*Math.PI*f;

            omegas[i] = -o*o;
        }

        DFT.divide(real, img, omegas);

        real[0] = 0.0;

        DFT.ifft(real, img, nextPowerOfTwo);

        return Arrays.copyOf(real, size);
    }
    */
    private static double calcAvgHeightShift(double[] input, int size)
    {
        double height;
        double[] absHeights = new double[size];
        double meanAbsHeights = 0.0;
        int[] sources = new int[size];
        int count = 0;
        int lastTurn = -1;

        for (int i=1; i<size-1; ++i) {
            boolean p = (input[i]-input[i-1]) < 0.0;
            boolean n = (input[i+1]-input[i]) < 0.0;

            if (p^n) {
                if (lastTurn>=0) {
                    height = input[i]-input[lastTurn];
                    if (height<0.0)
                        absHeights[count] = -height;
                    else
                        absHeights[count] = height;
                    if (count%2==1) {
                        height = Math.min(absHeights[count],absHeights[count-1])*2.0;
                        if (height<absHeights[count] || height<absHeights[count-1]) {
                            height = height / 2.0;
                        } else {
                            height = (absHeights[count]+absHeights[count-1]) / 2.0;
                        }
                        meanAbsHeights += height*2;
                        absHeights[count] = height;
                        absHeights[count-1] = height;
                    }
                    sources[count] = i;
                    ++count;
                }
                if (lastTurn>=0 || p==true) {
                    lastTurn = i;
                }
            }
        }

        if (count==0) {
            System.out.println("ERROR: No turning points found!");
            return Double.NaN;
        }

        meanAbsHeights = meanAbsHeights / count;

        double stddevAbsHeights = 0.0;
        for (int i=0; i<count; ++i) {
            stddevAbsHeights += (absHeights[i]-meanAbsHeights)*(absHeights[i]-meanAbsHeights);
        }
        stddevAbsHeights /= count;
        stddevAbsHeights = Math.sqrt(stddevAbsHeights);

        double deviation = stddevAbsHeights;

        double meanResult = 0.0;
        double resultCount = 0.0;

        for (int i=0; i<count; i+=2) {
            if (    absHeights[i]>=meanAbsHeights-deviation &&
                    absHeights[i+1]>=meanAbsHeights-deviation   )
            {
                meanResult += absHeights[i];
                resultCount += 1.0;
            }
        }

        return meanResult / resultCount;
    }

    private static double[][] simplifyCurveRDP(double[] vertHeights, double[] time, int size, double epsilon)
    {
        RamerDouglasPeucker rdp = new RamerDouglasPeucker(size, vertHeights, time);

        boolean[] filter = rdp.reduce(epsilon);

        int count = 0;
        for (int i=0; i<size; ++i) {
            if (!filter[i]) {
                ++count;
            }
        }

        double[] resHeights = new double[count];
        double[] resTimes = new double[count];

        int j = 0;
        for (int i=0; i<size; ++i) {
            if (!filter[i]) {
                resHeights[j] = vertHeights[i];
                resTimes[j] = time[i];
                ++j;
            }
        }

        return new double[][] {resTimes, resHeights};
    }

    private static double[] getIntervals(double[] accel, double[] time, int size)
    {
        double[] intervals = new double[size];

        for (int i=1; i<size; ++i) {
//            intervals[i] = (time[i]-time[i-1]);
            intervals[i] = intervals[i-1] + ((time[i]-time[i-1]) - 0.05);
        }

        return intervals;
    }
    
    public static void main(String[] args) {
        try {
            RotateSamplesToVerticalHorizontal rstvh = new RotateSamplesToVerticalHorizontal();
            List<Activity> activities = ActivityIO.read(new Scanner(SOURCE_FILE));
            FeatureExtractor extractor = new FeatureExtractor(WINDOW_SIZE);
            OutlierAnalyser analyser = new OutlierAnalyser(false, true, true);
            final List<DisplayData> dataSets = new ArrayList<DisplayData>();

            String[] dimensionNames = new String[] {
                "Hor", "Ver"
            };

            System.out.println(activities.size()+" total activities found.");

            for (int activityId=0; activityId<activities.size(); ++activityId) {
                Activity activity = activities.get(activityId);

                if (!activity.name.contains("WALKING")) continue;

                System.out.println("walking activity found.");

                int rowCount = activity.data.length - activity.data.length%WINDOW_SIZE;
                float[][] threeDimSamples = new float[WINDOW_SIZE][3];

                double minTime = Double.POSITIVE_INFINITY;

                for (int windowStart=0; windowStart+WINDOW_SIZE<=rowCount; windowStart+=WINDOW_SIZE) {

                    {
                        double startTime = activity.time[windowStart] / 1000.0;
                        if (startTime<minTime)
                            minTime = startTime;
                    }
                    
                    float[] features = extractor.extractUnrotated(activity.data, windowStart);
                    
                    if (features==null) {
                        System.out.println("Activity="+(activityId+1)+"("+activity.name+") Start="+windowStart+", didn't survive the rotation");
                        continue;
                    }
                    
                    boolean invalidSample = false;
//                    for (int i=0; i<FeatureExtractor.NUM_FEATURES; ++i) {
//                        if (features[i]<WALKING_STATISTIC_MEAN[i]-WALKING_STATISTIC_STDDEV[i] ||
//                                features[i]>WALKING_STATISTIC_MEAN[i]+WALKING_STATISTIC_STDDEV[i]) {
//                            invalidSample = true;
//                            break;
//                        }
//                    }
                    if (invalidSample) {
                        System.out.println("Activity="+(activityId+1)+" Start="+windowStart+", doesn't meet the statistics for walking.");
                        continue;
                    }

                    for (int i=0; i<WINDOW_SIZE; ++i) {
                        threeDimSamples[i][0] = activity.data[i+windowStart][0];
                        threeDimSamples[i][1] = activity.data[i+windowStart][1];
                        threeDimSamples[i][2] = activity.data[i+windowStart][2];
                    }
                    
                    double[][] displayData = new double[dimensionNames.length+1][WINDOW_SIZE];

                    for (int i=0; i<WINDOW_SIZE; ++i) {
                        double time = activity.time[i+windowStart] / 1000.0;

                        displayData[0][i] = time - minTime;

                        //  combine x & y to hor
                        displayData[1][i] =
                                Math.sqrt(  threeDimSamples[i][0]*threeDimSamples[i][0] +
                                            threeDimSamples[i][1]*threeDimSamples[i][1] );
                        //  vert = z - gravity
//                            displayData[2][i] = threeDimSamples[i][2] - 9.81;
                        displayData[2][i] = threeDimSamples[i][2] - rstvh.getGravity();
//                            displayData[2][i] = Math.sin(displayData[0][i]*Math.PI);
                    }


                    StringBuilder comments = new StringBuilder();


                    DefaultXYDataset dataset = new DefaultXYDataset();

                    double epsilon = 0.01;
                    double[] vertDistDI = accel2dist(displayData[2], displayData[0], WINDOW_SIZE);
                    double[] vertDistFFT = accel2distFFT(displayData[2], displayData[0], WINDOW_SIZE);
                    double[][] simplifiedCurveDI = simplifyCurveRDP(vertDistDI, displayData[0], WINDOW_SIZE, epsilon);
                    double[][] simplifiedCurveFFT = simplifyCurveRDP(vertDistFFT, displayData[0], WINDOW_SIZE, epsilon);
                    double velocityDI = calcAvgHeightShift(simplifiedCurveDI[1], simplifiedCurveDI[1].length) / 0.038;
                    double velocityFFT = calcAvgHeightShift(simplifiedCurveFFT[1], simplifiedCurveFFT[1].length) / 0.038;

                    System.out.println("velocityFFT = " + velocityFFT);

                    dataset.addSeries("RDF DI epsilon="+epsilon, simplifiedCurveDI);
                    dataset.addSeries("RDF FFT epsilon="+epsilon, simplifiedCurveFFT);

                    dataset.addSeries("VERT DIST (Double integration) vel="+velocityDI, new double[][] {
                        displayData[0],
                        accel2dist(displayData[2], displayData[0], WINDOW_SIZE)
                    });

                    dataset.addSeries("VERT DIST (FFT) vel="+velocityFFT, new double[][] {
                        displayData[0],
                        accel2distFFT(displayData[2], displayData[0], WINDOW_SIZE)
                    });

//                        dataset.addSeries("TIME INTERVALS", new double[][] {
//                            displayData[0],
//                            getIntervals(displayData[2], displayData[0], WINDOW_SIZE)
//                        });
//
//                        dataset.addSeries("VERT ACCEL (x0.1)", new double[][] {
//                            displayData[0],
//                            scale(displayData[2], WINDOW_SIZE, 0.1)
//                        });

                    dataSets.add(new DisplayData("Activity="+(activityId+1)+"("+activity.name+") Start="+windowStart+",\n"+comments.toString(), dataset));
                }
            }

            System.out.println(dataSets.size()+" graphs to be drawn.");

            //  let the GUI take over
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new DisplayWalkingGraphs(dataSets).setVisible(true);
                }
            });


        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
    
    private static class DataToolTipGenerator
            implements XYToolTipGenerator
    {

        public String generateToolTip( XYDataset dataset, int series, int item ) {
            return String.format("(%.0f, %.0f)", dataset.getX(series, item).doubleValue(), dataset.
                    getY(series, item).doubleValue());
        }
    }

    private class DisplayChartRunnable implements Runnable {
        int index;

        public DisplayChartRunnable(int index) {
            this.index = index;
        }

        public void run() {
            DisplayData displayData = dataSets.get(index);

            if (currentDisplayFrame!=null) {
                currentDisplayFrame.setVisible(false);
                currentDisplayFrame.dispose();
                currentDisplayFrame = null;
                System.gc();
            }

            try {
                NumberAxis xAxis = new NumberAxis("time (s)");
                xAxis.setAutoRangeIncludesZero(false);
                NumberAxis yAxis = new NumberAxis("values");
                XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES,
                                                                     new DataToolTipGenerator());

                XYPlot plot = new XYPlot(displayData.dataset, xAxis, yAxis, renderer);
                plot.setOrientation(PlotOrientation.VERTICAL);
                JFreeChart chart = new JFreeChart(
                        displayData.heading,
                        JFreeChart.DEFAULT_TITLE_FONT,
                        plot,
                        true);

                plot.setBackgroundPaint(new Color(200, 200, 200, 220));

                ChartFrame frame = new ChartFrame(chart.getTitle().getText(), chart, true);
                frame.pack();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);

                if (currentDisplayFrame!=null) {
                    System.out.println("Error: Lock doesn't work. Another frame was created before finishing to create this one!");
                }

                currentDisplayFrame = frame;
                currentData = index;
                updateButtons();
            } catch (OutOfMemoryError e) {
                JOptionPane.showMessageDialog(DisplayWalkingGraphs.this, "Ran out of memory");
                e.printStackTrace(System.out);
            } finally {
                createChartLock.unlock();
            }
        }
    }

    private List<DisplayData> dataSets;
    private int currentData = 0;
    
    private final ReentrantLock createChartLock = new ReentrantLock(true);
    private JFrame currentDisplayFrame = null;

    /** Creates new form WalkingGraphs */
    private DisplayWalkingGraphs(List<DisplayData> dataSets) {
        this.dataSets = dataSets;
        initComponents();
        display(currentData);

        for (int i=0; i<dataSets.size(); ++i) {
            final int index = i;
            JButton btn = new JButton(Integer.toString(i+1));
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    display(index);
                }
            });
            panelButtons.add(btn);
        }
    }

    private void display(int index) {

        if (createChartLock.tryLock()) {
            java.awt.EventQueue.invokeLater(new DisplayChartRunnable(index));
        }
        
    }

    private void updateButtons() {
        this.setTitle((currentData+1)+"/"+dataSets.size());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelButtons = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);

        panelButtons.setLayout(new java.awt.GridLayout(0, 15));
        getContentPane().add(panelButtons, java.awt.BorderLayout.CENTER);

        setBounds(0, 0, 880, 231);
    }// </editor-fold>//GEN-END:initComponents

    /*
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WalkingGraphsFrame().setVisible(true);
            }
        });
    }
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel panelButtons;
    // End of variables declaration//GEN-END:variables

}
