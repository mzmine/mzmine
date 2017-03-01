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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;

import net.sf.mzmine.parameters.ParameterSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

public class ProjectionPlotPanel extends ChartPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final Color gridColor = Color.lightGray;
    private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

    private static final float dataPointAlpha = 0.8f;

    private JFreeChart chart;
    private XYPlot plot;

    private ProjectionPlotItemLabelGenerator itemLabelGenerator;
    private ProjectionPlotRenderer spotRenderer;

    public ProjectionPlotPanel(ProjectionPlotWindow masterFrame,
	    ProjectionPlotDataset dataset, ParameterSet parameters) {
	super(null);

	boolean createLegend = false;
	if ((dataset.getNumberOfGroups() > 1)
		&& (dataset.getNumberOfGroups() < 20))
	    createLegend = true;

	chart = ChartFactory.createXYAreaChart("", dataset.getXLabel(),
		dataset.getYLabel(), dataset, PlotOrientation.VERTICAL,
		createLegend, false, false);
	chart.setBackgroundPaint(Color.white);

	setChart(chart);

	// title

	TextTitle chartTitle = chart.getTitle();
	chartTitle.setMargin(5, 0, 0, 0);
	chartTitle.setFont(titleFont);
	chart.removeSubtitle(chartTitle);

	// disable maximum size (we don't want scaling)
	setMaximumDrawWidth(Integer.MAX_VALUE);
	setMaximumDrawHeight(Integer.MAX_VALUE);

	// set the plot properties
	plot = chart.getXYPlot();
	plot.setBackgroundPaint(Color.white);
	plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

	// set grid properties
	plot.setDomainGridlinePaint(gridColor);
	plot.setRangeGridlinePaint(gridColor);

	// set crosshair (selection) properties
	plot.setDomainCrosshairVisible(false);
	plot.setRangeCrosshairVisible(false);

	plot.setForegroundAlpha(dataPointAlpha);

	NumberFormat numberFormat = NumberFormat.getNumberInstance();

	// set the X axis (component 1) properties
	NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
	xAxis.setNumberFormatOverride(numberFormat);

	// set the Y axis (component 2) properties
	NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
	yAxis.setNumberFormatOverride(numberFormat);

	plot.setDataset(dataset);

	spotRenderer = new ProjectionPlotRenderer(plot, dataset);
	itemLabelGenerator = new ProjectionPlotItemLabelGenerator(parameters);
	spotRenderer.setBaseItemLabelGenerator(itemLabelGenerator);
	spotRenderer.setBaseItemLabelsVisible(true);
	spotRenderer
		.setBaseToolTipGenerator(new ProjectionPlotToolTipGenerator(
			parameters));
	plot.setRenderer(spotRenderer);

	// Setup legend
	if (createLegend) {
	    LegendItemCollection legendItemsCollection = new LegendItemCollection();
	    for (int groupNumber = 0; groupNumber < dataset.getNumberOfGroups(); groupNumber++) {
		Object paramValue = dataset.getGroupParameterValue(groupNumber);
		if (paramValue == null) {
		    // No parameter value available: search for raw data files
		    // within this group, and use their names as group's name
		    String fileNames = new String();
		    for (int itemNumber = 0; itemNumber < dataset
			    .getItemCount(0); itemNumber++) {
			String rawDataFile = dataset.getRawDataFile(itemNumber);
			if (dataset.getGroupNumber(itemNumber) == groupNumber)
			    fileNames = fileNames.concat(rawDataFile);
		    }
		    if (fileNames.length() == 0)
			fileNames = "Empty group";

		    paramValue = fileNames;
		}
		Color nextColor = (Color) spotRenderer
			.getGroupPaint(groupNumber);
		Color groupColor = new Color(nextColor.getRed(),
			nextColor.getGreen(), nextColor.getBlue(),
			(int) Math.round(255 * dataPointAlpha));
		legendItemsCollection.add(new LegendItem(paramValue.toString(),
			"-", null, null, spotRenderer.getDataPointsShape(),
			groupColor));
	    }
	    plot.setFixedLegendItems(legendItemsCollection);
	}

    }

    protected void cycleItemLabelMode() {
	itemLabelGenerator.cycleLabelMode();
	spotRenderer.setBaseItemLabelsVisible(true);
    }
}
