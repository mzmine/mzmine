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

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * 
 */
class PeakDataRenderer extends XYLineAndShapeRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Color peakColor = Color.green;

    // data points shape
    private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2,
	    5, 5);

    PeakDataRenderer() {
	setBaseShapesFilled(true);
	setDrawOutlines(false);
	setUseFillPaint(false);
	setBaseShapesVisible(false);
	setBaseShape(dataPointsShape);

	PeakToolTipGenerator toolTipGenerator = new PeakToolTipGenerator();
	setBaseToolTipGenerator(toolTipGenerator);
        setDrawSeriesLineAsPath(true);

    }

    public Paint getItemPaint(int row, int column) {
	return peakColor;
    }

    public Shape getItemShape(int row, int column) {
	return dataPointsShape;
    }

}
