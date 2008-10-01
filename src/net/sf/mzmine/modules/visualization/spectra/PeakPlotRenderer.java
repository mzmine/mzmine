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


package net.sf.mzmine.modules.visualization.spectra;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

public class PeakPlotRenderer extends XYBarRenderer {

	private AlphaComposite alphaComp, alphaCompOriginal;
	private boolean transparent = false;

	public PeakPlotRenderer() {
		super();
		int type = AlphaComposite.SRC_OVER;
		alphaCompOriginal = (AlphaComposite.getInstance(type, 1.0f));
	}

	public void setTransparencyLevel(float transparency) {
		if ((transparency > 1.0) || (transparency < 0))
			transparency = 1.0f;
		int type = AlphaComposite.SRC_OVER;
		alphaComp = (AlphaComposite.getInstance(type, transparency));
		transparent = true;
	}
	
	public void setTransparency(boolean set){
		transparent = set;
	}

	public void drawItem(Graphics2D g2, XYItemRendererState state,
			Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
			ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
			int series, int item, CrosshairState crosshairState, int pass) {
		
		if (transparent) {
			g2.setComposite(alphaComp);
		} else {
			g2.setComposite(alphaCompOriginal);
		}

		super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis,
				dataset, series, item, crosshairState, pass);

	}

}
