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

/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.sf.mzmine.util.AddFilePopupMenu;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.RemoveFilePopupMenu;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

/**
 * 
 */
class BasePeakPlot extends ChartPanel {

    private JFreeChart chart;

    private XYPlot plot;

    // TODO: get these from parameter storage
    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");

    private BasePeakVisualizer visualizer;

    private int numberOfDataSets = 0;

    // plot colors for plotted files, circulated by numberOfDataSets
    private static Color[] plotColors = { new Color(0, 0, 192), // blue
            new Color(192, 0, 0), // red
            new Color(0, 192, 0), // green
            Color.magenta, Color.cyan, Color.orange };
    
    // peak labels color
    private static final Color labelsColor = Color.darkGray;
    
    // grid color
    private static final Color gridColor = Color.lightGray;
    
    //  crosshair (selection) color
    private static Color crossHairColor = Color.gray; 
    
    // crosshair stroke
    private static BasicStroke crossHairStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL, 1.0f, new float[] { 5, 3 }, 0);

    // data points shape
    private static Shape dataPointsShape = new Ellipse2D.Float(-2, -2, 5, 5);
    
    private LegendTitle legend;
    
    // title font
    private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);
    
    private TextTitle chartTitle;

    XYLineAndShapeRenderer defaultRenderer;

    /**
     * Indicates whether we have a request to show spectra visualizer for
     * selected data point. Since the selection (crosshair) is updated with some
     * delay after clicking with mouse, we cannot open the new visualizer
     * immediately. Therefore we place a request and open the visualizer later
     * in chartProgress()
     */
    private boolean showSpectrumRequest = false;

    /**
     * @param chart
     */
    BasePeakPlot(final BasePeakVisualizer visualizer) {
        // superconstructor with no chart yet
        // disable off-screen buffering (makes problems with late drawing of the title)
        super(null, false);

        this.visualizer = visualizer;
        
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // initialize the chart by default time series chart from factory
        chart = ChartFactory.createTimeSeriesChart("", // title
                "Retention time", // x-axis label
                "Base peak intensity", // y-axis label
                null, // no data yet
                true, // create legend?
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

        // the legend was constructed by ChartFactory, we can save it for later
        legend = chart.getLegend();
        chart.removeLegend();

        // set the plot properties
        plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        
        // set grid properties
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);
        
        // set crosshair (selection) properties
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setDomainCrosshairPaint(crossHairColor);
        plot.setRangeCrosshairPaint(crossHairColor);
        plot.setDomainCrosshairStroke(crossHairStroke);
        plot.setRangeCrosshairStroke(crossHairStroke);
        
        // set the X axis (retention time) properties
        DateAxis xAxis = (DateAxis) plot.getDomainAxis();
        xAxis.setDateFormatOverride(rtFormat);
        xAxis.setUpperMargin(0.001);
        xAxis.setLowerMargin(0.001);

        // set the Y axis (intensity) properties
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setNumberFormatOverride(intensityFormat);

        // set default renderer properties
        defaultRenderer = (XYLineAndShapeRenderer) plot.getRenderer();
        defaultRenderer.setShapesFilled(true);
        defaultRenderer.setDrawOutlines(false);
        defaultRenderer.setUseFillPaint(true);
        defaultRenderer.setItemLabelPaint(labelsColor);
        defaultRenderer.setShape(dataPointsShape);

        // set label generator
        BasePeakItemLabelGenerator labelGenerator = new BasePeakItemLabelGenerator(this);
        defaultRenderer.setItemLabelGenerator(labelGenerator);
        defaultRenderer.setItemLabelsVisible(true);

        // set toolTipGenerator
        BasePeakToolTipGenerator toolTipGenerator = new BasePeakToolTipGenerator();
        defaultRenderer.setToolTipGenerator(toolTipGenerator);

        // set focusable state to receive key events
        setFocusable(true);

        // register key handlers
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("LEFT"),
                visualizer, "MOVE_CURSOR_LEFT");
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("RIGHT"),
                visualizer, "MOVE_CURSOR_RIGHT");
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("SPACE"),
                visualizer, "SHOW_SPECTRUM");
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('+'),
                visualizer, "ZOOM_IN");
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('-'),
                visualizer, "ZOOM_OUT");

        // add items to popup menu
        JPopupMenu popupMenu = getPopupMenu();
        popupMenu.addSeparator();
        popupMenu.add(new AddFilePopupMenu(visualizer));
        popupMenu.add(new RemoveFilePopupMenu(visualizer));
        popupMenu.addSeparator();
        GUIUtils.addMenuItem(popupMenu, "Toggle showing peak values",
                visualizer, "SHOW_ANNOTATIONS");
        GUIUtils.addMenuItem(popupMenu, "Toggle showing data points",
                visualizer, "SHOW_DATA_POINTS");
        popupMenu.addSeparator();
        GUIUtils.addMenuItem(popupMenu, "Show spectrum of selected scan",
                visualizer, "SHOW_SPECTRUM");
        GUIUtils.addMenuItem(popupMenu, "Show multiple spectra",
                visualizer, "SHOW_MULTIPLE_SPECTRA");
        

    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent event) {

        // let the parent handle the event (selection etc.)
        super.mouseClicked(event);

        // request focus to receive key events
        requestFocus();

        // if user double-clicked left button, place a request to open a spectrum
        if ((event.getButton() == MouseEvent.BUTTON1)
                && (event.getClickCount() == 2)) {
            showSpectrumRequest = true;
        }

    }

    /**
     * @see org.jfree.chart.event.ChartProgressListener#chartProgress(org.jfree.chart.event.ChartProgressEvent)
     */
    public void chartProgress(ChartProgressEvent event) {
        
        super.chartProgress(event);
        
        if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
            
            visualizer.updateTitle();
            
            if (showSpectrumRequest) {
                showSpectrumRequest = false;
                visualizer.actionPerformed(new ActionEvent(event.getSource(),
                        ActionEvent.ACTION_PERFORMED, "SHOW_SPECTRUM"));
            }
            
        }
    }

    void switchItemLabelsVisible() {

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
                .getRenderer();
        boolean itemLabelsVisible = renderer.isSeriesItemLabelsVisible(0);
        for (int i = 0; i < numberOfDataSets; i++) {
            renderer = (XYLineAndShapeRenderer) plot.getRenderer(i);
            if (renderer != null) {
                renderer.setItemLabelsVisible(!itemLabelsVisible);
            }
        }
    }

    void switchDataPointsVisible() {

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
                .getRenderer();
        boolean dataPointsVisible = renderer.getBaseShapesVisible();
        for (int i = 0; i < numberOfDataSets; i++) {
            renderer = (XYLineAndShapeRenderer) plot.getRenderer(i);
            if (renderer != null) {
                renderer.setBaseShapesVisible(!dataPointsVisible);
            }
        }
    }

    XYPlot getXYPlot() {
        return plot;
    }

    synchronized void addDataset(BasePeakDataSet newSet) {

        plot.setDataset(numberOfDataSets, newSet);

        try {
            XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) defaultRenderer
                    .clone();
            Color rendererColor = plotColors[numberOfDataSets
                    % plotColors.length];
            newRenderer.setPaint(rendererColor);
            newRenderer.setFillPaint(rendererColor);
            plot.setRenderer(numberOfDataSets, newRenderer);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        numberOfDataSets++;

    }

    void showLegend(boolean show) {
        chart.removeLegend();
        if (show) {
            chart.addLegend(legend);
        }
    }
        
    void setTitle(String title) {
        chartTitle.setText(title);
    }
    
}
