package net.sf.mzmine.modules.visualization.kendrickMassPlot;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

public class KendrickMassPlotXYZToolTipGenerator implements XYZToolTipGenerator {

	private String xAxisLabel, yAxisLabel, zAxisLabel;
	private NumberFormat numberFormatX = new DecimalFormat("####0.0000");
	private NumberFormat numberFormatY = new DecimalFormat("0.000");

	public KendrickMassPlotXYZToolTipGenerator(String xAxisLabel, String yAxisLabel, String zAxisLabel){
		super();
		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
		this.zAxisLabel = zAxisLabel;

	}
	@Override
	public String generateToolTip(XYZDataset dataset, int series, int item) {
		return String.valueOf(xAxisLabel+": "+
				numberFormatX.format(dataset.getXValue(series, item))+
				" "+yAxisLabel+": "+
				numberFormatY.format(dataset.getYValue(series, item))+
				" "+zAxisLabel+": "+
				numberFormatY.format(dataset.getZValue(series, item)));
	}
	@Override
	public String generateToolTip(XYDataset dataset, int series, int item) {
		return String.valueOf(xAxisLabel+": "+
				numberFormatX.format(dataset.getXValue(series, item))+
				" "+yAxisLabel+": "+
				numberFormatY.format(dataset.getYValue(series, item)));
	}
	
}
