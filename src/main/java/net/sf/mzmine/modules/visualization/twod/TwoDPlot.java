/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.SaveImage;
import net.sf.mzmine.util.SaveImage.FileType;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleEdge;

import com.google.common.collect.Range;

/**
 * 
 */
class TwoDPlot extends ChartPanel {

    private static final long serialVersionUID = 1L;

    // crosshair (selection) color
    private static final Color crossHairColor = Color.gray;

    // crosshair stroke
    private static final BasicStroke crossHairStroke = new BasicStroke(1,
	    BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
		    5, 3 }, 0);

    private RawDataFile rawDataFile;
    private Range<Double> rtRange, mzRange;

    private JFreeChart chart;

    private BaseXYPlot plot;

    private PeakDataRenderer peakDataRenderer;

    // title font
    private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
    private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN,
	    11);
    private TextTitle chartTitle, chartSubTitle;

    private NumberAxis xAxis, yAxis;

    private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    // private TwoDItemRenderer renderer;

    TwoDPlot(RawDataFile rawDataFile, TwoDVisualizerWindow visualizer,
	    TwoDDataSet dataset, Range<Double> rtRange, Range<Double> mzRange,String whichPlotTypeStr) {

		super(null, true);

		this.rawDataFile = rawDataFile;
		this.rtRange = rtRange;
		this.mzRange = mzRange;

		setBackground(Color.white);
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

		// set the X axis (retention time) properties
		xAxis = new NumberAxis("Retention time");
		xAxis.setAutoRangeIncludesZero(false);
		xAxis.setNumberFormatOverride(rtFormat);
		xAxis.setUpperMargin(0);
		xAxis.setLowerMargin(0);

		// set the Y axis (intensity) properties
		yAxis = new NumberAxis("m/z");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setNumberFormatOverride(mzFormat);
		yAxis.setUpperMargin(0);
		yAxis.setLowerMargin(0);

		// set the plot properties
		if (whichPlotTypeStr=="default") {
			plot = new TwoDXYPlot(dataset, rtRange, mzRange, xAxis, yAxis);
		}
		else if (whichPlotTypeStr=="point2D") {
			plot = new PointTwoDXYPlot(dataset, rtRange, mzRange, xAxis, yAxis);
		}
	    plot.setBackgroundPaint(Color.white);
	    plot.setDomainGridlinesVisible(false);
	    plot.setRangeGridlinesVisible(false);

	    // chart properties
	    chart = new JFreeChart("", titleFont, plot, false);
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

	    // set crosshair (selection) properties
	    plot.setRangeCrosshairVisible(false);
	    plot.setDomainCrosshairVisible(true);
	    plot.setDomainCrosshairPaint(crossHairColor);
	    plot.setDomainCrosshairStroke(crossHairStroke);

	    // set rendering order
	    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

	    peakDataRenderer = new PeakDataRenderer();

	    JMenuItem plotTypeMenuItem = new JMenuItem(
	    	"Toggle centroid/continuous mode");
	    plotTypeMenuItem.addActionListener(visualizer);
		plotTypeMenuItem.setActionCommand("SWITCH_PLOTMODE");
		add(plotTypeMenuItem);

		JPopupMenu popupMenu = getPopupMenu();
		popupMenu.addSeparator();
		popupMenu.add(plotTypeMenuItem);

		// Add EMF and EPS options to the save as menu
		JMenuItem saveAsMenu = (JMenuItem) popupMenu.getComponent(3);
		GUIUtils.addMenuItem(saveAsMenu, "EMF...", this, "SAVE_EMF");
		GUIUtils.addMenuItem(saveAsMenu, "EPS...", this, "SAVE_EPS");

    }

    @Override
    public void actionPerformed(final ActionEvent event) {

	super.actionPerformed(event);

	final String command = event.getActionCommand();

	if ("SAVE_EMF".equals(command)) {

	    JFileChooser chooser = new JFileChooser();
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "EMF Image", "EMF");
	    chooser.setFileFilter(filter);
	    int returnVal = chooser.showSaveDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	       String file = chooser.getSelectedFile().getPath();
	       if (!file.toLowerCase().endsWith(".emf")) file += ".emf";
	       
	       int width = (int) this.getSize().getWidth();
	       int height = (int) this.getSize().getHeight(); 

	       // Save image
	       SaveImage SI = new SaveImage(getChart(), file, width, height, FileType.EMF);
	       new Thread(SI).start();
	       
	    }
	}

	if ("SAVE_EPS".equals(command)) {
	    
	    JFileChooser chooser = new JFileChooser();
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "EPS Image", "EPS");
	    chooser.setFileFilter(filter);
	    int returnVal = chooser.showSaveDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	       String file = chooser.getSelectedFile().getPath();
	       if (!file.toLowerCase().endsWith(".eps")) file += ".eps";
	       
	       int width = (int) this.getSize().getWidth();
	       int height = (int) this.getSize().getHeight(); 
	       
	       // Save image
	       SaveImage SI = new SaveImage(getChart(), file, width, height, FileType.EPS);
	       new Thread(SI).start();
	       
	    }
	    
	}
    }

    BaseXYPlot getXYPlot() {
	return plot;
    }

    void setTitle(String title) {
	chartTitle.setText(title);
    }

    void switchDataPointsVisible() {

	boolean dataPointsVisible = peakDataRenderer.getBaseShapesVisible();
	peakDataRenderer.setBaseShapesVisible(!dataPointsVisible);

    }

    void setPeaksNotVisible() {

	if (plot.getDataset(1) == null)
	    return;
	plot.setRenderer(1, null);
    }

    PlotMode getPlotMode() {
	return plot.getPlotMode();
    }

    void setPlotMode(PlotMode plotMode) {
	plot.setPlotMode(plotMode);
    }

    void loadPeakList(PeakList peakList) {

	PeakDataSet peaksDataSet = new PeakDataSet(rawDataFile, peakList,
		rtRange, mzRange);

	plot.setDataset(1, peaksDataSet);
	plot.setRenderer(1, peakDataRenderer);
    }

    public String getToolTipText(MouseEvent event) {

	String tooltip = super.getToolTipText(event);

	if (tooltip == null) {
	    int mouseX = event.getX();
	    int mouseY = event.getY();
	    Rectangle2D plotArea = getScreenDataArea();
	    RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
	    RectangleEdge yAxisEdge = plot.getRangeAxisEdge();
	    double rt = (double) xAxis.java2DToValue(mouseX, plotArea,
		    xAxisEdge);
	    double mz = (double) yAxis.java2DToValue(mouseY, plotArea,
		    yAxisEdge);

	    tooltip = "Retention time: " + rtFormat.format(rt) + "\nm/z: "
		    + mzFormat.format(mz);
	}

	return tooltip;

    }

    public void showPeaksTooltips(boolean mode) {
	if (mode) {
	    PeakToolTipGenerator toolTipGenerator = new PeakToolTipGenerator();
	    this.peakDataRenderer.setBaseToolTipGenerator(toolTipGenerator);
	} else {
	    this.peakDataRenderer.setBaseToolTipGenerator(null);
	}
    }

    public void setLogScale(boolean logscale) {
	if (plot != null) {
	    plot.setLogScale(logscale);
	}
    }
}
