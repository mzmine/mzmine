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

package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class ProjectionPlotRenderer extends XYLineAndShapeRenderer {

	private Paint[] paintsForGroups;

	private static final Shape dataPointsShape = new Ellipse2D.Float(-3, -3, 7,
			7);

	private ProjectionPlotDataset dataset;

	public ProjectionPlotRenderer(XYPlot plot, ProjectionPlotDataset dataset) {
		super(false, true);
		this.dataset = dataset;
		this.setBaseShape(dataPointsShape);

		paintsForGroups = new Paint[dataset.getNumberOfGroups()];
		for (int groupNumber = 0; groupNumber < dataset.getNumberOfGroups(); groupNumber++) {
			DrawingSupplier drawSupp = plot.getDrawingSupplier();
			paintsForGroups[groupNumber] = drawSupp.getNextPaint();
		}
	}

	@Override
	public Paint getItemPaint(int series, int item) {

		int groupNumber = dataset.getGroupNumber(item);
		return paintsForGroups[groupNumber];
	}

	public Paint getGroupPaint(int groupNumber) {
		return paintsForGroups[groupNumber];
	}

}
