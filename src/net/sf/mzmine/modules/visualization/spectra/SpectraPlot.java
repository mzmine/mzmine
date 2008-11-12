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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JInternalFrame;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MassDetectorSetupDialog;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;
import net.sf.mzmine.util.dialogs.ThicknessSetupDialog;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.ui.RectangleInsets;

/**
 * 
 */
public class SpectraPlot extends ChartPanel {

	private JFreeChart chart;
	private XYPlot plot;

	private PlotMode plotMode = PlotMode.UNDEFINED;

	// Tool Bar that is related with this plot in the same spectrum visualizer
	// window
	private SpectraToolBar toolBar;

	// plot color
	private static final Color plotColor = new Color(0, 0, 192);

	// plot colors for plotted files (predicted), circulated by
	// numOfPeakDataSets
	private static final Color[] plotColors = { new Color(148, 0, 211),
			Color.pink, Color.cyan, new Color(47, 79, 79), Color.orange,
			new Color(173, 255, 47) };

	// picked peaks color
	private static final Color pickedPeaksColor = Color.red;

	// peak labels color
	private static final Color labelsColor = Color.darkGray;

	// grid color
	private static final Color gridColor = Color.lightGray;

	// data points shape
	private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2,
			5, 5);

	// title font
	private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
	private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN,
			11);
	private TextTitle chartTitle, chartSubTitle;

	private static final double zoomCoefficient = 1.2f;

	// datasets counter
	private int numOfPeakDataSets = 1;

	// legend
	private LegendTitle legend;
	private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);

	private double thickness = 0.0005f;

	XYBarRenderer centroidRenderer;
	XYLineAndShapeRenderer continuousRenderer;
	XYToolTipGenerator peakToolTipGenerator;
	HashMap<Integer, XYBarRenderer> peakRendererMap;

	public SpectraPlot(ActionListener masterPlot, SpectraVisualizerType type) {

		super(null, true);

		setBackground(Color.white);
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		peakRendererMap = new HashMap<Integer, XYBarRenderer>();
		thickness = SpectraVisualizerParameters.barThickness;

		// initialize the chart by default time series chart from factory
		chart = ChartFactory.createXYLineChart("", // title
				"m/z", // x-axis label
				"Intensity", // y-axis label
				null, // data set
				PlotOrientation.VERTICAL, // orientation
				true, // isotopeFlag, // create legend?
				true, // generate tooltips?
				false // generate URLs?
				);
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

		// legend constructed by ChartFactory
		legend = chart.getLegend();
		legend.setItemFont(legendFont);
		legend.setFrame(BlockBorder.NONE);

		chart.removeLegend();

		// disable maximum size (we don't want scaling)
		setMaximumDrawWidth(Integer.MAX_VALUE);
		setMaximumDrawHeight(Integer.MAX_VALUE);
		setMinimumDrawHeight(0);

		// set the plot properties
		plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

		// set rendering order
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		// set grid properties
		plot.setDomainGridlinePaint(gridColor);
		plot.setRangeGridlinePaint(gridColor);

		// set crosshair (selection) properties
		plot.setDomainCrosshairVisible(false);
		plot.setRangeCrosshairVisible(false);

		NumberFormat mzFormat = MZmineCore.getMZFormat();
		NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

		// set the X axis (retention time) properties
		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		xAxis.setNumberFormatOverride(mzFormat);
		xAxis.setUpperMargin(0.001);
		xAxis.setLowerMargin(0.001);
		xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));

		// set the Y axis (intensity) properties
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setNumberFormatOverride(intensityFormat);

		// set default renderer properties
		continuousRenderer = new XYLineAndShapeRenderer();
		continuousRenderer.setBaseShapesFilled(true);
		continuousRenderer.setDrawOutlines(false);
		continuousRenderer.setUseFillPaint(true);
		continuousRenderer.setSeriesShape(0, dataPointsShape);
		continuousRenderer.setSeriesPaint(0, plotColor);
		continuousRenderer.setBaseFillPaint(plotColor);
		continuousRenderer.setBaseShapesVisible(false);

		centroidRenderer = new XYBarRenderer();
		centroidRenderer.setShadowVisible(false);
		centroidRenderer.setSeriesShape(0, dataPointsShape);
		centroidRenderer.setSeriesPaint(0, plotColor);

		// set label generator
		SpectraItemLabelGenerator labelGenerator = new SpectraItemLabelGenerator(
				this);
		continuousRenderer.setBaseItemLabelGenerator(labelGenerator);
		continuousRenderer.setBaseItemLabelsVisible(true);
		continuousRenderer.setBaseItemLabelPaint(labelsColor);
		centroidRenderer.setBaseItemLabelGenerator(labelGenerator);
		centroidRenderer.setBaseItemLabelsVisible(true);
		centroidRenderer.setBaseItemLabelPaint(labelsColor);

		// set toolTipGenerator
		SpectraToolTipGenerator spectraToolTipGenerator = new SpectraToolTipGenerator();
		continuousRenderer.setBaseToolTipGenerator(spectraToolTipGenerator);
		centroidRenderer.setBaseToolTipGenerator(spectraToolTipGenerator);

		peakToolTipGenerator = new PeakToolTipGenerator();

		XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter());

		// set focusable state to receive key events
		setFocusable(true);

		// register key handlers
		GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("LEFT"),
				masterPlot, "PREVIOUS_SCAN");
		GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("RIGHT"),
				masterPlot, "NEXT_SCAN");
		GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('+'), this,
				"ZOOM_IN");
		GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('-'), this,
				"ZOOM_OUT");

		// add items to popup menu
		JPopupMenu popupMenu = getPopupMenu();

		popupMenu.addSeparator();

		GUIUtils.addMenuItem(popupMenu, "Toggle centroid/continuous mode",
				this, "TOGGLE_PLOT_MODE");
		GUIUtils.addMenuItem(popupMenu,
				"Toggle displaying of data points in continuous mode", this,
				"SHOW_DATA_POINTS");
		GUIUtils.addMenuItem(popupMenu, "Toggle displaying of peak values",
				this, "SHOW_ANNOTATIONS");
		GUIUtils.addMenuItem(popupMenu, "Toggle displaying of picked peaks",
				this, "SHOW_PICKED_PEAKS");

		popupMenu.addSeparator();

		GUIUtils.addMenuItem(popupMenu, "Set axes range", this, "SETUP_AXES");

		if (!(masterPlot instanceof MassDetectorSetupDialog))
			GUIUtils.addMenuItem(popupMenu, "Set same range to all windows",
					this, "SET_SAME_RANGE");

		popupMenu.addSeparator();

		GUIUtils.addMenuItem(popupMenu, "Add isotope pattern", this,
				"ADD_ISOTOPE_PATTERN");
		GUIUtils.addMenuItem(popupMenu, "Remove all isotope patterns", this,
				"REMOVE_ALL_ISOTOPE_PATTERN");

	}

	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);

		String command = event.getActionCommand();

		if (command.equals("TOGGLE_PLOT_MODE")) {
			if (this.getPlotMode() == PlotMode.CONTINUOUS) {
				this.setPlotMode(PlotMode.CENTROID);
				toolBar.setCentroidButton(false);
			} else {
				this.setPlotMode(PlotMode.CONTINUOUS);
				toolBar.setCentroidButton(true);
			}
		}

		if (command.equals("SHOW_DATA_POINTS")) {
			this.switchDataPointsVisible();
		}

		if (command.equals("SHOW_ANNOTATIONS")) {
			this.switchItemLabelsVisible();
		}

		if (command.equals("SHOW_PICKED_PEAKS")) {
			this.switchPickedPeaksVisible();
		}

		if (command.equals("SHOW_ISOTOPE_PEAKS")) {
			this.switchIsotopePeaksVisible();
		}

		if (command.equals("SETUP_AXES")) {
			AxesSetupDialog dialog = new AxesSetupDialog(this.getXYPlot());
			dialog.setVisible(true);
		}

		if (command.equals("THICKNESS")) {
			ThicknessSetupDialog dialog = new ThicknessSetupDialog(this);
			dialog.setVisible(true);
		}

		if (command.equals("ADD_ISOTOPE_PATTERN")) {
			IsotopePatternCalculator ipc = IsotopePatternCalculator
					.getInstance();
			ipc.showIsotopePatternCalculatorWindow(this);
		}

		if (command.equals("REMOVE_ALL_ISOTOPE_PATTERN")) {
			removeIsotopePatterns();
		}

		if ((command.equals("ZOOM_IN"))
				|| (command.equals("ZOOM_IN_BOTH_COMMAND"))) {
			this.getXYPlot().getDomainAxis().resizeRange(1 / zoomCoefficient);
		}

		if ((command.equals("ZOOM_OUT"))
				|| (command.equals("ZOOM_OUT_BOTH_COMMAND"))) {
			this.getXYPlot().getDomainAxis().resizeRange(zoomCoefficient);
		}

		if (command.equals("SET_SAME_RANGE")) {

			// Get current axes range
			NumberAxis xAxis = (NumberAxis) this.getXYPlot().getDomainAxis();
			NumberAxis yAxis = (NumberAxis) this.getXYPlot().getRangeAxis();
			double xMin = (double) xAxis.getRange().getLowerBound();
			double xMax = (double) xAxis.getRange().getUpperBound();
			double xTick = (double) xAxis.getTickUnit().getSize();
			double yMin = (double) yAxis.getRange().getLowerBound();
			double yMax = (double) yAxis.getRange().getUpperBound();
			double yTick = (double) yAxis.getTickUnit().getSize();

			// Get all frames of my class
			JInternalFrame spectraFrames[] = MZmineCore.getDesktop()
					.getInternalFrames();

			// Set the range of these frames
			for (JInternalFrame frame : spectraFrames) {
				if (!(frame instanceof SpectraVisualizerWindow))
					continue;
				SpectraVisualizerWindow spectraFrame = (SpectraVisualizerWindow) frame;
				spectraFrame.setAxesRange(xMin, xMax, xTick, yMin, yMax, yTick);
			}

		}
	}

	/**
	 * @param plotMode
	 *            The plotMode to set.
	 */
	public void setPlotMode(PlotMode plotMode) {
		this.plotMode = plotMode;
		if (plotMode == PlotMode.CENTROID)
			plot.setRenderer(0, centroidRenderer);
		else
			plot.setRenderer(0, continuousRenderer);

	}

	public PlotMode getPlotMode() {
		return plotMode;
	}

	public XYPlot getXYPlot() {
		return plot;
	}

	public void switchItemLabelsVisible() {

		boolean itemLabelsVisible = continuousRenderer
				.getBaseItemLabelsVisible();
		centroidRenderer.setBaseItemLabelsVisible(!itemLabelsVisible);
		continuousRenderer.setBaseItemLabelsVisible(!itemLabelsVisible);
	}

	public void switchDataPointsVisible() {

		boolean dataPointsVisible = continuousRenderer.getBaseShapesVisible();
		continuousRenderer.setBaseShapesVisible(!dataPointsVisible);

	}

	public boolean getPickedPeaksVisible() {

		if (peakRendererMap.size() == 0)
			return true;
		Boolean pickedPeaksVisible = false;

		if (peakRendererMap.get(1).getBaseSeriesVisible())
			return true;

		return pickedPeaksVisible;
	}

	public boolean getIsotopePeaksVisible() {

		if (peakRendererMap.size() == 0)
			return true;

		Boolean isotopePeaksVisible = false;
		Iterator<Integer> itr = peakRendererMap.keySet().iterator();
		int key;
		while (itr.hasNext()) {
			key = itr.next();
			if (key == 1) {
				if (!((PeakListDataSet) plot.getDataset(1)).isPredicted())
					continue;
			}
			if (peakRendererMap.get(key).getBaseSeriesVisible()) {
				return true;
			}
		}
		return isotopePeaksVisible;
	}

	public void switchPickedPeaksVisible() {
		boolean pickedPeaksVisible = getPickedPeaksVisible();
		peakRendererMap.get(1).setBaseSeriesVisible(!pickedPeaksVisible);
	}

	public void switchIsotopePeaksVisible() {

		boolean isotopePeaksVisible = getIsotopePeaksVisible();
		Iterator<Integer> itr = peakRendererMap.keySet().iterator();
		int key;
		while (itr.hasNext()) {
			key = itr.next();
			if (key == 1){
				if (!((PeakListDataSet) plot.getDataset(1)).isPredicted())
					continue;
			}
			peakRendererMap.get(key).setBaseSeriesVisible(!isotopePeaksVisible);
		}

	}

	public void setTitle(String title, String subTitle) {
		chartTitle.setText(title);
		chartSubTitle.setText(subTitle);
	}

	public void setRelatedToolBar(SpectraToolBar toolBar) {
		this.toolBar = toolBar;
	}

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent event) {

		// let the parent handle the event (selection etc.)
		super.mouseClicked(event);

		// request focus to receive key events
		requestFocus();
	}

	public void setSpectrumDataSet(SpectraDataSet scanData) {

		XYItemRenderer renderer;

		if (scanData == null)
			return;

		if (plotMode == PlotMode.CENTROID) {
			renderer = centroidRenderer;
		} else {
			renderer = continuousRenderer;
		}

		if (scanData.isPredicted()) {
			try {
				renderer = (XYItemRenderer) centroidRenderer.clone();
			} catch (Exception e) {
				e.printStackTrace();
			}
			renderer.setSeriesPaint(0, plotColors[0]);
			setPlotLegend(true);
		}

		plot.setDataset(0, scanData);
		plot.setRenderer(0, renderer);


	}

	public void addPeaksDataSet(PeakListDataSet peakDataSet) {

		if (peakDataSet == null)
			return;

		Color rendererColor;
		peakDataSet.setThickness(thickness);
		XYBarRenderer newRenderer;

		if (peakDataSet.isPredicted()) {

			rendererColor = plotColors[numOfPeakDataSets % plotColors.length];

			newRenderer = new PeakPlotRenderer();
			((PeakPlotRenderer) newRenderer).setTransparencyLevel(0.8f);
			newRenderer.setSeriesPaint(0, rendererColor);
			newRenderer.setBaseToolTipGenerator(peakToolTipGenerator);
			newRenderer.setBaseItemLabelPaint(rendererColor);
			newRenderer.setShadowVisible(false);

			toolBar.setIsotopePeaksButtonEnabled(true);
			setPlotLegend(true);

			if (numOfPeakDataSets > 1) {
				PeakListDataSet firstPeakDataSet = (PeakListDataSet) plot
						.getDataset(1);
				if (firstPeakDataSet.isIsotopeDataSet()
						&& (!firstPeakDataSet.isPredicted())) {
					double score = IsotopePatternScoreCalculator.getScore(
							peakDataSet.getIsotopePattern(), firstPeakDataSet
									.getIsotopePattern());
					peakDataSet.setScore(score);
				}
			}

			if (peakDataSet.isAutoIncrease()) {
				SpectraDataSet spectra = (SpectraDataSet) plot.getDataset(0);
				double increase = spectra.getBiggestIntensity(peakDataSet
						.getIsotopePattern().getDataPoints());
				peakDataSet.setIncreaseIntensity(increase);
			}

			plot.setRenderer(numOfPeakDataSets, newRenderer);
			plot.setDataset(numOfPeakDataSets, peakDataSet);
			peakRendererMap.put(numOfPeakDataSets, newRenderer);
			numOfPeakDataSets++;

		} else {

			if (numOfPeakDataSets == 1) {

				rendererColor = pickedPeaksColor;
				newRenderer = new PeakPlotRenderer();
				newRenderer.setSeriesPaint(0, rendererColor);
				newRenderer.setBaseToolTipGenerator(peakToolTipGenerator);
				newRenderer.setBaseItemLabelPaint(rendererColor);
				newRenderer.setShadowVisible(false);
				plot.setRenderer(1, newRenderer);
				plot.setDataset(1, peakDataSet);
				peakRendererMap.put(1, newRenderer);
				numOfPeakDataSets++;

			} else {
				plot.setDataset(1, peakDataSet);
			}

		}

		updatePeakDataSetIncrease();

	}

	public void removeIsotopePatterns() {
		int dataSetCount = plot.getDatasetCount();
		PeakListDataSet dataSet;

		for (int i = 1; i < dataSetCount; i++) {
			dataSet = (PeakListDataSet) plot.getDataset(i);
			if (dataSet != null)
				if (dataSet.isPredicted()) {
					plot.setDataset(i, null);
					numOfPeakDataSets--;
				}
		}
		setPlotLegend(false);
		plot.datasetChanged(new DatasetChangeEvent(this, plot.getDataset(0)));
	}

	public void setPeakToolTipGenerator(XYToolTipGenerator peakToolTipGenerator) {
		this.peakToolTipGenerator = peakToolTipGenerator;
	}

	public void setBarThickness(double thickness) {
		this.thickness = thickness;
		SpectraVisualizerParameters.barThickness = thickness;

		int dataSetCount = plot.getDatasetCount();
		PeakListDataSet dataSet;

		for (int i = 1; i < dataSetCount; i++) {
			dataSet = (PeakListDataSet) plot.getDataset(i);
			if (dataSet != null)
				dataSet.setThickness(thickness);
		}

		plot.datasetChanged(new DatasetChangeEvent(this, plot.getDataset(0)));

	}

	public double getBarThickness() {
		return thickness;
	}

	private void updatePeakDataSetIncrease() {

		int dataSetCount = plot.getDatasetCount();
		PeakListDataSet dataSet;
		SpectraDataSet spectra = (SpectraDataSet) plot.getDataset(0);

		assert spectra != null;

		double increase;

		for (int i = 1; i < dataSetCount; i++) {
			dataSet = (PeakListDataSet) plot.getDataset(i);
			if (dataSet != null) {
				if (dataSet.isPredicted() && dataSet.isAutoIncrease()) {
					increase = spectra.getBiggestIntensity(dataSet
							.getIsotopePattern().getDataPoints());
					dataSet.setIncreaseIntensity(increase);
				}
			}
		}

	}

	public void setPlotLegend(boolean enable) {
		if (enable) {
			if (chart.getLegend() == null)
				chart.addLegend(legend);
		} else {
			chart.removeLegend();
		}

	}
}