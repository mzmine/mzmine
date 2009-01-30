/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.shapemodeler.peakmodels;

import java.util.TreeMap;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

public class TrianglePeakModel implements ChromatographicPeak {

    // Model information
	private double rtRight = -1, rtLeft = -1;
	private double alpha, beta;

	// Peak information
	private double rt, height, mz, area;
	private int[] scanNumbers;
	private RawDataFile rawDataFile;
	private PeakStatus status;
	private int representativeScan = -1, fragmentScan = -1;
	private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
			rawDataPointsRTRange;
	private TreeMap<Integer, MzPeak> dataPointsMap;

	public double getArea() {
		return area;
	}

	public RawDataFile getDataFile() {
		return rawDataFile;
	}

	public double getHeight() {
		return height;
	}

	public double getMZ() {
		return mz;
	}

	public int getMostIntenseFragmentScanNumber() {
		return fragmentScan;
	}

	public MzPeak getMzPeak(int scanNumber) {
		return dataPointsMap.get(scanNumber);
	}

	public PeakStatus getPeakStatus() {
		return status;
	}

	public double getRT() {
		return rt;
	}

	public Range getRawDataPointsIntensityRange() {
		return rawDataPointsIntensityRange;
	}

	public Range getRawDataPointsMZRange() {
		return rawDataPointsMZRange;
	}

	public Range getRawDataPointsRTRange() {
		return rawDataPointsRTRange;
	}

	public int getRepresentativeScanNumber() {
		return representativeScan;
	}

	public int[] getScanNumbers() {
		return scanNumbers;
	}
	
	public String toString(){
		return "Triangle peak " + PeakUtils.peakToString(this);
	}

	public TrianglePeakModel(ChromatographicPeak originalDetectedShape,
			int[] scanNumbers, double[] intensities, double[] retentionTimes,
			double resolution) {

		height = originalDetectedShape.getHeight();
		rt = originalDetectedShape.getRT();
		mz = originalDetectedShape.getMZ();
		this.scanNumbers = scanNumbers;
		rawDataFile = originalDetectedShape.getDataFile();
		rawDataPointsIntensityRange = originalDetectedShape
				.getRawDataPointsIntensityRange();
		rawDataPointsMZRange = originalDetectedShape.getRawDataPointsMZRange();
		rawDataPointsRTRange = originalDetectedShape.getRawDataPointsRTRange();
		dataPointsMap = new TreeMap<Integer, MzPeak>();
		status = originalDetectedShape.getPeakStatus();

		rtRight = retentionTimes[retentionTimes.length - 1];
		rtLeft = retentionTimes[0];
		
		alpha = (double) Math.atan(height / (rt - rtLeft));
		beta = (double) Math.atan(height / (rtRight - rt));

		// Calculate intensity of each point in the shape.
		double shapeHeight, currentRT, previousRT, previousHeight;
		MzPeak mzPeak;

		previousHeight = calculateIntensity(retentionTimes[0]);
		previousRT = retentionTimes[0];

		for (int i = 0; i < retentionTimes.length; i++) {
			
			currentRT = retentionTimes[i];
			shapeHeight = calculateIntensity(currentRT);
			mzPeak = new SimpleMzPeak(new SimpleDataPoint(mz, shapeHeight));
			dataPointsMap.put(scanNumbers[i], mzPeak);

			area += (currentRT - previousRT) * (shapeHeight + previousHeight)
					/ 2;
			previousRT = currentRT;
			previousHeight = shapeHeight;
		}

	}

	private double calculateIntensity(double retentionTime) {

		double intensity = 0;
		if ((retentionTime > rtLeft) && (retentionTime < rtRight)) {
			if (retentionTime <= rt) {
				intensity = (double) Math.tan(alpha) * (retentionTime - rtLeft);
			}
			if (retentionTime > rt) {
				intensity = (double) Math.tan(beta) * (rtRight - retentionTime);
			}
		}

		return intensity;
	}

}
