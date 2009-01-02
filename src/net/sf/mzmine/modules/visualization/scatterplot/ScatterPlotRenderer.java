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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingUtilities;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
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

	private AlphaComposite alphaComp, alphaCompOriginal;

	public ScatterPlotRenderer(boolean lines, boolean shapes) {
		super(lines, shapes);
	}

	public void setTransparency(float transparency) {
		if ((transparency > 1.0) || (transparency < 0))
			transparency = 1.0f;
		int type = AlphaComposite.SRC_OVER;
		alphaComp = (AlphaComposite.getInstance(type, transparency));
		alphaCompOriginal = (AlphaComposite.getInstance(type, 1.0f));
	}

	public void drawItem(Graphics2D g2, XYItemRendererState state,
			Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
			ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
			int series, int item, CrosshairState crosshairState, int pass) {

		if (series > 0) {
			g2.setComposite(alphaCompOriginal);
		} else if (series == 0) {
			g2.setComposite(alphaComp);
		}

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

		if (dataset instanceof ScatterPlotDataSet) {
			XYItemLabelGenerator generator = getItemLabelGenerator(series, item);
			if (generator != null) {
				Font labelFont = getItemLabelFont(series, item);
				g2.setFont(labelFont);
				String label = generator.generateLabel(dataset, series, item);

				if (label == null)
					return;
				else if (label.length() > 0) {

					// get the label position..
					ItemLabelPosition position = null;
					if (!negative) {
						position = getPositiveItemLabelPosition(series, item);
					} else {
						position = getNegativeItemLabelPosition(series, item);
					}

					// work out the label anchor point...
					Point2D anchorPoint = calculateLabelAnchorPoint(position
							.getItemLabelAnchor(), x, y, orientation);

					FontMetrics metrics = g2.getFontMetrics(labelFont);
					int width = SwingUtilities.computeStringWidth(metrics,
							label) + 2;
					int height = metrics.getHeight();

					int X = (int) (anchorPoint.getX() - (width / 2));
					int Y = (int) (anchorPoint.getY() - (height));

					g2.setPaint(Color.black);
					g2.drawRect(X, Y, width, height);

					g2.setPaint(((ScatterPlotDataSet) dataset)
							.getRendererColor(series));
					g2.fillRect(X, Y, width, height);

				}

			}
		}
		
		super.drawItemLabel(g2, orientation, dataset, series, item, x, y,
				negative);

	}

	protected void drawPrimaryLine(XYItemRendererState state, Graphics2D g2,
			XYPlot plot, XYDataset dataset, int pass, int series, int item,
			ValueAxis domainAxis, ValueAxis rangeAxis, Rectangle2D dataArea) {

		if (series > 0) {
			g2.setComposite(alphaCompOriginal);
		} else if (series == 0) {
			g2.setComposite(alphaComp);
		}

		super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item,
				domainAxis, rangeAxis, dataArea);

	}

	protected void drawFirstPassShape(Graphics2D g2, int pass, int series,
			int item, Shape shape) {
		if (series > 0) {
			g2.setComposite(alphaCompOriginal);
		} else if (series == 0) {
			g2.setComposite(alphaComp);
		}
		g2.setStroke(getItemStroke(series, item));
		g2.setPaint(getItemPaint(series, item));
		g2.draw(shape);
	}

	protected void drawPrimaryLineAsPath(XYItemRendererState state,
			Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
			int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
			Rectangle2D dataArea) {

		if (series > 0) {
			g2.setComposite(alphaCompOriginal);
		} else if (series == 0) {
			g2.setComposite(alphaComp);
		}

		super.drawPrimaryLineAsPath(state, g2, plot, dataset, pass, series,
				item, domainAxis, rangeAxis, dataArea);

	}

	protected void drawSecondaryPass(Graphics2D g2, XYPlot plot,
			XYDataset dataset, int pass, int series, int item,
			ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
			CrosshairState crosshairState, EntityCollection entities) {

		if (series > 0) {
			g2.setComposite(alphaCompOriginal);
		} else if (series == 0) {
			g2.setComposite(alphaComp);
		}

		super.drawSecondaryPass(g2, plot, dataset, pass, series, item,
				domainAxis, dataArea, rangeAxis, crosshairState, entities);

	}

}
