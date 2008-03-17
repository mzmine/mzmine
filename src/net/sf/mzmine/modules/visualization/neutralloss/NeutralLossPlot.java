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

package net.sf.mzmine.modules.visualization.neutralloss;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;

/**
 * 
 */
class NeutralLossPlot extends ChartPanel {

    private JFreeChart chart;

    private XYPlot plot;
    private XYItemRenderer renderer;

    private boolean showSpectrumRequest = false;

    private NeutralLossVisualizerWindow visualizer;

    // crosshair (selection) color
    private static final Color crossHairColor = Color.gray;

    // crosshair stroke
    private static final BasicStroke crossHairStroke = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
                    5, 3 }, 0);

    // title font
    private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

    private TextTitle chartTitle;

    private double highlightedMin, highlightedMax;

    NeutralLossPlot(NeutralLossVisualizerWindow visualizer,
            NeutralLossDataSet dataset, int xAxisType) {
        // superconstructor with no chart yet
        // disable off-screen buffering (makes problems with late drawing of the
        // title)
        super(null, false);

        this.visualizer = visualizer;

        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        NumberFormat rtFormat = MZmineCore.getRTFormat();
        NumberFormat mzFormat = MZmineCore.getMZFormat();

        // set the X axis (retention time) properties
        NumberAxis xAxis;
        if (xAxisType == 0) {
            xAxis = new NumberAxis("Precursor mass");
            xAxis.setNumberFormatOverride(mzFormat);
        } else {
            xAxis = new NumberAxis("Retention time");
            xAxis.setNumberFormatOverride(rtFormat);
        }
        xAxis.setUpperMargin(0);
        xAxis.setLowerMargin(0);
        xAxis.setAutoRangeIncludesZero(false);

        // set the Y axis (intensity) properties
        NumberAxis yAxis = new NumberAxis("Neutral loss");
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setNumberFormatOverride(mzFormat);
        yAxis.setUpperMargin(0);
        yAxis.setLowerMargin(0);

        // set the renderer properties
        renderer = new NeutralLossDataPointRenderer(this);

        // tooltips
        renderer.setBaseToolTipGenerator(dataset);

        // set the plot properties
        plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.white);

        // chart properties
        chart = new JFreeChart("", titleFont, plot, false);
        chart.setBackgroundPaint(Color.white);

        setChart(chart);

        // title
        chartTitle = chart.getTitle();
        chartTitle.setMargin(5, 0, 0, 0);
        chartTitle.setFont(titleFont);

        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);

        // set crosshair (selection) properties
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setDomainCrosshairPaint(crossHairColor);
        plot.setRangeCrosshairPaint(crossHairColor);
        plot.setDomainCrosshairStroke(crossHairStroke);
        plot.setRangeCrosshairStroke(crossHairStroke);

        plot.addRangeMarker(new ValueMarker(0));

        // set focusable state to receive key events
        setFocusable(true);

        // register key handlers
        GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("SPACE"),
                visualizer, "SHOW_SPECTRUM");

        // add items to popup menu
        JMenuItem highlightMenuItem;

        highlightMenuItem = new JMenuItem("Highlight precursor m/z range...");
        highlightMenuItem.addActionListener(visualizer);
        highlightMenuItem.setActionCommand("HIGHLIGHT");
        add(highlightMenuItem);

        JPopupMenu popupMenu = getPopupMenu();
        popupMenu.addSeparator();
        popupMenu.add(highlightMenuItem);

    }

    void setTitle(String title) {
        chartTitle.setText(title);
    }

    /**
     * @return Returns the highlightedMin.
     */
    double getHighlightedMin() {
        return highlightedMin;
    }

    /**
     * @param highlightedMin The highlightedMin to set.
     */
    void setHighlightedMin(double highlightedMin) {
        this.highlightedMin = highlightedMin;
    }

    /**
     * @return Returns the highlightedMax.
     */
    double getHighlightedMax() {
        return highlightedMax;
    }

    /**
     * @param highlightedMax The highlightedMax to set.
     */
    void setHighlightedMax(double highlightedMax) {
        this.highlightedMax = highlightedMax;
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

    XYPlot getXYPlot() {
        return plot;
    }

}