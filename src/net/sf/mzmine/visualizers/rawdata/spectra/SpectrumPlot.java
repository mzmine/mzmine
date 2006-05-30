/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

/**
 * 
 */
class SpectrumPlot extends ChartPanel {

    private JFreeChart chart;

    private XYPlot plot;

    // TODO: get these from parameter storage
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");
    private static NumberFormat mzFormat = new DecimalFormat("0.00");

    static enum PlotMode {
        CENTROID, CONTINUOUS
    };

    private PlotMode plotMode = PlotMode.CONTINUOUS;

    static final Color plotColor = new Color(0, 0, 192);
    
    // data points shape
    private static Shape dataPointsShape = new Ellipse2D.Float(-2, -2, 5, 5);
    
    // title font
    private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

    private TextTitle chartTitle;
    
    XYBarRenderer centroidRenderer;
    XYLineAndShapeRenderer continuousRenderer;

    SpectrumPlot(SpectrumVisualizer visualizer, XYDataset dataset) {
        // superconstructor with no chart yet
        // disable off-screen buffering (makes problems with late drawing of the title)
        super(null, false);

        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // initialize the chart by default time series chart from factory
        chart = ChartFactory.createXYLineChart("", // title
                "m/z", // x-axis label
                "Intensity", // y-axis label
                dataset, // data set
                PlotOrientation.VERTICAL, // orientation
                false, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );
        chart.setBackgroundPaint(Color.white);
        setChart(chart);

        // title
        chartTitle = chart.getTitle();
        chartTitle.setMargin(5,0,0,0);
        chartTitle.setFont(titleFont);
        
        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);

        // set the plot properties
        plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        // set grid properties
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);

        // set crosshair (selection) properties
        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);

        // set the X axis (retention time) properties
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setNumberFormatOverride(mzFormat);
        xAxis.setUpperMargin(0.001);
        xAxis.setLowerMargin(0.001);
        xAxis.setTickLabelInsets(new RectangleInsets(0,0,20,20));

        // set the Y axis (intensity) properties
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setNumberFormatOverride(intensityFormat);

        // set default renderer properties
        continuousRenderer = new XYLineAndShapeRenderer();
        continuousRenderer.setShapesFilled(true);
        continuousRenderer.setDrawOutlines(false);
        continuousRenderer.setUseFillPaint(true);
        continuousRenderer.setShape(dataPointsShape);
        continuousRenderer.setPaint(plotColor);
        continuousRenderer.setFillPaint(plotColor);
        continuousRenderer.setBaseShapesVisible(false);

        centroidRenderer = new XYBarRenderer();
        centroidRenderer.setBaseShape(dataPointsShape);
        centroidRenderer.setPaint(plotColor);
        
        // set label generator
        SpectrumItemLabelGenerator labelGenerator = new SpectrumItemLabelGenerator(
                this);
        continuousRenderer.setItemLabelGenerator(labelGenerator);
        continuousRenderer.setItemLabelsVisible(true);
        centroidRenderer.setItemLabelGenerator(labelGenerator);
        centroidRenderer.setItemLabelsVisible(true);

        // set toolTipGenerator
        SpectrumToolTipGenerator toolTipGenerator = new SpectrumToolTipGenerator();
        continuousRenderer.setToolTipGenerator(toolTipGenerator);
        centroidRenderer.setToolTipGenerator(toolTipGenerator);

        // add items to popup menu
        JMenuItem annotationsMenuItem, dataPointsMenuItem, plotTypeMenuItem;

        annotationsMenuItem = new JMenuItem("Toggle displaying of peak values");
        annotationsMenuItem.addActionListener(visualizer);
        annotationsMenuItem.setActionCommand("SHOW_ANNOTATIONS");

        dataPointsMenuItem = new JMenuItem("Toggle displaying of data points in continuous mode");
        dataPointsMenuItem.addActionListener(visualizer);
        dataPointsMenuItem.setActionCommand("SHOW_DATA_POINTS");

        plotTypeMenuItem = new JMenuItem("Toggle centroid/continuous mode");
        plotTypeMenuItem.addActionListener(visualizer);
        plotTypeMenuItem.setActionCommand("TOGGLE_PLOT_MODE");
        add(plotTypeMenuItem);

        JPopupMenu popupMenu = getPopupMenu();
        popupMenu.addSeparator();
        popupMenu.add(annotationsMenuItem);
        popupMenu.add(dataPointsMenuItem);
        popupMenu.add(plotTypeMenuItem);

    }

    /**
     * @param plotMode
     *            The plotMode to set.
     */
    void setPlotMode(PlotMode plotMode) {
        this.plotMode = plotMode;
        if (plotMode == PlotMode.CENTROID) 
            plot.setRenderer(centroidRenderer);
         else 
            plot.setRenderer(continuousRenderer);
            
    }

    PlotMode getPlotMode() {
        return plotMode;
    }

    XYPlot getXYPlot() {
        return plot;
    }

    void switchItemLabelsVisible() {

        boolean itemLabelsVisible = continuousRenderer.isSeriesItemLabelsVisible(0);
        centroidRenderer.setItemLabelsVisible(!itemLabelsVisible);
        continuousRenderer.setItemLabelsVisible(!itemLabelsVisible);
    }

    void switchDataPointsVisible() {

        boolean dataPointsVisible = continuousRenderer.getBaseShapesVisible();

        continuousRenderer.setBaseShapesVisible(!dataPointsVisible);

    }
    
    void setTitle(String title) {
        chartTitle.setText(title);
    }

}