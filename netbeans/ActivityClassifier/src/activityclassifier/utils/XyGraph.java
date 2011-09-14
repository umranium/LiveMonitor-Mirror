/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.utils;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFrame;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

/**
 *
 * @author Umran
 */
public class XyGraph {

    private static class DisplayChartRunnable
            implements Runnable
    {

        private DefaultXYDataset dataset;

        public DisplayChartRunnable( DefaultXYDataset dataset ) {
            this.dataset = dataset;
        }

        public void run() {

            NumberAxis xAxis = new NumberAxis("time (s)");
            xAxis.setAutoRangeIncludesZero(false);
            NumberAxis yAxis = new NumberAxis("values");
            XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES,
                                                                 new DataToolTipGenerator());

            XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
            plot.setOrientation(PlotOrientation.VERTICAL);
            JFreeChart chart = new JFreeChart(
                    "Analyzed Data",
                    JFreeChart.DEFAULT_TITLE_FONT,
                    plot,
                    true);
            
            plot.setBackgroundPaint(new Color(200, 200, 200, 220));

            ChartFrame frame = new ChartFrame(chart.getTitle().getText(), chart, true);
            frame.pack();
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        }
    }

    public static void displayChart( DefaultXYDataset dataset ) {
        java.awt.EventQueue.invokeLater(new DisplayChartRunnable(dataset));
    }

    private static class DataToolTipGenerator
            implements XYToolTipGenerator
    {

        public String generateToolTip( XYDataset dataset, int series, int item ) {
            return String.format("(%.0f, %.0f)", dataset.getX(series, item).doubleValue(), dataset.
                    getY(series, item).doubleValue());
        }
    }
    
}
