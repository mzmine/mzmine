package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.plot.ColorPalette;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.AbstractXYZDataset;

public class ProjectionPlotRenderer extends XYLineAndShapeRenderer {

	private static final Shape dataPointsShape = new Ellipse2D.Float(-3, -3, 7, 7);
	
	public ProjectionPlotRenderer() {
		super(false,true);
		this.setShape(dataPointsShape);
	}
	
}
