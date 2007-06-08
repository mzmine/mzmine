package net.sf.mzmine.modules.dataanalysis.projectionplots;

import net.sf.mzmine.io.OpenedRawDataFile;

import org.jfree.data.xy.XYDataset;

public interface ProjectionPlotDataset extends XYDataset {

	public OpenedRawDataFile getOpenedRawDataFile(int item);
	
	public String getXLabel();
	
	public String getYLabel();
	
}
