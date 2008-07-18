package net.sf.mzmine.modules.visualization.tic;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class TICPlotRenderer extends XYLineAndShapeRenderer {

	private float transparency = 1.0f;

	public TICPlotRenderer() {
		super(true, false);
	}
	
	private AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}

	public void drawItem(Graphics2D g2, XYItemRendererState state,
			Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
			ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
			int series, int item, CrosshairState crosshairState, int pass) {

		g2.setComposite(makeComposite(transparency));

		super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis,
				dataset, series, item, crosshairState, pass);

	}

	protected void drawPrimaryLine(XYItemRendererState state, Graphics2D g2,
			XYPlot plot, XYDataset dataset, int pass, int series, int item,
			ValueAxis domainAxis, ValueAxis rangeAxis, Rectangle2D dataArea) {

		g2.setComposite(makeComposite(transparency));

		super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item,
				domainAxis, rangeAxis, dataArea);

	}

	protected void drawFirstPassShape(Graphics2D g2, int pass, int series,
			int item, Shape shape) {
		g2.setComposite(makeComposite(transparency));
		g2.setStroke(getItemStroke(series, item));
		g2.setPaint(getItemPaint(series, item));
		g2.draw(shape);
	}

	protected void drawPrimaryLineAsPath(XYItemRendererState state,
			Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
			int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
			Rectangle2D dataArea) {

		g2.setComposite(makeComposite(transparency));

		super.drawPrimaryLineAsPath(state, g2, plot, dataset, pass, series,
				item, domainAxis, rangeAxis, dataArea);

	}

	protected void drawSecondaryPass(Graphics2D g2, XYPlot plot,
			XYDataset dataset, int pass, int series, int item,
			ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
			CrosshairState crosshairState, EntityCollection entities) {

		g2.setComposite(makeComposite(transparency));

		super.drawSecondaryPass(g2, plot, dataset, pass, series, item,
				domainAxis, dataArea, rangeAxis, crosshairState, entities);

	}
	
	

}
