package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

public class RTMZToolTipGenerator implements XYZToolTipGenerator {

	public String generateToolTip(RTMZDataset dataset, int series, int item) {
		return dataset.getPeakListRow(item).toString();
	}

	public String generateToolTip(XYDataset dataset, int series, int item) {
		if (dataset instanceof RTMZDataset) 
			return ((RTMZDataset)dataset).getPeakListRow(item).toString();
		return null;
	}	

	public String generateToolTip(XYZDataset dataset, int series, int item) {
		if (dataset instanceof RTMZDataset) 
			return ((RTMZDataset)dataset).getPeakListRow(item).toString();
		return null;
	}	
}
