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
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class DiagonalLineRenderer extends XYLineAndShapeRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // plot colors for diagonal lines
    private static final Color[] plotDiagonalColors = { new Color(165, 42, 42),
	    Color.BLACK, new Color(165, 42, 42) };

    private static final Shape diagonalPointsShape = new Rectangle2D.Float(-3,
	    -3, 6, 6);

    public static final AlphaComposite alpha = AlphaComposite.getInstance(
	    AlphaComposite.SRC_OVER, 0.7f);

    public DiagonalLineRenderer() {

	super(true, true);

	setBaseShapesFilled(true);
	setDrawOutlines(true);
	setUseFillPaint(true);

	for (int i = 0; i < plotDiagonalColors.length; i++) {
	    setSeriesShape(i, diagonalPointsShape);
	    setSeriesPaint(i, plotDiagonalColors[i]);
	    setSeriesFillPaint(i, plotDiagonalColors[i]);
	}

	setBaseShapesVisible(true);

	XYItemLabelGenerator diagonallabelGenerator = new DiagonalLineLabelGenerator();
	setBaseItemLabelGenerator(diagonallabelGenerator);
	setBaseItemLabelsVisible(true);

	setDrawSeriesLineAsPath(true);
    }

    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea,
	    XYPlot plot, XYDataset dataset, PlotRenderingInfo info) {

	// Set transparency
	g2.setComposite(alpha);

	return super.initialise(g2, dataArea, plot, dataset, info);
    }

}
