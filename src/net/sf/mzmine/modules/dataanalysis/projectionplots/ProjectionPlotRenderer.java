package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class ProjectionPlotRenderer extends XYLineAndShapeRenderer {

	
	private Paint[] paintsForGroups;
	
	private static final Shape dataPointsShape = new Ellipse2D.Float(-3, -3, 7, 7);
	
	private ProjectionPlotDataset dataset;
	
	public ProjectionPlotRenderer(XYPlot plot, ProjectionPlotDataset dataset) {
		super(false,true);
		this.dataset = dataset;
		this.setBaseShape(dataPointsShape);
		
		paintsForGroups = new Paint[dataset.getNumberOfGroups()];
		for (int groupNumber=0; groupNumber<dataset.getNumberOfGroups(); groupNumber++) {
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
