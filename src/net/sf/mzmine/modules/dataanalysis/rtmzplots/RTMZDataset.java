package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import net.sf.mzmine.data.PeakListRow;

import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYZDataset;

public interface RTMZDataset extends XYZDataset {

	public abstract PeakListRow getPeakListRow(int item);
	
}
