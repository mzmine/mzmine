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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class TICPlotRenderer extends XYLineAndShapeRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private double transparency = 1.0f;

    public TICPlotRenderer() {
	super(true, false);
        setDrawSeriesLineAsPath(true);
    }

    private AlphaComposite makeComposite(double alpha) {
	int type = AlphaComposite.SRC_OVER;
	return (AlphaComposite.getInstance(type, (float) alpha));
    }

    public void drawItem(Graphics2D g2, XYItemRendererState state,
	    Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
	    ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
	    int series, int item, CrosshairState crosshairState, int pass) {

	g2.setComposite(makeComposite(transparency));

	super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis,
		dataset, series, item, crosshairState, pass);

    }

    protected void drawPrimaryLine(XYItemRendererState state, Graphics2D g2,
	    XYPlot plot, XYDataset dataset, int pass, int series, int item,
	    ValueAxis domainAxis, ValueAxis rangeAxis, Rectangle2D dataArea) {

	g2.setComposite(makeComposite(transparency));

	super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item,
		domainAxis, rangeAxis, dataArea);

    }

    protected void drawFirstPassShape(Graphics2D g2, int pass, int series,
	    int item, Shape shape) {
	g2.setComposite(makeComposite(transparency));
	g2.setStroke(getItemStroke(series, item));
	g2.setPaint(getItemPaint(series, item));
	g2.draw(shape);
    }

    protected void drawPrimaryLineAsPath(XYItemRendererState state,
	    Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
	    int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
	    Rectangle2D dataArea) {

	g2.setComposite(makeComposite(transparency));

	super.drawPrimaryLineAsPath(state, g2, plot, dataset, pass, series,
		item, domainAxis, rangeAxis, dataArea);

    }

    protected void drawSecondaryPass(Graphics2D g2, XYPlot plot,
	    XYDataset dataset, int pass, int series, int item,
	    ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
	    CrosshairState crosshairState, EntityCollection entities) {

	g2.setComposite(makeComposite(transparency));

	super.drawSecondaryPass(g2, plot, dataset, pass, series, item,
		domainAxis, dataArea, rangeAxis, crosshairState, entities);

    }

}
