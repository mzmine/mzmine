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

package net.sf.mzmine.modules.visualization.ida;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class IDAPlotRenderer extends XYLineAndShapeRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Shape dataPointsShape = new Ellipse2D.Float(-2.5f,
	    -2.5f, 5, 5);

    public static final Color pointColor = Color.black;

    public IDAPlotRenderer() {

	super(false, true);

	IDAPlotToolTipGenerator toolTipGenerator = new IDAPlotToolTipGenerator();
	setBaseToolTipGenerator(toolTipGenerator);

	setBaseItemLabelsVisible(false);

	setSeriesVisibleInLegend(0, false);
	setSeriesItemLabelsVisible(0, false);
	setSeriesPaint(0, pointColor);
	setSeriesShape(0, dataPointsShape);

	setDrawSeriesLineAsPath(true);
    }

    @Override
    public Paint getItemPaint(int row, int col) {
	IDADataSet IDADataSet = (IDADataSet) getPlot().getDataset();
	return IDADataSet.getColor(row, col);
    }
}
