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

package net.sf.mzmine.modules.visualization.intensityplot;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import org.jfree.chart.plot.DefaultDrawingSupplier;

/**
 * Supplier for shapes and color for the intensity plot data series
 *
 */
class IntensityPlotDrawingSupplier extends DefaultDrawingSupplier {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    // use shapes 1.75 times bigger than default
    private final AffineTransform resizeTransform = AffineTransform
	    .getScaleInstance(1.75, 1.75);

    public Shape getNextShape() {
	Shape baseShape = super.getNextShape();
	return resizeTransform.createTransformedShape(baseShape);
    }

    public Paint getNextPaint() {

	// get new color from the default supplier
	Color baseColor = (Color) super.getNextPaint();

	// ban colors that are too bright
	int colorSum = baseColor.getRed() + baseColor.getGreen()
		+ baseColor.getBlue();
	if (colorSum > 520)
	    baseColor = baseColor.darker();

	return baseColor;
    }

}
