package net.sf.mzmine.modules.visualization.tic;

import java.awt.AlphaComposite;
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

	private float transparency = 1.0f;

	public PeakTICPlotRenderer(double transparency) {
		super(XYAreaRenderer.AREA);
		if ((transparency > 1.0) || (transparency < 0))
			return;
		else
			this.transparency = (float) transparency;
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

}
