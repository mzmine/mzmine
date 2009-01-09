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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel.DiagonalLabelGenerator;
import net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel.DiagonalPlotDataset;
import net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel.ScatterPlotDataSet;
import net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel.ScatterPlotItemLabelGenerator;
import net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel.ScatterPlotToolTipGenerator;
import net.sf.mzmine.modules.visualization.scatterplot.plottooltip.CustomToolTipManager;
import net.sf.mzmine.modules.visualization.scatterplot.plottooltip.CustomToolTipProvider;
import net.sf.mzmine.modules.visualization.scatterplot.plottooltip.PeakSummaryComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.ui.RectangleInsets;

public class ScatterPlot extends ChartPanel implements CustomToolTipProvider{

	//private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final Shape dataPointsShape = new Ellipse2D.Float(-2.5f,
			-2.5f, 5, 5);

	private static final Shape dataPointsShape2 = new Ellipse2D.Float(-3.5f,
			-3.5f, 7, 7);

	private static final Shape diagonalPointsShape = new Rectangle2D.Float(-3,
			-3, 6, 6);

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

	private JFreeChart chart;
	private XYPlot plot;
	private ScatterPlotPanel scatterPlotPanel;
	private boolean showItemName = false;

	private XYLineAndShapeRenderer defaultRenderer;
	private XYLineAndShapeRenderer diagonalLineRenderer;
	private ScatterPlotToolTipGenerator toolTipGenerator;

	// datasets counter
	private int numOfDataSets = 0, numDiagonals = 0;

	// toolTip
	private ScatterPlotDataSet dataSet;
	
	private CustomToolTipManager ttm;

	public ScatterPlot(ScatterPlotPanel masterFrame) {

		super(null, true);

		this.scatterPlotPanel = masterFrame;

		// initialize the chart by default time series chart from factory
		chart = ChartFactory.createXYLineChart("", // title
				"", // x-axis label
				"", // y-axis label
				null, // data set
				PlotOrientation.VERTICAL, // orientation
				false, // create legend
				false, // generate tooltips
				false // generate URLs
				);

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
		plot.setRangeCrosshairVisible(false);

		// set the logarithmic axis
		LogAxis logAxisDomain = new LogAxis();
		logAxisDomain.setMinorTickCount(1);
		logAxisDomain.setNumberFormatOverride(MZmineCore.getIntensityFormat());
		logAxisDomain.setAutoRange(true);

		LogAxis logAxisRange = new LogAxis();
		logAxisRange.setMinorTickCount(1);
		logAxisRange.setNumberFormatOverride(MZmineCore.getIntensityFormat());
		logAxisRange.setAutoRange(true);

		plot.setDomainAxis(logAxisDomain);
		plot.setRangeAxis(logAxisRange);

		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainCrosshairPaint(crossHairColor);
		plot.setRangeCrosshairPaint(crossHairColor);
		plot.setDomainCrosshairStroke(crossHairStroke);
		plot.setRangeCrosshairStroke(crossHairStroke);

		// set default renderer properties
		defaultRenderer = new ScatterPlotRenderer(false, true);
		diagonalLineRenderer = new ScatterPlotRenderer(true, false);

		// set toolTipGenerator
		toolTipGenerator = new ScatterPlotToolTipGenerator();
		defaultRenderer.setBaseToolTipGenerator(toolTipGenerator);

		XYItemLabelGenerator ItemlabelGenerator = new ScatterPlotItemLabelGenerator();
		defaultRenderer.setBaseItemLabelGenerator(ItemlabelGenerator);
		defaultRenderer.setBaseItemLabelFont(new Font("SansSerif", Font.BOLD,
				11));
		defaultRenderer.setBaseItemLabelPaint(Color.black);
		defaultRenderer.setBaseItemLabelsVisible(false);

		XYItemLabelGenerator diagonallabelGenerator = new DiagonalLabelGenerator();
		diagonalLineRenderer.setBaseItemLabelGenerator(diagonallabelGenerator);
		diagonalLineRenderer.setBaseItemLabelsVisible(true);

		this.setMinimumSize(new Dimension(400, 400));
		this.setDismissDelay(Integer.MAX_VALUE);
		this.setInitialDelay(0);

		// this.setMouseZoomable(true, false);

		// add items to popup menu
		JPopupMenu popupMenu = getPopupMenu();
		popupMenu.addSeparator();
		popupMenu.add(newPopmenuItem("Show Chromatogram", "TIC"));
		// popupMenu.add(newPopmenuItem("Search KEGG compounds", "ON_LINE"));

		this.registerCustomToolTip();
	}

	public void registerCustomToolTip() {
		
		ttm = new CustomToolTipManager();
		ttm.registerComponent(this);
	}
	
	public JComponent getCustomToolTipComponent(MouseEvent event){

		String index = this.getToolTipText(event);
		if (index == null)
			return null;
		
		// Get info
		int[] indexDomains = dataSet.getDomainsIndexes();
		RawDataFile[] rawDataFiles = dataSet.getPeakList().getRawDataFiles();
		int indX = indexDomains[0];
		int indY = indexDomains[1];
		RawDataFile[] dataFiles = new RawDataFile[] { rawDataFiles[indX], rawDataFiles[indY] };
		PeakListRow row = dataSet.getPeakList().getRow(Integer.parseInt(index));
		
		return new PeakSummaryComponent(row, dataFiles);

	}

	/**
	 * 
	 */
	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);

		String command = event.getActionCommand();

		if (command.equals("SETUP_AXES")) {
			scatterPlotPanel.actionPerformed(event);
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
				scatterPlotPanel.actionPerformed(new ActionEvent(event
						.getSource(), ActionEvent.ACTION_PERFORMED,
						"SHOW_ITEM_NAME"));
			}
		}
	}

	synchronized public void setSeriesColor(ScatterPlotDataSet newSet) {

		XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) plot
				.getRendererForDataset(newSet);
		Color color = Color.BLUE;
		for (int i = 1; i < newSet.getSeriesCount(); i++) {

			if (i > 0) {
				color = ((ScatterPlotDataSet) newSet).getRendererColor(i);
			}
			newRenderer.setSeriesPaint(i, color, false);
			newRenderer.setSeriesFillPaint(i, color, false);
			newRenderer.setSeriesShape(i, dataPointsShape2, false);
		}
	}

	synchronized public void addDataset(ScatterPlotDataSet newSet) {

		drawDiagonalLines(newSet);
		dataSet = newSet;

		plot.setDataset(numOfDataSets + numDiagonals, newSet);

		try {
			XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) defaultRenderer
					.clone();
			((ScatterPlotRenderer) newRenderer).setTransparency(0.4f);
			newRenderer.setSeriesPaint(0, Color.BLUE, false);
			newRenderer.setSeriesFillPaint(0, Color.BLUE, false);
			newRenderer.setSeriesShape(0, dataPointsShape, false);
			plot.setRenderer(numOfDataSets + numDiagonals, newRenderer, false);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		numOfDataSets++;

	}

	public void drawDiagonalLines(ScatterPlotDataSet newSet) {
		double[] maxMinValue = newSet.getMaxMinValue();
		int fold = scatterPlotPanel.selectedFold();
		
		double foldValue = fold;
		for (int i = 0; i < 3; i++) {
			if (i == 0)
				foldValue = 1.0 / (double) fold;
			if (i == 1)
				foldValue = i;
			
			//logger.finest("Value of fold = " + foldValue);
			DiagonalPlotDataset newDiagonalSet = new DiagonalPlotDataset(
					maxMinValue, foldValue);
			addDiagonalSet(newDiagonalSet);
			foldValue = fold;
		}
	}

	private void addDiagonalSet(DiagonalPlotDataset newSet) {

		plot.setDataset(numOfDataSets + numDiagonals, newSet);

		try {
			XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) diagonalLineRenderer
					.clone();
			((ScatterPlotRenderer) newRenderer).setTransparency(0.7f);
			newRenderer.setBaseShapesFilled(true);
			newRenderer.setDrawOutlines(true);
			newRenderer.setUseFillPaint(true);
			Color rendererColor = plotDiagonalColors[numDiagonals
					% plotDiagonalColors.length];
			newRenderer.setSeriesShape(0, diagonalPointsShape, false);
			newRenderer.setSeriesPaint(0, rendererColor, false);
			newRenderer.setSeriesFillPaint(0, rendererColor, false);
			newRenderer.setBaseShapesVisible(true);
			plot.setRenderer(numOfDataSets + numDiagonals, newRenderer, true);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		numDiagonals++;

	}

	public JMenuItem newPopmenuItem(String text, String actionCommand) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(scatterPlotPanel);
		item.setActionCommand(actionCommand);
		return item;
	}

	public void startDatasetCounter() {
		numOfDataSets = 0;
		numDiagonals = 0;
	}

	public ScatterPlotPanel getMaster() {
		return scatterPlotPanel;
	}

	public XYPlot getXYPlot() {
		return plot;
	}

	public ScatterPlotToolTipGenerator getToolTipGenerator() {
		return toolTipGenerator;
	}

	public void setAxisNames(String axisXName, String axisYName) {
		plot.getRangeAxis().setLabel(axisYName);
		plot.getDomainAxis().setLabel(axisXName);
	}

}