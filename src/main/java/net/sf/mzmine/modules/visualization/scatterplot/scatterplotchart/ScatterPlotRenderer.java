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

package net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class ScatterPlotRenderer extends XYLineAndShapeRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Shape dataPointsShape = new Ellipse2D.Float(-2.5f,
	    -2.5f, 5, 5);

    public static final Color pointColor = Color.blue;
    public static final Color searchColor = Color.orange;

    public static final AlphaComposite pointAlpha = AlphaComposite.getInstance(
	    AlphaComposite.SRC_OVER, 0.6f);

    public static final AlphaComposite selectionAlpha = AlphaComposite
	    .getInstance(AlphaComposite.SRC_OVER, 0.9f);

    public ScatterPlotRenderer() {

	super(false, true);

	ScatterPlotToolTipGenerator toolTipGenerator = new ScatterPlotToolTipGenerator();
	setDefaultToolTipGenerator(toolTipGenerator);

	XYItemLabelGenerator ItemlabelGenerator = new ScatterPlotItemLabelGenerator();
	setDefaultItemLabelGenerator(ItemlabelGenerator);
	setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 11));
	setDefaultItemLabelPaint(Color.black);
	setDefaultItemLabelsVisible(false);

	setSeriesItemLabelsVisible(0, false);
	setSeriesPaint(0, pointColor);
	setSeriesShape(0, dataPointsShape);

	setSeriesItemLabelsVisible(1, false);
	setSeriesPaint(1, searchColor);
	setSeriesShape(1, dataPointsShape);
	
	setDrawSeriesLineAsPath(true);

    }

    public void drawItem(java.awt.Graphics2D g2, XYItemRendererState state,
	    java.awt.geom.Rectangle2D dataArea, PlotRenderingInfo info,
	    XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
	    XYDataset dataset, int series, int item,
	    CrosshairState crosshairState, int pass) {

	if (series == 0)
	    g2.setComposite(pointAlpha);
	else
	    g2.setComposite(selectionAlpha);

	super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis,
		dataset, series, item, crosshairState, pass);
    }

    /**
     * Draws an item label.
     * 
     * @param g2
     *            the graphics device.
     * @param orientation
     *            the orientation.
     * @param dataset
     *            the dataset.
     * @param series
     *            the series index (zero-based).
     * @param item
     *            the item index (zero-based).
     * @param x
     *            the x coordinate (in Java2D space).
     * @param y
     *            the y coordinate (in Java2D space).
     * @param negative
     *            indicates a negative value (which affects the item label
     *            position).
     */
    protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation,
	    XYDataset dataset, int series, int item, double x, double y,
	    boolean negative) {

	XYItemLabelGenerator generator = getItemLabelGenerator(series, item);
	Font labelFont = getItemLabelFont(series, item);
	g2.setFont(labelFont);
	String label = generator.generateLabel(dataset, series, item);

	if ((label == null) || (label.length() == 0))
	    return;

	// get the label position..
	ItemLabelPosition position = null;
	if (!negative) {
	    position = getPositiveItemLabelPosition(series, item);
	} else {
	    position = getNegativeItemLabelPosition(series, item);
	}

	// work out the label anchor point...
	Point2D anchorPoint = calculateLabelAnchorPoint(
		position.getItemLabelAnchor(), x, y, orientation);

	FontMetrics metrics = g2.getFontMetrics(labelFont);
	int width = SwingUtilities.computeStringWidth(metrics, label) + 2;
	int height = metrics.getHeight();

	int X = (int) (anchorPoint.getX() - (width / 2));
	int Y = (int) (anchorPoint.getY() - (height));

	g2.setPaint(searchColor);
	g2.fillRect(X, Y, width, height);

	super.drawItemLabel(g2, orientation, dataset, series, item, x, y,
		negative);

    }

}
