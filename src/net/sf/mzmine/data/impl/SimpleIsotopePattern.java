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

import java.util.HashSet;
import java.util.Set;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.Range;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

    private int charge = UNKNOWN_CHARGE;
    private Set<ChromatographicPeak> peaks;
    private ChromatographicPeak representativePeak;

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
     * @param p Peak to add
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
     * @param charge The charge to set.
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
     * @param representativePeak The representativePeak to set.
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
    public float getMZ() {
        return representativePeak.getMZ();
    }

    /**
     * @see net.sf.mzmine.data.ChromatographicPeak#getRT()
     */
    public float getRT() {
        return representativePeak.getRT();
    }

    /**
     * @see net.sf.mzmine.data.ChromatographicPeak#getHeight()
     */
    public float getHeight() {
        return representativePeak.getHeight();
    }

    /**
     * @see net.sf.mzmine.data.ChromatographicPeak#getArea()
     */
    public float getArea() {
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
    public DataPoint getDataPoint(int scanNumber) {
        return representativePeak.getDataPoint(scanNumber);
    }

    /**
     * @see net.sf.mzmine.data.ChromatographicPeak#getRawDatapoint(int)
     */
    public DataPoint[] getRawDataPoints(int scanNumber) {
        return representativePeak.getRawDataPoints(scanNumber);
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

    public float getMass() {
        if (charge == UNKNOWN_CHARGE) return representativePeak.getMZ();
        else return representativePeak.getMZ() * charge;
    }

	public void setMZ(float mz) {
		// TODO Auto-generated method stub
		
	}

}