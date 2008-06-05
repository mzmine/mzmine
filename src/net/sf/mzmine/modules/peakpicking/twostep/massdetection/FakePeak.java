package net.sf.mzmine.modules.peakpicking.twostep.massdetection;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.util.Range;

class FakePeak implements Peak {

	private DataPoint datapoint;
	private Range mzRange, intensityRange;
	
	
	public FakePeak(float mz, float intensity ) {//, float rt, int scanNumber) {
		super();
        datapoint = new SimpleDataPoint(mz, intensity);
        mzRange = new Range(mz);
        intensityRange = new Range(intensity);
	}

	public float getArea() {
		return 0;
	}

	public RawDataFile getDataFile() {
		return null;
	}

	public DataPoint getDataPoint(int scanNumber) {
		return datapoint;
	}

	public float getHeight() {
		return 0;
	}

	public float getMZ() {
		return 0;
	}

	public PeakStatus getPeakStatus() {
		return null;
	}

	public float getRT() {
		return 0;
	}

	public DataPoint[] getRawDataPoints(int scanNumber) {
		return null;
	}

	public Range getRawDataPointsIntensityRange() {
		return intensityRange;
	}

	public Range getRawDataPointsMZRange() {
		return mzRange;
	}

	public Range getRawDataPointsRTRange() {
		return null;
	}

	public int[] getScanNumbers() {
		return null;
	}

}
