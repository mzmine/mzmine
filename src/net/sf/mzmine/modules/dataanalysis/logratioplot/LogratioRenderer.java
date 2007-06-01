package net.sf.mzmine.modules.dataanalysis.logratioplot;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.plot.ColorPalette;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class LogratioRenderer extends XYLineAndShapeRenderer {

	private static final Shape dataPointsShape = new Ellipse2D.Float(-3, -3, 7, 7);
	
	private LogratioDataset dataset;
	private PaintScale paintScale;
	
	
	public LogratioRenderer(LogratioDataset dataset, PaintScale paintScale) {
		super(false,true);
		this.dataset = dataset;
		this.paintScale = paintScale;
		this.setShape(dataPointsShape);
	}

	@Override
	public Paint getItemPaint(int series, int item) {
	
		double cv = dataset.getZValue(series, item);
		if (cv==Double.NaN) return new Color(255,0,0); 
		
		return paintScale.getPaint(cv);
		
	
	
	}
	
	
}
