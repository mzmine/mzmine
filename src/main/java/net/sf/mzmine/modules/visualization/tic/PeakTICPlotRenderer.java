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
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

public class PeakTICPlotRenderer extends XYAreaRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final float OPACITY = 0.6f;

    private static Composite makeComposite(final float alpha) {

	return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
    }

    @Override
    public void drawItem(final Graphics2D g2, final XYItemRendererState state,
	    final Rectangle2D dataArea, final PlotRenderingInfo info,
	    final XYPlot plot, final ValueAxis domainAxis,
	    final ValueAxis rangeAxis, final XYDataset dataSet,
	    final int series, final int item,
	    final CrosshairState crosshairState, final int pass) {

	g2.setComposite(makeComposite(OPACITY));
	super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis,
		dataSet, series, item, crosshairState, pass);
    }
}
