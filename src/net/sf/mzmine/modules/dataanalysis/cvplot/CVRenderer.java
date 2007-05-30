package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class CVRenderer extends XYLineAndShapeRenderer {

	private CVDataset dataset;
	
	private HeatMapColorPicker colorPicker;
	
	public CVRenderer(CVDataset dataset) {
		super(false,true);
		this.dataset = dataset;
		colorPicker = new HeatMapColorPicker();
	}

	@Override
	public Paint getItemPaint(int series, int item) {
		
		double cv = dataset.getZValue(series, item);
		if (cv==Double.NaN) return Color.RED;

		return colorPicker.getColor(cv);
		
	
	}
	
	
}
