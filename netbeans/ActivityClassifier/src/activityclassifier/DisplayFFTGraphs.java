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
import activityclassifier.utils.FT;
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
import java.util.Map;
import java.util.TreeMap;
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
public class DisplayFFTGraphs extends javax.swing.JFrame {

    private static final double SAMPLING_START_TIME = 1.0;
    private static final double SAMPLING_PERIOD = 0.2;

    private static final double SAMPLING_FREQUENCY = 1000.0;
    private static final double INTERSAMPLE_DURATION = 1 / SAMPLING_FREQUENCY;

    private static final int WINDOW_SIZE;
    private static final int POWER_OF_TWO;

    static {
        int actualSamples = (int)Math.floor(SAMPLING_PERIOD * SAMPLING_FREQUENCY);
        int nextPowerOfTwo = 0;

        System.out.println("actual samples = "+actualSamples);
        System.out.println("inter sample duration = "+INTERSAMPLE_DURATION);
        System.out.println("total duration = "+(actualSamples*INTERSAMPLE_DURATION));

        while (actualSamples>0) {
            actualSamples >>= 1;
            nextPowerOfTwo++;
        }

        WINDOW_SIZE = 1 << nextPowerOfTwo;
        POWER_OF_TWO = nextPowerOfTwo;

        System.out.println("Next Power of Two Window Size = "+WINDOW_SIZE);
        System.out.println("Next Power of Two Duration = "+(WINDOW_SIZE*INTERSAMPLE_DURATION));
    }

    private static class DisplayData {
        final String heading;
        final DefaultXYDataset dataset;

        public DisplayData(String heading, DefaultXYDataset dataset) {
            this.heading = heading;
            this.dataset = dataset;
        }
    }

    public static double[][] countFrequency(double[] values)
    {
        Map<Double,Integer> counter = new TreeMap<Double, Integer>();

        int maxC = Integer.MIN_VALUE;

        for (double v:values) {
            if (!counter.containsKey(v)) {
                counter.put(v, 0);
            }

            Integer c = counter.get(v);

            int newC = c + 1;

            if (newC>maxC) {
                maxC = newC;
            }

            counter.put(v, newC);
        }

        int resSize = counter.keySet().size();
        double res[][] = new double[2][resSize];

        int i = 0;
        for (Double v:counter.keySet()) {
            Integer c = counter.get(v);

            res[0][i] = c;
            res[1][i] = v;
            ++i;
        }

        return res;
    }

    private static double[] copyAvoidNaN(double[] array, int size)
    {
        double[] copy = Arrays.copyOf(array, size);

        for (int i=0; i<size; ++i)
            if (Double.isNaN(copy[i]))
                copy[i] = 0.0;

        return copy;
    }

    public static void main(String[] args) {
        try {
            final List<DisplayData> dataSets = new ArrayList<DisplayData>();

            double[] time = new double[WINDOW_SIZE];
            double[] frequency = new double[WINDOW_SIZE];
            double[] vertHeight = new double[WINDOW_SIZE];

            for (int i=0; i<WINDOW_SIZE; ++i) {
                time[i] = SAMPLING_START_TIME + (double)i*INTERSAMPLE_DURATION;
                frequency[i] = i * SAMPLING_FREQUENCY / WINDOW_SIZE;

                double sqrt2 = Math.sqrt(2);

                vertHeight[i] =
                        10.0 * sqrt2 * Math.sin(2.0 * Math.PI * 50.0 * time[i]) +
                        5.0 * sqrt2 * Math.sin(2.0 * Math.PI * 120.0 * time[i]) +
                        8.0 * sqrt2 * Math.sin(2.0 * Math.PI * 315.0 * time[i]) +
                        2.0 * sqrt2 * Math.sin(2.0 * Math.PI * 500.0 * time[i]) ;
            }

            DefaultXYDataset dataset = new DefaultXYDataset();

            double[] real = Arrays.copyOf(vertHeight, WINDOW_SIZE);
            double[] img = new double[WINDOW_SIZE];

            DFT.fft(real, img, POWER_OF_TWO);

            DFT.clearUpperHalf(real, img);

            double[] omegas = new double[WINDOW_SIZE];

            for (int i=0; i<WINDOW_SIZE; ++i) {
                omegas[i] = 2.0*Math.PI*frequency[i];

                omegas[i] = -omegas[i]*omegas[i];
            }
            
            DFT.divide(real, img, omegas);

            real[0] = 0.0;
            
            DFT.ifft(real, img, POWER_OF_TWO);

            dataset.addSeries("DISPLACEMENT", new double[][] {
                copyAvoidNaN(time, WINDOW_SIZE),
                copyAvoidNaN(real, WINDOW_SIZE)
            });

                /*
                double[][] powerSpectrum = DFT.computePowerSpectrum(real, img, frequency);
                double[][] meanGroupedPowerSpectrum = DFT.computeGroupedPowerSpectrum(powerSpectrum,
                                                                                      new double[][] {
                            {40.0, 60.0},
                            {110.0, 130.0},
                            {305.0, 325.0},
                            {490.0, 510.0}
                });
                dataset.addSeries("POWER SPECTRUM", powerSpectrum);
                dataset.addSeries("MEAN GROUPED POWER SPECTRUM", meanGroupedPowerSpectrum);
                */

//            dataset.addSeries("ACCEL", new double[][] {
//                time,
//                vertHeight
//            });

            dataSets.add(new DisplayData("Test", dataset));
            
            //  let the GUI take over
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new DisplayFFTGraphs(dataSets).setVisible(true);
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
                JOptionPane.showMessageDialog(DisplayFFTGraphs.this, "Ran out of memory");
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
    private DisplayFFTGraphs(List<DisplayData> dataSets) {
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
