/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.util.GUIUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class WindowDetectionSupplier extends AlgorithmSupplier
{
    private static final String NAME = "Window Detection";

    public static final DoubleParameter PREF_WINDOW_WIDTH =
            new DoubleParameter("Window Detection: Preferred width (min)",
                    "Preferred width of a window",
                    NumberFormat.getNumberInstance(), 0.5);

    public static final IntegerParameter MIN_NUM_PEAKS =
            new IntegerParameter("Window Detection: Min number of peaks",
                    "Minimum number of peaks in a window",
                    5);

    private static final Parameter<?>[] PARAMETERS = new Parameter<?>[] {PREF_WINDOW_WIDTH, MIN_NUM_PEAKS};

    private final JPanel pnlPlotXY;

    WindowDetectionSupplier()
    {
        // TIC plot.
        ChartPanel plot = new Plot();
//        plot.setMinimumSize(MINIMUM_TIC_DIMENSIONS);

        // Panel for XYPlot.
        pnlPlotXY = new JPanel(new BorderLayout());
        pnlPlotXY.setBackground(Color.white);
        pnlPlotXY.add(plot, BorderLayout.CENTER);
        GUIUtils.addMarginAndBorder(pnlPlotXY, 10);
    }

    @Override
    public String getName() {return NAME;}

    @Override
    public Parameter[] getParameters() {return PARAMETERS;}

    @Override
    public JPanel getPanel() {return pnlPlotXY;}

    @Override
    public void actionPerformed(ActionEvent ae) {}

    @Override
    public void updateData(DataProvider dataProvider)
    {
        ParameterSet parameterSet = dataProvider.getParameterSet();


        RetTimeClusterer.Item[] ranges = dataProvider.getRanges(false);



        RetTimeClusterer clusterer = new RetTimeClusterer()
    }


    private static class Plot extends ChartPanel
    {
        private static final Color[] COLORS = new Color[] {
                Color.BLUE, Color.PINK, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.GREEN, Color.RED};

        private static final int SERIES_ID = 0;

        private final JFreeChart chart;
        private final XYPlot plot;
        private final NumberAxis xAxis, yAxis;
        private final XYSeriesCollection xyDataset;

        Plot()
        {
            super(null, true);

            setBackground(Color.white);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            xAxis = new NumberAxis("Retention time");
            xAxis.setAutoRangeIncludesZero(false);
            xAxis.setUpperMargin(0);
            xAxis.setLowerMargin(0);

            yAxis = new NumberAxis("m/z values");
            yAxis.setAutoRangeIncludesZero(false);
            yAxis.setUpperMargin(0);
            yAxis.setLowerMargin(0);

            xyDataset = new XYSeriesCollection();

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {
                @Override
                public Paint getItemPaint(int row, int col) {
                    String description = xyDataset.getSeries(row).getDescription();
                    return COLORS[Integer.parseInt(description) % COLORS.length];
                }
            };

//        renderer.setDotHeight(3);
//        renderer.setDotWidth(3);
//        renderer.setBaseShapesVisible(false);
            renderer.setDefaultShapesVisible(false);

            plot = new XYPlot(xyDataset, xAxis, yAxis, renderer);
            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinesVisible(true);
            plot.setRangeGridlinesVisible(true);

            chart = new JFreeChart("",
                    new Font("SansSerif", Font.BOLD, 12),
                    plot,
                    false);
            chart.setBackgroundPaint(Color.white);

            super.setChart(chart);
        }

        void updateData() {
            xyDataset.removeAllSeries();
            xyDataset.setNotify(false);

            int seriesID = 0;
            int colorID = 0;
            for (RetTimeClusterer.Cluster c : clusters)
            {
                for (RetTimeClusterer.Item range : c.ranges) {
                    XYSeries series = new XYSeries(seriesID++);
                    series.setDescription(Integer.toString(colorID));
                    series.add(range.getInterval().lowerEndpoint().doubleValue(), range.getMZ());
                    series.add(range.getInterval().upperEndpoint().doubleValue(), range.getMZ());
                    xyDataset.addSeries(series);
                }
                ++colorID;
//            toolTips.add(String.format("M/z: %.2f\nIntensity: %.0f",
//                    peak.getMZ(), peak.getIntensity()));
            }

            xyDataset.setNotify(true);
        }
    }
}
