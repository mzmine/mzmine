/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.neutralloss;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * Renderer which highlights selected points
 */
class NeutralLossDataPointRenderer extends XYLineAndShapeRenderer {

    // small circle
    private static final Shape dataPointsShape = new Ellipse2D.Double(-1, -1, 2, 2);

    private NeutralLossPlot nlPlot;
    
    NeutralLossDataPointRenderer(NeutralLossPlot nlPlot) {
        
        // draw shapes, not lines
        super(false, true);
        
        this.nlPlot = nlPlot;

        setBaseShape(dataPointsShape);

    }

    public void drawItem(Graphics2D g2, XYItemRendererState state,
            Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
            ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
            int series, int item, CrosshairState crosshairState, int pass) {

        NeutralLossDataSet nlDataset = (NeutralLossDataSet) dataset;

        NeutralLossDataPoint point = nlDataset.getDataPoint(item);

        // set the color to red for highlighted points
        if ((point.getPrecursorMZ() < nlPlot.getHighlightedMin())
                || (point.getPrecursorMZ() > nlPlot.getHighlightedMax()))
            setBasePaint(Color.blue, false);
        else
            setBasePaint(Color.red, false);

        super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis,
                dataset, series, item, crosshairState, pass);

    }

}
