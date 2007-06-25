/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.dataanalysis.intensityplot;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.NumberFormatter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;

/**
 * 
 */
public class IntensityPlotFrame extends JInternalFrame {

    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
    static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private IntensityPlotDataset dataset;

    public IntensityPlotFrame(IntensityPlotParameters parameters) {
        super("", true, true, true, true);

        String title = "Intensity plot ["
                + parameters.getSourcePeakList() + "]";
        String xAxisLabel = parameters.getXAxisValueSource().toString();
        String yAxisLabel = parameters.getYAxisValueSource().toString();

        // create dataset
        dataset = new IntensityPlotDataset(parameters);

        // create new JFreeChart
        logger.finest("Creating new chart instance");
        JFreeChart chart = ChartFactory.createLineChart(title, xAxisLabel,
                yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true,
                false);
        chart.setBackgroundPaint(Color.white);

        // create chart JPanel
        ChartPanel chartPanel = new ChartPanel(chart);
        add(chartPanel);

        // disable maximum size (we don't want scaling)
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);

        // set title properties
        TextTitle chartTitle = chart.getTitle();
        chartTitle.setMargin(5, 0, 0, 0);
        chartTitle.setFont(titleFont);

        LegendTitle legend = chart.getLegend();
        legend.setItemFont(legendFont);
        legend.setBorder(0, 0, 0, 0);

        // set renderer
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, true);
        renderer.setLinesVisible(false);
        plot.setRenderer(renderer);

        // set tooltip generator
        CategoryToolTipGenerator toolTipGenerator = new IntensityPlotTooltipGenerator();
        renderer.setToolTipGenerator(toolTipGenerator);

        // set y axis properties
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        Desktop desktop = MZmineCore.getDesktop();
        NumberFormatter yAxisFormat = desktop.getIntensityFormat();
        if (parameters.getYAxisValueSource() == IntensityPlotParameters.PeakRTOption) yAxisFormat = desktop.getRTFormat();
        yAxis.setNumberFormatOverride(yAxisFormat);

        setTitle(title);
        setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        setBackground(Color.white);
        pack();

    }

}
