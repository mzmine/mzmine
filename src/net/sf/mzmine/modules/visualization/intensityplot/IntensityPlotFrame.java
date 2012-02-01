/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.intensityplot;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
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
    private JFreeChart chart;

    public IntensityPlotFrame(ParameterSet parameters) {
	super("", true, true, true, true);

	PeakList peakList = parameters.getParameter(
		IntensityPlotParameters.peakList).getValue()[0];

	String title = "Intensity plot [" + peakList + "]";
	String xAxisLabel = parameters
		.getParameter(IntensityPlotParameters.xAxisValueSource)
		.getValue().toString();
	String yAxisLabel = parameters
		.getParameter(IntensityPlotParameters.yAxisValueSource)
		.getValue().toString();

	// create dataset
	dataset = new IntensityPlotDataset(parameters);

	// create new JFreeChart
	logger.finest("Creating new chart instance");
	Object xAxisValueSource = parameters.getParameter(
		IntensityPlotParameters.xAxisValueSource).getValue();
	boolean isCombo = (xAxisValueSource instanceof ParameterWrapper)
		&& (((ParameterWrapper) xAxisValueSource).getParameter() instanceof ComboParameter);
	if ((xAxisValueSource == IntensityPlotParameters.rawDataFilesOption)
		|| isCombo) {

	    chart = ChartFactory.createLineChart(title, xAxisLabel, yAxisLabel,
		    dataset, PlotOrientation.VERTICAL, true, true, false);

	    CategoryPlot plot = (CategoryPlot) chart.getPlot();

	    // set renderer
	    StatisticalLineAndShapeRenderer renderer = new StatisticalLineAndShapeRenderer(
		    false, true);
	    renderer.setBaseStroke(new BasicStroke(2));
	    plot.setRenderer(renderer);
	    plot.setBackgroundPaint(Color.white);

	    // set tooltip generator
	    CategoryToolTipGenerator toolTipGenerator = new IntensityPlotTooltipGenerator();
	    renderer.setBaseToolTipGenerator(toolTipGenerator);

	    CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
	    xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

	} else {

	    chart = ChartFactory.createXYLineChart(title, xAxisLabel,
		    yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true,
		    false);

	    XYPlot plot = (XYPlot) chart.getPlot();

	    XYErrorRenderer renderer = new XYErrorRenderer();
	    renderer.setBaseStroke(new BasicStroke(2));
	    plot.setRenderer(renderer);
	    plot.setBackgroundPaint(Color.white);

	    // set tooltip generator
	    XYToolTipGenerator toolTipGenerator = new IntensityPlotTooltipGenerator();
	    renderer.setBaseToolTipGenerator(toolTipGenerator);

	}

	chart.setBackgroundPaint(Color.white);

	// create chart JPanel
	ChartPanel chartPanel = new ChartPanel(chart);
	add(chartPanel, BorderLayout.CENTER);

	IntensityPlotToolBar toolBar = new IntensityPlotToolBar(this);
	add(toolBar, BorderLayout.EAST);

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

	Plot plot = chart.getPlot();

	// set shape provider
	IntensityPlotDrawingSupplier shapeSupplier = new IntensityPlotDrawingSupplier();
	plot.setDrawingSupplier(shapeSupplier);

	// set y axis properties
	NumberAxis yAxis;
	if (plot instanceof CategoryPlot)
	    yAxis = (NumberAxis) ((CategoryPlot) plot).getRangeAxis();
	else
	    yAxis = (NumberAxis) ((XYPlot) plot).getRangeAxis();
	NumberFormat yAxisFormat = MZmineCore.getConfiguration()
		.getIntensityFormat();
	if (parameters.getParameter(IntensityPlotParameters.yAxisValueSource)
		.getValue() == YAxisValueSource.RT)
	    yAxisFormat = MZmineCore.getConfiguration().getRTFormat();
	yAxis.setNumberFormatOverride(yAxisFormat);

	setTitle(title);
	setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
	setBackground(Color.white);
	pack();

    }

    JFreeChart getChart() {
	return chart;
    }

}
