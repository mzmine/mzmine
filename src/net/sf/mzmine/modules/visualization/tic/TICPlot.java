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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.GUIUtils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

/**
 * 
 */
class TICPlot extends ChartPanel {

    private JFreeChart chart;

    private XYPlot plot;

    private TICVisualizerWindow visualizer;

    // plot colors for plotted files, circulated by numberOfDataSets
    private static final Color[] plotColors = { new Color(0, 0, 192), // blue
            new Color(192, 0, 0), // red
            new Color(0, 192, 0), // green
            Color.magenta, Color.cyan, Color.orange };

    private static final Color[] peakColors = { Color.pink, Color.red,
            Color.yellow };

    // peak labels color
    private static final Color labelsColor = Color.darkGray;

    // grid color
    private static final Color gridColor = Color.lightGray;

    // crosshair (selection) color
    private static final Color crossHairColor = Color.gray;

    // crosshair stroke
    private static final BasicStroke crossHairStroke = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
                    5, 3 }, 0);

    // data points shape
    private static final Shape dataPointsShape = new Ellipse2D.Float(-2, -2, 5,
            5);

    // titles
    private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
    private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN,
            11);
    private TextTitle chartTitle, subTitle;

    // legend
    private LegendTitle legend;
    private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);

    // renderers
    private XYLineAndShapeRenderer defaultRenderer;

    // datasets counter
    private int numOfDataSets = 0, numOfPeaks = 0;

    /**
     * Indicates whether we have a request to show spectra visualizer for
     * selected data point. Since the selection (crosshair) is updated with some
     * delay after clicking with mouse, we cannot open the new visualizer
     * immediately. Therefore we place a request and open the visualizer later
     * in chartProgress()
     */
    private boolean showSpectrumRequest = false;

    /**
     * 
     */
    TICPlot(final TICVisualizerWindow visualizer) {

        super(null, true);

        this.visualizer = visualizer;

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        String yAxisLabel;
        if (visualizer.getPlotType() == TICVisualizerParameters.plotTypeBP)
            yAxisLabel = "Base peak intensity";
        else
            yAxisLabel = "Total ion intensity";

        // initialize the chart by default time series chart from factory
        chart = ChartFactory.createXYLineChart("", // title
                "Retention time", // x-axis label
                yAxisLabel, // y-axis label
                null, // data set
                PlotOrientation.VERTICAL, // orientation
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
        );
        chart.setBackgroundPaint(Color.white);
        setChart(chart);

        // title
        chartTitle = chart.getTitle();
        chartTitle.setFont(titleFont);
        chartTitle.setMargin(5, 0, 0, 0);

        subTitle = new TextTitle();
        subTitle.setFont(subTitleFont);
        subTitle.setMargin(5, 0, 0, 0);
        chart.addSubtitle(subTitle);

        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);

        // legend constructed by ChartFactory
        legend = chart.getLegend();
        legend.setItemFont(legendFont);
        legend.setFrame(BlockBorder.NONE);

        // set the plot properties
        plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

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

        Desktop desktop = MZmineCore.getDesktop();
        NumberFormat rtFormat = desktop.getRTFormat();
        NumberFormat intensityFormat = desktop.getIntensityFormat();

        // set the X axis (retention time) properties
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setNumberFormatOverride(rtFormat);
        xAxis.setUpperMargin(0.001);
        xAxis.setLowerMargin(0.001);

        // set the Y axis (intensity) properties
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setNumberFormatOverride(intensityFormat);

        // set default renderer properties
        defaultRenderer = (XYLineAndShapeRenderer) plot.getRenderer();
        defaultRenderer.setBaseShapesFilled(true);
        defaultRenderer.setDrawOutlines(false);
        defaultRenderer.setUseFillPaint(true);
        defaultRenderer.setBaseItemLabelPaint(labelsColor);

        // set label generator
        XYItemLabelGenerator labelGenerator = new TICItemLabelGenerator(this,
                visualizer);
        defaultRenderer.setBaseItemLabelGenerator(labelGenerator);
        defaultRenderer.setBaseItemLabelsVisible(true);

        // set toolTipGenerator
        XYToolTipGenerator toolTipGenerator = new TICToolTipGenerator();
        defaultRenderer.setBaseToolTipGenerator(toolTipGenerator);

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
        GUIUtils.addMenuItem(popupMenu, "Show multiple spectra", visualizer,
                "SHOW_MULTIPLE_SPECTRA");

    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent event) {

        // let the parent handle the event (selection etc.)
        super.mouseClicked(event);

        // request focus to receive key events
        requestFocus();

        // if user double-clicked left button, place a request to open a
        // spectrum
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

        XYItemRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        boolean itemLabelsVisible = renderer.isSeriesItemLabelsVisible(0);
        for (int i = 0; i < plot.getDatasetCount(); i++) {
            renderer = (XYItemRenderer) plot.getRenderer(i);
            if (renderer != null) {
                renderer.setBaseItemLabelsVisible(!itemLabelsVisible);
            }
        }
    }

    void switchDataPointsVisible() {

        for (int i = 0; i < plot.getDatasetCount(); i++) {
            if (plot.getRenderer(i).getClass() == XYLineAndShapeRenderer.class) {
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(i);
                if (renderer != null) {
                    boolean dataPointsVisible = renderer.getBaseShapesVisible();
                    renderer.setBaseShapesVisible(!dataPointsVisible);
                }
            }
        }
    }

    XYPlot getXYPlot() {
        return plot;
    }

    synchronized void addTICDataset(TICDataSet newSet) {

        plot.setDataset(numOfDataSets + numOfPeaks, newSet);

        try {
            XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) defaultRenderer.clone();
            Color rendererColor = plotColors[numOfDataSets % plotColors.length];
            newRenderer.setSeriesPaint(0, rendererColor);
            newRenderer.setSeriesFillPaint(0, rendererColor);
            newRenderer.setSeriesShape(0, dataPointsShape);
            plot.setRenderer(numOfDataSets + numOfPeaks, newRenderer);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        numOfDataSets++;

    }

    synchronized void addPeakDataset(PeakDataSet newSet) {

        plot.setDataset(numOfDataSets + numOfPeaks, newSet);

        XYAreaRenderer newRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        Color peakColor = peakColors[numOfPeaks % peakColors.length];
        newRenderer.setSeriesPaint(0, peakColor);
        plot.setRenderer(numOfDataSets + numOfPeaks, newRenderer);

        numOfPeaks++;

    }

    void setTitle(String titleText, String subTitleText) {
        chartTitle.setText(titleText);
        subTitle.setText(subTitleText);
    }

}
