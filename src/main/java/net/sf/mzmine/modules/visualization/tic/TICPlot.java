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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;
import net.sf.mzmine.util.SaveImage;
import net.sf.mzmine.util.SaveImage.FileType;

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
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

/**
 * TIC plot.
 * 
 * Added the possibility to switch to TIC plot type from a
 * "non-TICVisualizerWindow" context.
 */
public class TICPlot extends ChartPanel implements MouseWheelListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Logger.
    private static final Logger LOG = Logger.getLogger(TICPlot.class.getName());

    // Zoom factor.
    private static final double ZOOM_FACTOR = 1.2;

    // Plot colors for plotted files.
    private static final Color[] PLOT_COLORS = { new Color(0, 0, 192), // blue
	    new Color(192, 0, 0), // red
	    new Color(0, 192, 0), // green
	    Color.magenta, Color.cyan, Color.orange };

    // Peak colours.
    private static final Color[] PEAK_COLORS = { Color.pink, Color.red,
	    Color.yellow, Color.blue, Color.lightGray, Color.orange,
	    Color.green };

    // peak labels color
    private static final Color LABEL_COLOR = Color.darkGray;

    // grid color
    private static final Color GRID_COLOR = Color.lightGray;

    // Cross-hair (selection) color.
    private static final Color CROSS_HAIR_COLOR = Color.gray;

    // Cross-hair stroke.
    private static final Stroke CROSS_HAIR_STROKE = new BasicStroke(1.0F,
	    BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
		    5.0F, 3.0F }, 0.0F);

    // data points shape
    private static final Shape DATA_POINT_SHAPE = new Ellipse2D.Double(-2.0,
	    -2.0, 5.0, 5.0);

    // Fonts.
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN,
	    11);
    private static final Font LEGEND_FONT = new Font("SansSerif", Font.PLAIN,
	    11);

    // Axis offsets.
    private static final RectangleInsets AXIS_OFFSET = new RectangleInsets(5.0,
	    5.0, 5.0, 5.0);

    // Axis margins.
    private static final double AXIS_MARGINS = 0.001;

    // Title margin.
    private static final double TITLE_TOP_MARGIN = 5.0;

    // Plot type.
    private TICPlotType plotType;
    // The plot.
    private final XYPlot plot;

    // TICVisualizerWindow visualizer.
    private final ActionListener visualizer;

    // Titles.
    private final TextTitle chartTitle;
    private final TextTitle chartSubTitle;

    // Renderer.
    private final TICPlotRenderer defaultRenderer;

    // Counters.
    private int numOfDataSets;
    private int numOfPeaks;

    public JMenuItem RemoveFilePopupMenu;

    /**
     * Indicates whether we have a request to show spectra visualizer for
     * selected data point. Since the selection (cross-hair) is updated with
     * some delay after clicking with mouse, we cannot open the new visualizer
     * immediately. Therefore we place a request and open the visualizer later
     * in chartProgress()
     */
    private boolean showSpectrumRequest;

    // Label visibility: 0 -> none; 1 -> m/z; 2 -> identities
    private int labelsVisible;
    private boolean havePeakLabels;

    public TICPlot(final ActionListener listener) {

	super(null, true);

	// Initialize.
	visualizer = listener;
	labelsVisible = 1;
	havePeakLabels = false;
	numOfDataSets = 0;
	numOfPeaks = 0;
	showSpectrumRequest = false;

	// Set cursor.
	setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

	// Plot type
	if (visualizer instanceof TICVisualizerWindow) {
	    this.plotType = ((TICVisualizerWindow) visualizer).getPlotType();
	} else {

	    this.plotType = TICPlotType.BASEPEAK;
	}
	// Y-axis label.
	final String yAxisLabel = (this.plotType == TICPlotType.BASEPEAK) ? "Base peak intensity"
		: "Total ion intensity";

	// Initialize the chart by default time series chart from factory.
	final JFreeChart chart = ChartFactory.createXYLineChart("", // title
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

	// Title.
	chartTitle = chart.getTitle();
	chartTitle.setFont(TITLE_FONT);
	chartTitle.setMargin(TITLE_TOP_MARGIN, 0.0, 0.0, 0.0);

	// Subtitle.
	chartSubTitle = new TextTitle();
	chartSubTitle.setFont(SUBTITLE_FONT);
	chartSubTitle.setMargin(TITLE_TOP_MARGIN, 0.0, 0.0, 0.0);
	chart.addSubtitle(chartSubTitle);

	// Disable maximum size (we don't want scaling).
	setMaximumDrawWidth(Integer.MAX_VALUE);
	setMaximumDrawHeight(Integer.MAX_VALUE);

	// Legend constructed by ChartFactory.
	final LegendTitle legend = chart.getLegend();
	legend.setItemFont(LEGEND_FONT);
	legend.setFrame(BlockBorder.NONE);

	// Set the plot properties.
	plot = chart.getXYPlot();
	plot.setBackgroundPaint(Color.white);
	plot.setAxisOffset(AXIS_OFFSET);
	plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

	// Set grid properties.
	plot.setDomainGridlinePaint(GRID_COLOR);
	plot.setRangeGridlinePaint(GRID_COLOR);

	// Set cross-hair (selection) properties.
	if (listener instanceof TICVisualizerWindow) {

	    plot.setDomainCrosshairVisible(true);
	    plot.setRangeCrosshairVisible(true);
	    plot.setDomainCrosshairPaint(CROSS_HAIR_COLOR);
	    plot.setRangeCrosshairPaint(CROSS_HAIR_COLOR);
	    plot.setDomainCrosshairStroke(CROSS_HAIR_STROKE);
	    plot.setRangeCrosshairStroke(CROSS_HAIR_STROKE);
	}

	// Set the x-axis (retention time) properties.
	final NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
	xAxis.setNumberFormatOverride(MZmineCore.getConfiguration()
		.getRTFormat());
	xAxis.setUpperMargin(AXIS_MARGINS);
	xAxis.setLowerMargin(AXIS_MARGINS);

	// Set the y-axis (intensity) properties.
	final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
	yAxis.setNumberFormatOverride(MZmineCore.getConfiguration()
		.getIntensityFormat());

	// Set default renderer properties.
	defaultRenderer = new TICPlotRenderer();
	defaultRenderer.setBaseShapesFilled(true);
	defaultRenderer.setDrawOutlines(false);
	defaultRenderer.setUseFillPaint(true);
	defaultRenderer.setBaseItemLabelPaint(LABEL_COLOR);

	// Set label generator
	final XYItemLabelGenerator labelGenerator = new TICItemLabelGenerator(
		this);
	defaultRenderer.setBaseItemLabelGenerator(labelGenerator);
	defaultRenderer.setBaseItemLabelsVisible(true);

	// Set toolTipGenerator
	final XYToolTipGenerator toolTipGenerator = new TICToolTipGenerator();
	defaultRenderer.setBaseToolTipGenerator(toolTipGenerator);

	// Set focus state to receive key events.
	setFocusable(true);

	// Register key handlers.
	GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("LEFT"),
		listener, "MOVE_CURSOR_LEFT");
	GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("RIGHT"),
		listener, "MOVE_CURSOR_RIGHT");
	GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("SPACE"),
		listener, "SHOW_SPECTRUM");
	GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('+'), this,
		"ZOOM_IN");
	GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('-'), this,
		"ZOOM_OUT");
	GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('*'), this,
		"ZOOM_AUTO");

	// Add items to popup menu.
	final JPopupMenu popupMenu = getPopupMenu();
	popupMenu.addSeparator();

	// Add EMF and EPS options to the save as menu
	JMenuItem saveAsMenu = (JMenuItem) popupMenu.getComponent(3);	
	GUIUtils.addMenuItem(saveAsMenu, "EMF...", this, "SAVE_EMF");
	GUIUtils.addMenuItem(saveAsMenu, "EPS...", this, "SAVE_EPS");

	if (listener instanceof TICVisualizerWindow) {

	    popupMenu.add(new ExportPopUpMenu((TICVisualizerWindow) listener));
	    popupMenu.addSeparator();
	    popupMenu.add(new AddFilePopupMenu((TICVisualizerWindow) listener));
	    RemoveFilePopupMenu = popupMenu.add(new RemoveFilePopupMenu(
		    (TICVisualizerWindow) listener));
	    popupMenu.addSeparator();
	    RemoveFilePopupMenu.setEnabled(false);
	}

	GUIUtils.addMenuItem(popupMenu, "Toggle showing peak values", this,
		"SHOW_ANNOTATIONS");
	GUIUtils.addMenuItem(popupMenu, "Toggle showing data points", this,
		"SHOW_DATA_POINTS");

	if (listener instanceof TICVisualizerWindow) {
	    popupMenu.addSeparator();
	    GUIUtils.addMenuItem(popupMenu, "Show spectrum of selected scan",
		    listener, "SHOW_SPECTRUM");
	}

	popupMenu.addSeparator();

	GUIUtils.addMenuItem(popupMenu, "Set axes range", this, "SETUP_AXES");

	if (listener instanceof TICVisualizerWindow) {

	    GUIUtils.addMenuItem(popupMenu, "Set same range to all windows",
		    this, "SET_SAME_RANGE");
	}

	// Register for mouse-wheel events
	addMouseWheelListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent event) {

	super.actionPerformed(event);

	final String command = event.getActionCommand();

	if ("SHOW_DATA_POINTS".equals(command)) {

	    switchDataPointsVisible();
	}

	if ("SHOW_ANNOTATIONS".equals(command)) {

	    switchItemLabelsVisible();
	}

	if ("SETUP_AXES".equals(command)) {
	    JFrame parent = null;
	    if (visualizer instanceof JFrame)
		parent = (JFrame) visualizer;
	    new AxesSetupDialog(parent, getXYPlot()).setVisible(true);
	}

	if ("ZOOM_IN".equals(command)) {

	    getXYPlot().getDomainAxis().resizeRange(1.0 / ZOOM_FACTOR);
	    getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
	}

	// Set tick size to auto when zooming
	String[] zoomList = new String[] { "ZOOM_IN_BOTH", "ZOOM_IN_DOMAIN",
		"ZOOM_IN_RANGE", "ZOOM_OUT_BOTH", "ZOOM_DOMAIN_BOTH",
		"ZOOM_RANGE_BOTH", "ZOOM_RESET_BOTH", "ZOOM_RESET_DOMAIN",
		"ZOOM_RESET_RANGE" };
	if (Arrays.asList(zoomList).contains(command)) {
	    getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
	    getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);
	}

	if ("ZOOM_OUT".equals(command)) {

	    getXYPlot().getDomainAxis().resizeRange(ZOOM_FACTOR);
	    getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
	    // if (getXYPlot().getDomainAxis().getRange().contains(0.0000001)) {
	    // getXYPlot().getDomainAxis().setAutoRange(true);
	    // getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
	    // }
	}

	if ("ZOOM_AUTO".equals(command)) {
	    getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
	    getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);
	    restoreAutoDomainBounds();
	    restoreAutoRangeBounds();
	}

	if ("SET_SAME_RANGE".equals(command)) {

	    // Get current axes range.
	    final NumberAxis xAxis = (NumberAxis) getXYPlot().getDomainAxis();
	    final NumberAxis yAxis = (NumberAxis) getXYPlot().getRangeAxis();
	    final double xMin = xAxis.getRange().getLowerBound();
	    final double xMax = xAxis.getRange().getUpperBound();
	    final double xTick = xAxis.getTickUnit().getSize();
	    final double yMin = yAxis.getRange().getLowerBound();
	    final double yMax = yAxis.getRange().getUpperBound();
	    final double yTick = yAxis.getTickUnit().getSize();

	    // Set the range of these frames
	    for (final Window frame : JFrame.getWindows()) {
		if (frame instanceof TICVisualizerWindow) {

		    final TICVisualizerWindow ticFrame = (TICVisualizerWindow) frame;
		    ticFrame.setAxesRange(xMin, xMax, xTick, yMin, yMax, yTick);
		}
	    }
	}

	if ("SHOW_SPECTRUM".equals(command)) {

	    visualizer.actionPerformed(event);
	}

	if ("SHOW_LEGEND".equals(command)) {
	    // Toggle legend visibility.
	    final LegendTitle legend = getChart().getLegend();
	    legend.setVisible(!legend.isVisible());
	}

	if ("GRAY_BACKGROUND".equals(command)) {
	    // Toggle background color
	    final Paint color = getChart().getPlot().getBackgroundPaint();
	    Color bgColor, liColor;
	    if (color.equals(Color.lightGray)) {
		bgColor = Color.white;
		liColor = Color.lightGray;
	    }
	    else {
		bgColor = Color.lightGray;
		liColor = Color.white;		
	    }
	    getChart().getPlot().setBackgroundPaint(bgColor);
	    getChart().getXYPlot().setDomainGridlinePaint(liColor);
	    getChart().getXYPlot().setRangeGridlinePaint(liColor);
	}

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

    public void mouseWheelMoved(MouseWheelEvent event) {
	int notches = event.getWheelRotation();
	if (notches < 0) {
	    getXYPlot().getDomainAxis().resizeRange(1.0 / ZOOM_FACTOR);
	} else {
	    getXYPlot().getDomainAxis().resizeRange(ZOOM_FACTOR);
	}
    }

    public void restoreAutoBounds() {
	getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
	getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);
	restoreAutoDomainBounds();
	restoreAutoRangeBounds();
    }

    @Override
    public void mouseClicked(final MouseEvent event) {

	// Let the parent handle the event (selection etc.)
	super.mouseClicked(event);

	// Request focus to receive key events.
	requestFocus();

	// Handle mouse click events
	if (event.getButton() == MouseEvent.BUTTON1) {

	    if (event.getX() < 70) { // User clicked on Y-axis
		if (event.getClickCount() == 2) { // Reset zoom on Y-axis
		    XYDataset data = ((XYPlot) getChart().getPlot())
			    .getDataset();
		    Number maximum = DatasetUtilities
			    .findMaximumRangeValue(data);
		    getXYPlot().getRangeAxis().setRange(0,
			    1.05 * maximum.floatValue());
		} else if (event.getClickCount() == 1) { // Auto range on Y-axis
		    getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);
		    getXYPlot().getRangeAxis().setAutoRange(true);
		}
	    } else if (event.getY() > this.getChartRenderingInfo()
		    .getPlotInfo().getPlotArea().getMaxY() - 41
		    && event.getClickCount() == 2) {
		// Reset zoom on X-axis
		getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
		restoreAutoDomainBounds();
	    } else if (event.getClickCount() == 2) { // If user double-clicked
						     // left button, place a
						     // request to open a
						     // spectrum.
		showSpectrumRequest = true;
	    }
	}

    }

    @Override
    public void chartProgress(final ChartProgressEvent event) {

	super.chartProgress(event);

	if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {

	    if (visualizer instanceof TICVisualizerWindow) {

		((TICVisualizerWindow) visualizer).updateTitle();
	    }

	    if (showSpectrumRequest) {

		showSpectrumRequest = false;
		visualizer.actionPerformed(new ActionEvent(event.getSource(),
			ActionEvent.ACTION_PERFORMED, "SHOW_SPECTRUM"));
	    }
	}
    }

    public void switchItemLabelsVisible() {

	// Switch to next mode. Include peaks mode only if peak labels are
	// present.
	labelsVisible = (labelsVisible + 1) % (havePeakLabels ? 3 : 2);

	final int dataSetCount = plot.getDatasetCount();
	for (int i = 0; i < dataSetCount; i++) {

	    final XYDataset dataSet = plot.getDataset(i);
	    final XYItemRenderer renderer = plot.getRenderer(i);
	    if (dataSet instanceof TICDataSet) {

		renderer.setBaseItemLabelsVisible(labelsVisible == 1);

	    } else if (dataSet instanceof PeakDataSet) {

		renderer.setBaseItemLabelsVisible(labelsVisible == 2);

	    } else {

		renderer.setBaseItemLabelsVisible(false);

	    }
	}
    }

    public void switchDataPointsVisible() {

	Boolean dataPointsVisible = null;
	final int count = plot.getDatasetCount();
	for (int i = 0; i < count; i++) {

	    if (plot.getRenderer(i) instanceof XYLineAndShapeRenderer) {

		final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
			.getRenderer(i);
		if (dataPointsVisible == null) {
		    dataPointsVisible = !renderer.getBaseShapesVisible();
		}
		renderer.setBaseShapesVisible(dataPointsVisible);
	    }
	}
    }

    public XYPlot getXYPlot() {

	return plot;
    }

    public synchronized void addTICDataset(final XYDataset dataSet) {
	// Check if the dataSet to be added is compatible with the type of plot.
	if ((dataSet instanceof TICDataSet)
		&& (((TICDataSet) dataSet).getPlotType() != this.plotType))
	    throw new IllegalArgumentException("Added dataset of class '"
		    + dataSet.getClass()
		    + "' does not have a compatible plotType. Expected '"
		    + this.plotType.toString() + "'");
	try {
	    final TICPlotRenderer renderer = (TICPlotRenderer) defaultRenderer
		    .clone();
	    final Color rendererColor = PLOT_COLORS[numOfDataSets
		    % PLOT_COLORS.length];
	    renderer.setSeriesPaint(0, rendererColor);
	    renderer.setSeriesFillPaint(0, rendererColor);
	    renderer.setSeriesShape(0, DATA_POINT_SHAPE);
	    renderer.setBaseItemLabelsVisible(labelsVisible == 1);
	    addDataSetRenderer(dataSet, renderer);
	    numOfDataSets++;

	    // Enable remove plot menu
	    if (visualizer instanceof TICVisualizerWindow && numOfDataSets > 1) {
		RemoveFilePopupMenu.setEnabled(true);
	    }
	} catch (CloneNotSupportedException e) {
	    LOG.log(Level.WARNING, "Unable to clone renderer", e);
	}
    }

    public synchronized void addPeakDataset(final XYDataset dataSet) {

	final PeakTICPlotRenderer renderer = new PeakTICPlotRenderer();
	renderer.setBaseToolTipGenerator(new TICToolTipGenerator());
	renderer.setSeriesPaint(0, PEAK_COLORS[numOfPeaks % PEAK_COLORS.length]);
	addDataSetRenderer(dataSet, renderer);
	numOfPeaks++;
    }

    public synchronized void addLabelledPeakDataset(final XYDataset dataSet,
	    final String label) {

	// Add standard peak data set.
	addPeakDataset(dataSet);

	// Do we have a label?
	if (label != null && label.length() > 0) {

	    // Add peak label renderer and data set.
	    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(
		    false, false);
	    renderer.setBaseItemLabelsVisible(labelsVisible == 2);
	    renderer.setBaseItemLabelPaint(LABEL_COLOR);
	    addDataSetRenderer(dataSet, renderer);
	    renderer.setDrawSeriesLineAsPath(true);
	    renderer.setBaseItemLabelGenerator(new XYItemLabelGenerator() {

		@Override
		public String generateLabel(final XYDataset xyDataSet,
			final int series, final int item) {

		    return ((PeakDataSet) xyDataSet).isPeak(item) ? label
			    : null;
		}
	    });

	    havePeakLabels = true;
	}
    }

    public void removeAllTICDataSets() {

	final int dataSetCount = plot.getDatasetCount();
	for (int index = 0; index < dataSetCount; index++) {

	    plot.setDataset(index, null);
	}
	numOfPeaks = 0;
	numOfDataSets = 0;
    }

    public void setTitle(final String titleText, final String subTitleText) {

	chartTitle.setText(titleText);
	chartSubTitle.setText(subTitleText);
    }

    public void setPlotType(final TICPlotType plotType) {

	if (this.plotType != plotType) {
	    // Plot type
	    if (visualizer instanceof TICVisualizerWindow) {
		this.plotType = ((TICVisualizerWindow) visualizer)
			.getPlotType();
	    } else {
		this.plotType = plotType;
	    }
	    // Y-axis label.
	    String yAxisLabel = (this.plotType == TICPlotType.BASEPEAK) ? "Base peak intensity"
		    : "Total ion intensity";
	    getXYPlot().getRangeAxis().setLabel(yAxisLabel);
	}
    }

    private void addDataSetRenderer(final XYDataset dataSet,
	    final XYItemRenderer renderer) {

	final int index = numOfDataSets + numOfPeaks;
	plot.setRenderer(index, renderer);
	plot.setDataset(index, dataSet);
    }
}
