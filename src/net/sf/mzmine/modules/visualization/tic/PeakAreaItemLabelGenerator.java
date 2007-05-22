package net.sf.mzmine.modules.visualization.tic;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

public class PeakAreaItemLabelGenerator implements XYItemLabelGenerator {

	private String[] labels;
	public PeakAreaItemLabelGenerator(String[] labels) {
		this.labels = labels;
	}
	public String generateLabel(XYDataset arg0, int arg1, int arg2) {

		if (labels==null) return null;
		
		return "m/z " + labels[arg2];

	}

}
