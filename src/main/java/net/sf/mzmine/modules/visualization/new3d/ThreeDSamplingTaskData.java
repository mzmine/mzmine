package net.sf.mzmine.modules.visualization.new3d;

import java.util.logging.Logger;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

public class ThreeDSamplingTaskData {
	public Logger logger;
	public RawDataFile dataFile;
	public Scan[] scans;
	public Range<Double> rtRange;
	public Range<Double> mzRange;
	public int rtResolution;
	public int mzResolution;
	public int retrievedScans;
	public ThreeDDisplay display;
	public ThreeDBottomPanel bottomPanel;
	public double maxBinnedIntensity;

	public ThreeDSamplingTaskData(Logger logger, int retrievedScans) {
		this.logger = logger;
		this.retrievedScans = retrievedScans;
	}
}