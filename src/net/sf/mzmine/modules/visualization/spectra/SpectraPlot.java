/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

/**
 * 
 */
public class SpectraPlot extends ChartPanel {

    private JFreeChart chart;

    private XYPlot plot;

    private PlotMode plotMode = PlotMode.UNDEFINED;

    // plot color
    private static final Color plotColor = new Color(0, 0, 192);

    // picked peaks color
    private static final Color pickedPeaksColor = Color.red;

    // peak labels color
    private static final Color labelsColor = Color.darkGray;

    // grid color
    private static final Color gridColor = Color.lightGray;

    // data points shape
    private static final Shape dataPointsShape = new Ellipse2D.Float(-2, -2, 5,
            5);

    // title font
    private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
    private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN,
            11);
    private TextTitle chartTitle, chartSubTitle;

    XYBarRenderer centroidRenderer, peakListRenderer;
    XYLineAndShapeRenderer continuousRenderer;

    //public SpectraPlot(SpectraVisualizerWindow visualizer) {
    public SpectraPlot(ActionListener visualizer) {

        super(null, true);

        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // initialize the chart by default time series chart from factory
        chart = ChartFactory.createXYLineChart("", // title
                "m/z", // x-axis label
                "Intensity", // y-axis label
                null, // data set
                PlotOrientation.VERTICAL, // orientation
                false, // create legend?
                true, // generate tooltips?
                false // generate URLs?
        );
        chart.setBackgroundPaint(Color.white);
        setChart(chart);

        // title
        chartTitle = chart.getTitle();
        chartTitle.setMargin(5, 0, 0, 0);
        chartTitle.setFont(titleFont);

        chartSubTitle = new TextTitle();
        chartSubTitle.setFont(subTitleFont);
        chartSubTitle.setMargin(5, 0, 0, 0);
        chart.addSubtitle(chartSubTitle);

        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);

        // set the plot properties
        plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        // set rendering order
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        // set grid properties
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);

        // set crosshair (selection) properties
        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);

        NumberFormat mzFormat = MZmineCore.getMZFormat();
        NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

        // set the X axis (retention time) properties
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setNumberFormatOverride(mzFormat);
        xAxis.setUpperMargin(0.001);
        xAxis.setLowerMargin(0.001);
        xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));

        // set the Y axis (intensity) properties
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setNumberFormatOverride(intensityFormat);

        // set default renderer properties
        continuousRenderer = new XYLineAndShapeRenderer();
        continuousRenderer.setBaseShapesFilled(true);
        continuousRenderer.setDrawOutlines(false);
        continuousRenderer.setUseFillPaint(true);
        continuousRenderer.setSeriesShape(0, dataPointsShape);
        continuousRenderer.setSeriesPaint(0, plotColor);
        continuousRenderer.setBaseFillPaint(plotColor);
        continuousRenderer.setBaseShapesVisible(false);

        centroidRenderer = new XYBarRenderer();
        centroidRenderer.setSeriesShape(0, dataPointsShape);
        centroidRenderer.setSeriesPaint(0, plotColor);

        peakListRenderer = new XYBarRenderer();
        peakListRenderer.setSeriesPaint(0, pickedPeaksColor);

        // set label generator
        SpectraItemLabelGenerator labelGenerator = new SpectraItemLabelGenerator(
                this);
        continuousRenderer.setBaseItemLabelGenerator(labelGenerator);
        continuousRenderer.setBaseItemLabelsVisible(true);
        continuousRenderer.setBaseItemLabelPaint(labelsColor);
        centroidRenderer.setBaseItemLabelGenerator(labelGenerator);
        centroidRenderer.setBaseItemLabelsVisible(true);
        centroidRenderer.setBaseItemLabelPaint(labelsColor);

        // set toolTipGenerator
        SpectraToolTipGenerator spectraToolTipGenerator = new SpectraToolTipGenerator();
        continuousRenderer.setBaseToolTipGenerator(spectraToolTipGenerator);
        centroidRenderer.setBaseToolTipGenerator(spectraToolTipGenerator);

        PeakToolTipGenerator peakToolTipGenerator = new PeakToolTipGenerator();
        peakListRenderer.setBaseToolTipGenerator(peakToolTipGenerator);

        // set focusable state to receive key events
        setFocusable(true);

        // register key handlers
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("LEFT"),
                visualizer, "PREVIOUS_SCAN");
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("RIGHT"),
                visualizer, "NEXT_SCAN");
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('+'),
                visualizer, "ZOOM_IN");
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('-'),
                visualizer, "ZOOM_OUT");

        // add items to popup menu
        JPopupMenu popupMenu = getPopupMenu();
        popupMenu.addSeparator();

        GUIUtils.addMenuItem(popupMenu, "Toggle centroid/continuous mode",
                visualizer, "TOGGLE_PLOT_MODE");
        GUIUtils.addMenuItem(popupMenu,
                "Toggle displaying of data points in continuous mode",
                visualizer, "SHOW_DATA_POINTS");
        GUIUtils.addMenuItem(popupMenu, "Toggle displaying of peak values",
                visualizer, "SHOW_ANNOTATIONS");
        GUIUtils.addMenuItem(popupMenu, "Toggle displaying of picked peaks",
                visualizer, "SHOW_PICKED_PEAKS");

        popupMenu.addSeparator();

        GUIUtils.addMenuItem(popupMenu, "Set axes range", visualizer,
                "SETUP_AXES");
        GUIUtils.addMenuItem(popupMenu, "Set same range to all windows",
                visualizer, "SET_SAME_RANGE");

    }

    /**
     * @param plotMode The plotMode to set.
     */
    public void setPlotMode(PlotMode plotMode) {
        this.plotMode = plotMode;
        if (plotMode == PlotMode.CENTROID)
            plot.setRenderer(0, centroidRenderer);
        else
            plot.setRenderer(0, continuousRenderer);

    }

    public PlotMode getPlotMode() {
        return plotMode;
    }

    public XYPlot getXYPlot() {
        return plot;
    }

    public void switchItemLabelsVisible() {

        boolean itemLabelsVisible = continuousRenderer.getBaseItemLabelsVisible();
        centroidRenderer.setBaseItemLabelsVisible(!itemLabelsVisible);
        continuousRenderer.setBaseItemLabelsVisible(!itemLabelsVisible);
        peakListRenderer.setBaseItemLabelsVisible(!itemLabelsVisible);
    }

    public void switchDataPointsVisible() {

        boolean dataPointsVisible = continuousRenderer.getBaseShapesVisible();
        continuousRenderer.setBaseShapesVisible(!dataPointsVisible);

    }

    public boolean getPickedPeaksVisible() {
        Boolean pickedPeaksVisible = peakListRenderer.getBaseSeriesVisible();
        if (pickedPeaksVisible == null)
            return true;
        return pickedPeaksVisible;
    }

    public void switchPickedPeaksVisible() {

        boolean pickedPeaksVisible = getPickedPeaksVisible();
        peakListRenderer.setBaseSeriesVisible(!pickedPeaksVisible);

    }

    public void setTitle(String title, String subTitle) {
        chartTitle.setText(title);
        chartSubTitle.setText(subTitle);
    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent event) {

        // let the parent handle the event (selection etc.)
        super.mouseClicked(event);

        // request focus to receive key events
        requestFocus();
    }

    public void setDataSets(ScanDataSet scanData, PeakListDataSet peakListData) {

        plot.setDataset(0, scanData);
        if (plotMode == PlotMode.CENTROID)
            plot.setRenderer(0, centroidRenderer);
        else
            plot.setRenderer(0, continuousRenderer);

        if (peakListData != null) {
            plot.setDataset(1, peakListData);
            plot.setRenderer(1, peakListRenderer);
        }
        
    }

}