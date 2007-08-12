package net.sf.mzmine.modules.dataanalysis.projectionplots;

import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

public class ProjectionPlotItemLabelGenerator extends
		StandardXYItemLabelGenerator {
	
	public String generateLabel(ProjectionPlotDataset dataset, int series, int item) {
		ProjectionPlotDataset projectionPlotDataSet = (ProjectionPlotDataset)dataset;
		return dataset.getRawDataFile(item).toString();
	}

	public String generateLabel(XYDataset dataset, int series, int item) {
		if (dataset instanceof ProjectionPlotDataset)
			return generateLabel((ProjectionPlotDataset)dataset, series, item);
		else
			return null;
	}

	public String generateLabel(XYZDataset dataset, int series, int item) {
		if (dataset instanceof ProjectionPlotDataset)
			return generateLabel((ProjectionPlotDataset)dataset, series, item);
		else
			return null;
	}
	
}
