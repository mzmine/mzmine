/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.data.impl;

import java.text.Format;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.Range;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

	private int charge = UNKNOWN_CHARGE;
	private Set<ChromatographicPeak> peaks;
	private ChromatographicPeak representativePeak;
	private IsotopePatternStatus patternStatus = IsotopePatternStatus.DETECTED;

	public SimpleIsotopePattern() {
		peaks = new HashSet<ChromatographicPeak>();
	}

	/**
	 * @param charge
	 * @param peaks
	 * @param representativePeak
	 */
	public SimpleIsotopePattern(int charge, ChromatographicPeak peaks[],
			ChromatographicPeak representativePeak) {
		this();
		this.charge = charge;
		for (ChromatographicPeak p : peaks)
			this.peaks.add(p);
		this.representativePeak = representativePeak;
	}

	/**
	 * Adds new isotope (peak) to this pattern
	 * 
	 * @param p
	 *            Peak to add
	 */
	public void addPeak(ChromatographicPeak p) {
		peaks.add(p);
	}

	/**
	 * Returns the charge state of peaks in the pattern
	 * 
	 * @see net.sf.mzmine.data.IsotopePattern#getCharge()
	 */
	public int getCharge() {
		return charge;
	}

	/**
	 * @param charge
	 *            The charge to set.
	 */
	public void setCharge(int charge) {
		this.charge = charge;
	}

	/**
	 * @see net.sf.mzmine.data.IsotopePattern#getOriginalPeaks()
	 */
	public ChromatographicPeak[] getOriginalPeaks() {
		return peaks.toArray(new ChromatographicPeak[0]);
	}

	/**
	 * @see net.sf.mzmine.data.IsotopePattern#getRepresentativePeak()
	 */
	public ChromatographicPeak getRepresentativePeak() {
		return representativePeak;
	}

	/**
	 * @param representativePeak
	 *            The representativePeak to set.
	 */
	public void setRepresentativePeak(ChromatographicPeak representativePeak) {
		this.representativePeak = representativePeak;
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getDataFile()
	 */
	public RawDataFile getDataFile() {
		return representativePeak.getDataFile();
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getMZ()
	 */
	public double getMZ() {
		return representativePeak.getMZ();
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getRT()
	 */
	public double getRT() {
		return representativePeak.getRT();
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getHeight()
	 */
	public double getHeight() {
		return representativePeak.getHeight();
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getArea()
	 */
	public double getArea() {
		return representativePeak.getArea();
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getScanNumbers()
	 */
	public int[] getScanNumbers() {
		return representativePeak.getScanNumbers();
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getPeakStatus()
	 */
	public PeakStatus getPeakStatus() {
		return representativePeak.getPeakStatus();
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getRawDatapoint(int)
	 */
	public MzPeak getMzPeak(int scanNumber) {
		return representativePeak.getMzPeak(scanNumber);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return representativePeak.toString();
	}

	public Range getRawDataPointsIntensityRange() {
		return representativePeak.getRawDataPointsIntensityRange();
	}

	public Range getRawDataPointsMZRange() {
		return representativePeak.getRawDataPointsMZRange();
	}

	public Range getRawDataPointsRTRange() {
		return representativePeak.getRawDataPointsRTRange();
	}

	public double getIsotopeMass() {
		if (charge == UNKNOWN_CHARGE)
			return representativePeak.getMZ();
		else
			return representativePeak.getMZ() * charge;
	}

	public void setMZ(double mz) {
	}

	public DataPoint[] getDataPoints() {

		RawDataFile dataFile = representativePeak.getDataFile();
		int repScanNumber = representativePeak.getRepresentativeScanNumber();
		Scan scan = dataFile.getScan(repScanNumber);

		return scan.getDataPoints();
	}

	public int getNumberOfDataPoints() {
		return getDataPoints().length;
	}

	public int getRepresentativeScanNumber() {
		return representativePeak.getRepresentativeScanNumber();
	}

	public String getIsotopeInfo() {
		StringBuffer buf = new StringBuffer();
		Format mzFormat = MZmineCore.getMZFormat();
		Format timeFormat = MZmineCore.getRTFormat();
		buf.append(mzFormat.format(getIsotopeMass()));
		buf.append(" m/z @");
		buf.append(timeFormat.format(getRT()));
		return buf.toString();
	}

	public int getNumberOfIsotopes() {
		return peaks.size();
	}

	public Range getIsotopeMzRange() {
		
		Iterator<ChromatographicPeak> itr = peaks.iterator();
		ChromatographicPeak cp;
		double repRT = representativePeak.getMZ(), H = 1.0078f;
		Range mzRange = new Range(repRT);
		while (itr.hasNext()) {
			cp = itr.next();
			mzRange.extendRange(cp.getRawDataPointsMZRange());
		}

		// Increase range by +/- one hydrogen
		if ((mzRange.getMin() > (repRT - H))
				|| (mzRange.getMax() < (repRT + 2 * H))) {
			double extendRange = mzRange.getMin() - H;
			mzRange.extendRange(extendRange);
			extendRange = mzRange.getMax() + 2 * H;
			mzRange.extendRange(extendRange);
		}

		return mzRange;
	}

	public IsotopePatternStatus getIsotopePatternStatus() {
		return patternStatus;
	}

	public String getFormula() {
		return null;
	}

	public double getIsotopeHeight() {
		return getRepresentativePeak().getHeight();
	}

	public DataPoint[] getIsotopes() {
		
		TreeSet<DataPoint> dataPoints = new TreeSet<DataPoint>(
				new DataPointSorter(true, true));
		ChromatographicPeak cp;
		Iterator<ChromatographicPeak> itr = peaks.iterator();
		
		while (itr.hasNext()) {
			cp = itr.next();
			dataPoints.add(new SimpleDataPoint(cp.getMZ(), cp.getHeight()));
		}
		
		return dataPoints.toArray(new DataPoint[0]);
	}

	public int getMostIntenseFragmentScanNumber() {
		return representativePeak.getMostIntenseFragmentScanNumber();
	}

}