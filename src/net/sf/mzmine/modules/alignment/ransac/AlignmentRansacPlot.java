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
package net.sf.mzmine.modules.alignment.ransac;

import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.Vector;
import net.sf.mzmine.main.MZmineCore;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class AlignmentRansacPlot extends ChartPanel {

	private XYSeriesCollection dataset;
	private JFreeChart chart;
	private TextTitle chartTitle;
	private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);	

	// legend
	private LegendTitle legend;
	private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);
	private XYToolTipGenerator toolTipGenerator;
	
	private NumberFormat rtFormat = MZmineCore.getRTFormat();

	public AlignmentRansacPlot() {
		super(null, true);

		dataset = new XYSeriesCollection();
		chart = ChartFactory.createXYLineChart(
				"",
				null,
				null,
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false);

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

	}

	/**
	 * Remove all series from the chart
	 */
	public void removeSeries() {
		dataset.removeAllSeries();
	}

	/**
	 * Add new serie.
	 * @param v Vector with the alignments
	 * @param Name Name of the type of lipids in this alignment
	 */
	public void addSeries(Vector<AlignStructMol> data, String title) {
		try {
			chart.setTitle(title);
			XYSeries s1 = new XYSeries("Aligned Molecules");
			XYSeries s2 = new XYSeries("Non aligned Molecules");

			for (AlignStructMol point : data) {

				if (point.Aligned) {
					s1.add(point.row1.getPeaks()[0].getRT(), point.row2.getPeaks()[0].getRT());
				} else {
					s2.add(point.row1.getPeaks()[0].getRT(), point.row2.getPeaks()[0].getRT());
				}
			}

			this.dataset.addSeries(s1);
			this.dataset.addSeries(s2);


		} catch (Exception e) {
		}
	}

	
	public void printAlignmentChart(String axisTitleX, String axisTitleY) {
		try {
			toolTipGenerator = new AlignmentPreviewTooltipGenerator(axisTitleX, axisTitleY);

			XYPlot plot = chart.getXYPlot();

			NumberAxis xAxis = new NumberAxis(axisTitleX);
			xAxis.setNumberFormatOverride(rtFormat);
			xAxis.setAutoRangeIncludesZero(false);
			plot.setDomainAxis(xAxis);

			NumberAxis yAxis = new NumberAxis(axisTitleY);
			yAxis.setNumberFormatOverride(rtFormat);			
			yAxis.setAutoRangeIncludesZero(false);		
			plot.setRangeAxis(yAxis);

			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			renderer.setBaseLinesVisible(false);
			renderer.setBaseShapesVisible(true);
			renderer.setBaseToolTipGenerator(toolTipGenerator);
			plot.setRenderer(renderer);

			chart.setBackgroundPaint(Color.white);
			plot.setOutlinePaint(Color.black);

		} catch (Exception e) {
		}
	}
	
}
