package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.plot.ColorPalette;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class CVRenderer extends XYLineAndShapeRenderer {

	private static final Shape dataPointsShape = new Ellipse2D.Float(-3, -3, 7, 7);
	
	private CVDataset dataset;
	private PaintScale paintScale;
	
	
	public CVRenderer(CVDataset dataset, PaintScale paintScale) {
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
