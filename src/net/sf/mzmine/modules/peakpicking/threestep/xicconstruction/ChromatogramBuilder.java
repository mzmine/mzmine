package net.sf.mzmine.modules.peakpicking.threestep.xicconstruction;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MzPeak;

public interface ChromatogramBuilder {

	public void addScan(Scan scan, MzPeak[] mzValues);

	/**
	 * This method creates an array of peaks with all MzPeaks that have not yet
	 * connected. This function must be called after the last scan of the
	 * DataFile.
	 * 
	 * @return Peak[]
	 */
	public Chromatogram[] finishChromatograms();
	
}
