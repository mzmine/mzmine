package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import net.sf.mzmine.data.PeakListRow;

import org.jfree.data.xy.AbstractXYZDataset;

public abstract class RTMZDataset extends AbstractXYZDataset {

	public abstract PeakListRow getPeakListRow(int item);
	
}
