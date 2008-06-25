package net.sf.mzmine.modules.peakpicking.twostep.xicconstruction;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;

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
