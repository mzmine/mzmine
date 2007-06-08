package net.sf.mzmine.modules.dataanalysis.projectionplots;

import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

public class ProjectionPlotToolTipGenerator implements XYZToolTipGenerator {

	public String generateToolTip(ProjectionPlotDataset dataset, int series, int item) {
		return dataset.getOpenedRawDataFile(item).toString();
	}

	public String generateToolTip(XYDataset dataset, int series, int item) {
		if (dataset instanceof ProjectionPlotDataset) 
			return ((ProjectionPlotDataset)dataset).getOpenedRawDataFile(item).toString();
		return null;
	}	

	public String generateToolTip(XYZDataset dataset, int series, int item) {
		if (dataset instanceof ProjectionPlotDataset) 
			return ((ProjectionPlotDataset)dataset).getOpenedRawDataFile(item).toString();
		return null;
	}	
}
