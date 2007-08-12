package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;

import net.sf.mzmine.io.RawDataFile;

import org.jfree.data.xy.XYDataset;

public interface ProjectionPlotDataset extends XYDataset {

	public RawDataFile getRawDataFile(int item);
	
	public int getGroupNumber(int item);
	
	public Object getGroupParameterValue(int groupNumber);
	
	public int getNumberOfGroups();
	
	public String getXLabel();
	
	public String getYLabel();
	
	
	
}
