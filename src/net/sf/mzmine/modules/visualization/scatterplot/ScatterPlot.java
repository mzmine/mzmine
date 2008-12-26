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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

public class ScatterPlot extends ChartPanel {
	
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
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
	private boolean selectionLasso = false;
	private Cursor selectionCursor;
	private int mousex = 0, prevx = 0, initialx = 0;
	private int mousey = 0, prevy = 0, initialy = 0;
	private boolean initialFreeHand = true;
	private Polygon selectionShape;

	private XYLineAndShapeRenderer defaultRenderer;
	private XYLineAndShapeRenderer diagonalLineRenderer;
	private ScatterPlotToolTipGenerator toolTipGenerator;

	// datasets counter
	private int numOfDataSets = 0, numDiagonals = 0;

	public ScatterPlot(ScatterPlotPanel masterFrame) {

		super(null, true);

		this.scatterPlotPanel = masterFrame;

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Image image = toolkit.getImage("icons/pencil.png");
		Point hotSpot = new Point(0, 0);
		selectionCursor = toolkit.createCustomCursor(image, hotSpot, "Pencil");

		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

		// initialize the chart by default time series chart from factory
		chart = ChartFactory.createXYLineChart("", // title
				"", // x-axis label
				"", // y-axis label
				null, // data set
				PlotOrientation.VERTICAL, // orientation
				false, // create legend?
				true, // generate tooltips?
				false // generate URLs?
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
		logAxisDomain.setNumberFormatOverride(new DecimalFormat("###.#####"));
		logAxisDomain.setAutoRange(true);

		LogAxis logAxisRange = new LogAxis();
		logAxisRange.setMinorTickCount(1);
		logAxisRange.setNumberFormatOverride(new DecimalFormat("###.#####"));
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
		toolTipGenerator = new ScatterPlotToolTipGenerator(masterFrame.selectedFold());
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
		popupMenu.add(newPopmenuItem("Toggle Crop selection", "CROP_SEL"));
		popupMenu.add(newPopmenuItem("Search info online", "ON_LINE"));

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

		if (command.equals("SELECTION")) {
			JToggleButton button = (JToggleButton) event.getSource();
			if (button.isSelected()) {
				toolTipGenerator.setEnable(false);
				plot.setDomainCrosshairVisible(false);
				plot.setRangeCrosshairVisible(false);
				selectionLasso = true;
				setCursor(selectionCursor);
			} else {
				toolTipGenerator.setEnable(true);
				plot.setDomainCrosshairVisible(true);
				plot.setRangeCrosshairVisible(true);
				selectionLasso = false;
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}

			scatterPlotPanel.actionPerformed(event);
			return;
		}
	}

	public void mousePressed(MouseEvent e) {
		if (selectionLasso) {
			freeHandOperation(e);
		} else
			super.mousePressed(e);
	}

	public void mouseDragged(MouseEvent e) {
		if (selectionLasso) {
			freeHandOperation(e);
		} else
			super.mouseDragged(e);
	}

	public void freeHandOperation(MouseEvent e) {
		Graphics2D g2d = (Graphics2D) getGraphics();
		g2d.setXORMode(Color.orange);
		g2d.setPaintMode();
		if (initialFreeHand) {
			selectionShape = new Polygon();
			initialFreeHand = false;
			mousex = e.getX();
			mousey = e.getY();
			g2d.drawLine(mousex, mousey, mousex, mousey);
			prevx = mousex;
			prevy = mousey;
			initialx = mousex;
			initialy = mousey;
			selectionShape.addPoint(initialx, initialy);
		}
		if (mouseHasMoved(e)) {
			mousex = e.getX();
			mousey = e.getY();
			g2d.drawLine(prevx, prevy, mousex, mousey);
			prevx = mousex;
			prevy = mousey;
			selectionShape.addPoint(prevx, prevy);
		}
		g2d.setPaintMode();
	}

	public boolean mouseHasMoved(MouseEvent e) {
		return (mousex != e.getX() || mousey != e.getY());
	}

	public void mouseReleased(MouseEvent e) {
		
		if (selectionLasso) {
			freeHandOperation(new MouseEvent((Component)e.getSource(),0,0L,0,initialx, initialy,1,false));
			initialFreeHand = true;
			int localIndex = numDiagonals;
			ScatterPlotDataSet dataSet = (ScatterPlotDataSet) plot.getDataset(localIndex); 
			String[] listNames = dataSet.getListNames();
	        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
	        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
	        ValueAxis domainAxis = plot.getDomainAxisForDataset(localIndex);
            ValueAxis rangeAxis = plot.getRangeAxisForDataset(localIndex);
            Rectangle2D dataArea = getScreenDataArea();
			int length = listNames.length;
			double x, y;
			Point screenPoint;
			Vector<String> searchNames = new Vector<String>();
			Vector<Integer> searchItems = new Vector<Integer>();

            for (int i=0; i<length; i++){
				// get the data point...
				x = dataSet.getXValue(0, i);
				y = dataSet.getYValue(0, i);
		        if (Double.isNaN(y) || Double.isNaN(x)) {
		            continue;
		        }

		        double transX = domainAxis.valueToJava2D(x, dataArea, xAxisLocation);
		        double transY = rangeAxis.valueToJava2D(y, dataArea, yAxisLocation);

				screenPoint = translateJava2DToScreen(new Point2D.Double(transX,transY));

				if (selectionShape.contains(screenPoint)){
					searchNames.add(listNames[i]);
					searchItems.add(i);
				}
			}
            
            if (searchNames.size()>0){
            	ListSelectionItem selectionItem = new ListSelectionItem();
            	selectionItem.setManuallySearchValues(searchNames.toArray(new String[0]));
            	selectionItem.setMatches(searchItems.toArray(new Integer[0]));
            	selectionItem.setCompareFlag();
            	ScatterPlotSearchPanel bp = ((ScatterPlotWindow)scatterPlotPanel.getMaster()).getBotomPanel();
            	bp.addCustomListSelection(selectionItem);
            	
            }
		} else
			super.mouseReleased(e);

	}

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent event) {
		if (!selectionLasso) {
			super.mouseClicked(event);
			requestFocus();
			if (event.getButton() == MouseEvent.BUTTON1)
				showItemName = true;
		}
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
				scatterPlotPanel.actionPerformed(new ActionEvent(event.getSource(),
						ActionEvent.ACTION_PERFORMED, "SHOW_ITEM_NAME"));
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
		toolTipGenerator.setDataFile(newSet);

		plot.setDataset(numOfDataSets + numDiagonals, newSet);

		try {
			XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) defaultRenderer
					.clone();
			((ScatterPlotRenderer) newRenderer).setTransparency(0.4f);
			newRenderer.setSeriesPaint(0, Color.BLUE,false);
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
		toolTipGenerator.setSelectedFold(fold);
		float foldValue = fold;
		for (int i = 0; i < 3; i++) {
			if (i == 0)
				foldValue = (float) 1 / fold;
			if (i == 1)
				foldValue = i;
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
			plot.setRenderer(numOfDataSets + numDiagonals, newRenderer, false);
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
	
	public ScatterPlotPanel getMaster(){
		return scatterPlotPanel;
	}
	
	public boolean getRawDataVisibleStatus(){
		XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) plot.getRenderer(numDiagonals);
		return newRenderer.isSeriesVisible(0);
	}
	
	public void setRawDataVisible(boolean visible){
		XYLineAndShapeRenderer newRenderer = (XYLineAndShapeRenderer) plot.getRenderer(numDiagonals);
		newRenderer.setSeriesVisible(0, visible);
	}
	
	public XYPlot getXYPlot() {
		return plot;
	}

	public ScatterPlotToolTipGenerator getToolTipGenerator() {
		return toolTipGenerator;
	}

}