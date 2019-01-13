package net.sf.mzmine.modules.visualization.productionfilter;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
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
class ProductIonFilterDataPointRenderer extends XYLineAndShapeRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AlphaComposite alphaComp, alphaCompOriginal;

	public ProductIonFilterDataPointRenderer(boolean lines, boolean shapes) {
		super(lines, shapes);
		setDrawSeriesLineAsPath(true);
	}

	public void setTransparency(float transparency) {
		if ((transparency > 1.0) || (transparency < 0))
			transparency = 1.0f;
		int type = AlphaComposite.SRC_OVER;
		alphaComp = (AlphaComposite.getInstance(type, transparency));
		alphaCompOriginal = (AlphaComposite.getInstance(type, 1.0f));
	}

	public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info,
			XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item,
			CrosshairState crosshairState, int pass) {

		if (series > 0) {
			g2.setComposite(alphaComp);
		} else if (series == 0) {
			g2.setComposite(alphaCompOriginal);
		}

		super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState,
				pass);

	}

}
