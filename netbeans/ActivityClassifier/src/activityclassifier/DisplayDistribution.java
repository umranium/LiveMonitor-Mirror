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

import activityclassifier.utils.ActivityIO;
import activityclassifier.utils.AllFeatureExtractor;
import activityclassifier.utils.RotateSamplesToVerticalHorizontal;
import java.awt.event.ActionEvent;
import java.util.Scanner;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
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
public class DisplayDistribution extends javax.swing.JFrame {

    private static final double BLOCK_START = 0.0;
    private static final double BLOCK_SIZE = 0.1;

    private static final File SOURCE_FILE = Common.CLEAN_SQL_DATA;
//    private static final File SOURCE_FILE = Common.DIRTY_CSV_DATA;
    private static final int WINDOW_SIZE = 128;
    private static final int WINDOW_STEP = 64;

    private static class DisplayData {
        final String heading;
        final DefaultXYDataset dataset;

        public DisplayData(String heading, DefaultXYDataset dataset) {
            this.heading = heading;
            this.dataset = dataset;
        }
    }
    
    public static void main(String[] args) {
        try {
            RotateSamplesToVerticalHorizontal rstvh = new RotateSamplesToVerticalHorizontal();
            List<Activity> activities = ActivityIO.read(new Scanner(SOURCE_FILE));
            AllFeatureExtractor extractor = new AllFeatureExtractor(WINDOW_SIZE);
            final List<DisplayData> dataSets = new ArrayList<DisplayData>();

            System.out.println(activities.size()+" total activities found.");

            Map<String,Map<Double,Integer>[]> data = new TreeMap<String, Map<Double,Integer>[]>(new Comparator<String>() {
                public int compare( String o1, String o2 ) {
                    return o1.compareToIgnoreCase(o2);
                }
            });

            for (int activityId=0; activityId<activities.size(); ++activityId) {
                Activity activity = activities.get(activityId);

                Map<Double,Integer>[] valueMap = data.get(activity.name);
                if (valueMap == null) {
                    valueMap = new Map[AllFeatureExtractor.NUM_FEATURES];
                    for (int i=0; i<AllFeatureExtractor.NUM_FEATURES; ++i) {
                        valueMap[i] = new TreeMap<Double, Integer>();
                    }
                    data.put(activity.name, valueMap);
                }
                
                int rowCount = activity.data.length - activity.data.length%WINDOW_SIZE;
                float[][] threeDimSamples = new float[WINDOW_SIZE][3];

                double minTime = Double.POSITIVE_INFINITY;

                for (int windowStart=0; windowStart+WINDOW_SIZE<=rowCount; windowStart+=WINDOW_STEP) {

                    {
                        double startTime = activity.time[windowStart] / 1000.0;
                        if (startTime<minTime)
                            minTime = startTime;
                    }

                    for (int i=0; i<WINDOW_SIZE; ++i) {
                        threeDimSamples[i][0] = activity.data[i+windowStart][0];
                        threeDimSamples[i][1] = activity.data[i+windowStart][1];
                        threeDimSamples[i][2] = activity.data[i+windowStart][2];
                    }

                    if (rstvh.rotateToWorldCoordinates(threeDimSamples)) {
                        float[] features = extractor.extractRotated(threeDimSamples, 0);
                        
//                        ExtractedFeature extractedFeature = new ExtractedFeature(activity.name, features);
//                        if (!Filters.shouldFilter(extractedFeature))
                        {
                            for (int i=0; i<AllFeatureExtractor.NUM_FEATURES; ++i) {
                                double value = (double)features[i];
                                int block = (int)((value - BLOCK_START) / BLOCK_SIZE);
                                System.out.print(" feature='"+AllFeatureExtractor.FEATURE_NAMES[i]+"', block="+block+", orig_value="+value);
                                value = BLOCK_START + block * BLOCK_SIZE;
                                System.out.print(" final_value="+value);

                                Integer count = valueMap[i].get(value);
                                if (count==null)
                                    count = 0;
                                count = count + 1;
                                valueMap[i].put(value, count);

                                System.out.println(" count="+count);
                            }
                        }

                    } else {
                        System.out.println("Activity="+(activityId+1)+"("+activity.name+") Start="+windowStart+", didn't survive the rotation ");
                    }
                }
            }

            for (int f=0; f<AllFeatureExtractor.NUM_FEATURES; ++f) {

                DefaultXYDataset dataset = new DefaultXYDataset();
                
                for (String activity:data.keySet()) {
                    
                    Map<Double,Integer>[] valueMaps = data.get(activity);
                    if (valueMaps==null) {
                        System.out.println("Activity: "+activity+" has no values!!");
                    }

                    Map<Double,Integer> valueMap = valueMaps[f];

                    double minValue = Double.POSITIVE_INFINITY;
                    double maxValue = Double.NEGATIVE_INFINITY;
                    for (Double val:valueMap.keySet()) {
                        if (val>maxValue)
                            maxValue = val;
                        if (val<minValue)
                            minValue = val;
                    }







                    ArrayList<double[]> arrayList = new ArrayList<double[]>();

                    if (!Double.isInfinite(minValue) && !Double.isInfinite(maxValue)) {
                        for (int block=0; BLOCK_START+block*BLOCK_SIZE<maxValue+BLOCK_SIZE; ++block) {
                            double value = BLOCK_START+block*BLOCK_SIZE;
                            Integer count = valueMap.get(value);
                            if (count==null) {
                                arrayList.add(new double[] { value, 0.0 } );
                                arrayList.add(new double[] { value+BLOCK_SIZE, 0.0 } );
                            } else {
                                System.out.println("\tplotting: "+activity+":"+AllFeatureExtractor.FEATURE_NAMES[f]+", value="+value+", count="+count);
                                arrayList.add(new double[] { value, (double)(int)count } );
                                arrayList.add(new double[] { value+BLOCK_SIZE, (double)(int)count } );
                            }
                        }
                    }

                    double[][] values = new double[2][arrayList.size()];
                    for (int i=0; i<arrayList.size(); ++i) {
                        double[] vals = arrayList.get(i);
                        values[0][i] = vals[0];
                        values[1][i] = vals[1];
                    }

                    dataset.addSeries(activity, values);
                }

                dataSets.add(new DisplayData(AllFeatureExtractor.FEATURE_NAMES[f], dataset));
            }


            System.out.println(dataSets.size()+" graphs to be drawn.");

            //  let the GUI take over
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new DisplayDistribution(dataSets).setVisible(true);
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
                NumberAxis xAxis = new NumberAxis("count");
                xAxis.setAutoRangeIncludesZero(false);
                NumberAxis yAxis = new NumberAxis("values");
                XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES,
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
                JOptionPane.showMessageDialog(DisplayDistribution.this, "Ran out of memory");
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
    private DisplayDistribution(List<DisplayData> dataSets) {
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
