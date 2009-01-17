/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.logging.Logger;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramDataType;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramPlotDataset;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

public class Histogram extends ChartPanel{

	private Logger logger = Logger.getLogger(this.getClass().getName());

	// plot colors for diagonal lines
	private static final Color[] plotDiagonalColors = { new Color(165, 42, 42),
			Color.BLACK, new Color(165, 42, 42) };

	// grid color
	private static final Color gridColor = Color.lightGray;

	// crosshair (selection) color
	private static final Color crossHairColor = Color.gray;

	// crosshair stroke
	private static final BasicStroke crossHairStroke = new BasicStroke(1,
			BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
					5, 3 }, 0);
	
	// titles
	private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
	private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN,
			11);
	private TextTitle chartTitle, chartSubTitle;
	
	// legend
	private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);

	private JFreeChart chart;
	private XYPlot plot;
	private HistogramPanel histogramPanel;
	private boolean showItemName = false;
	private HistogramPlotDataset dataSet;
	
	public Histogram(HistogramPanel masterFrame) {

		super(null, true);

		this.histogramPanel = masterFrame;

		// initialize the chart by default time series chart from factory
		chart = ChartFactory.createHistogram("", // title
				"", // x-axis label
				"", // y-axis label
				null, // data set
				PlotOrientation.VERTICAL, // orientation
				true, // create legend
				false, // generate tooltips
				false // generate URLs
				);
		
		// title
		chartTitle = chart.getTitle();
		chartTitle.setFont(titleFont);
		chartTitle.setMargin(5, 0, 0, 0);

		chartSubTitle = new TextTitle();
		chartSubTitle.setFont(subTitleFont);
		chartSubTitle.setMargin(5, 0, 0, 0);
		chart.addSubtitle(chartSubTitle);
		
		// legend constructed by ChartFactory
		LegendTitle legend = chart.getLegend();
		legend.setItemFont(legendFont);
		legend.setFrame(BlockBorder.NONE);


		chart.setBackgroundPaint(Color.white);
		setChart(chart);

		// disable maximum size (we don't want scaling)
		setMaximumDrawWidth(Integer.MAX_VALUE);
		setMaximumDrawHeight(Integer.MAX_VALUE);

		// set the plot properties
		plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
		plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

		// set grid properties
		plot.setDomainGridlinePaint(gridColor);
		plot.setRangeGridlinePaint(gridColor);

		// set crosshair (selection) properties
		plot.setDomainCrosshairVisible(false);
		plot.setRangeCrosshairVisible(true);

		// set the logarithmic axis
		NumberAxis axisDomain = new NumberAxis();
		axisDomain.setMinorTickCount(1);
		axisDomain.setAutoRange(true);

		NumberAxis axisRange = new NumberAxis();
		axisRange.setMinorTickCount(1);
		axisRange.setAutoRange(true);

		plot.setDomainAxis(axisDomain);
		plot.setRangeAxis(axisRange);
		
		plot.setRangeCrosshairPaint(crossHairColor);
		plot.setRenderer(new ClusteredXYBarRenderer());


		this.setMinimumSize(new Dimension(400, 400));
		this.setDismissDelay(Integer.MAX_VALUE);
		this.setInitialDelay(0);

	}

	/**
	 * 
	 */
	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);

		String command = event.getActionCommand();

		if (command.equals("SETUP_AXES")) {
			histogramPanel.actionPerformed(event);
			return;
		}

	}

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent event) {
		super.mouseClicked(event);
		requestFocus();
		if (event.getButton() == MouseEvent.BUTTON1)
			showItemName = true;
	}

	/**
	 * @see org.jfree.chart.event.ChartProgressListener#chartProgress(org.jfree.chart.event.ChartProgressEvent)
	 */
	@Override
	public void chartProgress(ChartProgressEvent event) {
		// super.chartProgress(event);
		if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
			if (showItemName) {
				showItemName = false;
				histogramPanel.actionPerformed(new ActionEvent(event
						.getSource(), ActionEvent.ACTION_PERFORMED,
						"SHOW_ITEM_NAME"));
			}
		}
	}

	synchronized public void addDataset(HistogramPlotDataset newSet, HistogramDataType dataType) {
		dataSet = newSet;
		setAxisNumberFormat(dataType);
		plot.getDomainAxis().setLabel(dataSet.getPeakList().getName());
		plot.setDataset(0, newSet);
		setTitle(dataSet.getPeakList().getName(), dataType.getText());
	}
	
	public void setAxisNumberFormat(HistogramDataType dataType){
		
		NumberFormat formatter = null;
		switch (dataType){
		case AREA:
			formatter = MZmineCore.getIntensityFormat();
			break;
		case MASS:
			formatter = MZmineCore.getMZFormat();
			break;
		case HEIGHT:
			formatter = MZmineCore.getIntensityFormat();
			break;
		case RT:
			formatter = MZmineCore.getRTFormat();
			break;
		}
		((NumberAxis)plot.getDomainAxis()).setNumberFormatOverride(formatter);
		
	}
	
	void setTitle(String titleText, String subTitleText) {
		chartTitle.setText(titleText);
		chartSubTitle.setText(subTitleText);
	}

	public HistogramPanel getMaster() {
		return histogramPanel;
	}

	public XYPlot getXYPlot() {
		return plot;
	}


}
