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
package net.sf.mzmine.modules.peaklistmethods.alignment.ransac;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import net.sf.mzmine.main.MZmineCore;

public class AlignmentRansacPlot extends ChartPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
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
    private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2,
	    5, 5);
    // titles
    private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
    private TextTitle chartTitle;
    // legend
    private LegendTitle legend;
    private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);
    private XYToolTipGenerator toolTipGenerator;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private XYPlot plot;
    private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();

    public AlignmentRansacPlot() {
	super(null, true);

	dataset = new XYSeriesCollection();
	chart = ChartFactory.createXYLineChart("", null, null, dataset,
		PlotOrientation.VERTICAL, true, true, false);

	chart.setBackgroundPaint(Color.white);
	setChart(chart);

	// title
	chartTitle = chart.getTitle();
	chartTitle.setMargin(5, 0, 0, 0);
	chartTitle.setFont(titleFont);

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

	// set default renderer properties
	XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	renderer.setDefaultLinesVisible(false);
	renderer.setDefaultShapesVisible(true);
	renderer.setSeriesShape(0, dataPointsShape);
	renderer.setSeriesShape(1, dataPointsShape);
	renderer.setSeriesLinesVisible(2, true);
	renderer.setSeriesShapesVisible(2, false);
	renderer.setSeriesPaint(0, Color.RED);
	renderer.setSeriesPaint(1, Color.GRAY);
	renderer.setSeriesPaint(2, Color.BLUE);
	renderer.setDefaultItemLabelPaint(labelsColor);
	renderer.setDrawSeriesLineAsPath(true);

	plot.setRenderer(renderer);

    }

    /**
     * Remove all series from the chart
     */
    public void removeSeries() {
	dataset.removeAllSeries();
    }

    /**
     * Add new serie.
     * 
     * @param v
     *            Vector with the alignments
     * @param Name
     *            Name of the type of lipids in this alignment
     */
    public void addSeries(Vector<AlignStructMol> data, String title,
	    boolean linear) {
	try {
	    chart.setTitle(title);
	    XYSeries s1 = new XYSeries("Aligned pairs");
	    XYSeries s2 = new XYSeries("Non-aligned pairs");
	    XYSeries s3 = new XYSeries("Model");

	    PolynomialFunction function = getPolynomialFunction(data, linear);

	    for (AlignStructMol point : data) {

		if (point.Aligned) {
		    s1.add(point.row1.getPeaks()[0].getRT(),
			    point.row2.getPeaks()[0].getRT());
		} else {
		    s2.add(point.row1.getPeaks()[0].getRT(),
			    point.row2.getPeaks()[0].getRT());
		}
		try {
		    s3.add(function.value(point.row2.getPeaks()[0].getRT()),
			    point.row2.getPeaks()[0].getRT());
		} catch (Exception e) {
		}
	    }

	    this.dataset.addSeries(s1);
	    this.dataset.addSeries(s2);
	    this.dataset.addSeries(s3);

	} catch (Exception e) {
	}
    }

    private PolynomialFunction getPolynomialFunction(
	    Vector<AlignStructMol> list, boolean linear) {
	List<RTs> data = new ArrayList<RTs>();
	for (AlignStructMol m : list) {
	    if (m.Aligned) {
		data.add(new RTs(m.RT2, m.RT));
	    }
	}

	data = this.smooth(data);
	Collections.sort(data, new RTs());

	double[] xval = new double[data.size()];
	double[] yval = new double[data.size()];
	int i = 0;

	for (RTs rt : data) {
	    xval[i] = rt.RT;
	    yval[i++] = rt.RT2;
	}

	int degree = 2;
	if (linear) {
	    degree = 1;
	}

	PolynomialFitter fitter = new PolynomialFitter(degree,
		new GaussNewtonOptimizer(true));
	for (RTs rt : data) {
	    fitter.addObservedPoint(1, rt.RT, rt.RT2);
	}
	try {
	    return fitter.fit();

	} catch (Exception ex) {
	    return null;
	}
    }

    private List<RTs> smooth(List<RTs> list) {
	// Add points to the model in between of the real points to smooth the
	// regression model
	Collections.sort(list, new RTs());

	for (int i = 0; i < list.size() - 1; i++) {
	    RTs point1 = list.get(i);
	    RTs point2 = list.get(i + 1);
	    if (point1.RT < point2.RT - 2) {
		SimpleRegression regression = new SimpleRegression();
		regression.addData(point1.RT, point1.RT2);
		regression.addData(point2.RT, point2.RT2);
		double rt = point1.RT + 1;
		while (rt < point2.RT) {
		    RTs newPoint = new RTs(rt, regression.predict(rt));
		    list.add(newPoint);
		    rt++;
		}

	    }
	}

	return list;
    }

    public void printAlignmentChart(String axisTitleX, String axisTitleY) {
	try {
	    toolTipGenerator = new AlignmentPreviewTooltipGenerator(axisTitleX,
		    axisTitleY);
	    plot.getRenderer().setDefaultToolTipGenerator(toolTipGenerator);
	    NumberAxis xAxis = new NumberAxis(axisTitleX);
	    xAxis.setNumberFormatOverride(rtFormat);
	    xAxis.setAutoRangeIncludesZero(false);
	    plot.setDomainAxis(xAxis);

	    NumberAxis yAxis = new NumberAxis(axisTitleY);
	    yAxis.setNumberFormatOverride(rtFormat);
	    yAxis.setAutoRangeIncludesZero(false);
	    plot.setRangeAxis(yAxis);

	} catch (Exception e) {
	}
    }
}
