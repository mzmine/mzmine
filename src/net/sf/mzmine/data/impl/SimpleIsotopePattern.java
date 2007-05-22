/*
 * Copyright 2006-2007 The MZmine Development Team
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

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;

/**
 * 
 */
public class SimpleIsotopePattern implements
        IsotopePattern {

    private int charge;
    private Set<Peak> peaks;
    private Peak representativePeak;

    public SimpleIsotopePattern() {
        peaks = new HashSet<Peak>();
    }

    /**
     * @param charge
     * @param peaks
     * @param representativePeak
     */
    public SimpleIsotopePattern(int charge, Peak peaks[],
            Peak representativePeak) {
        this();
        this.charge = charge;
        for (Peak p : peaks)
            this.peaks.add(p);
        this.representativePeak = representativePeak;
    }

    /**
     * Adds new isotope (peak) to this pattern
     * 
     * @param p Peak to add
     */
    public void addPeak(Peak p) {
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
    public Peak[] getOriginalPeaks() {
        return peaks.toArray(new Peak[0]);
    }

    /**
     * @see net.sf.mzmine.data.IsotopePattern#getRepresentativePeak()
     */
    public Peak getRepresentativePeak() {
        return representativePeak;
    }

    /**
     * @param representativePeak The representativePeak to set.
     */
    public void setRepresentativePeak(Peak representativePeak) {
        this.representativePeak = representativePeak;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getArea()
     */
    public double getArea() {
        return representativePeak.getArea();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDataFile()
     */
    public OpenedRawDataFile getDataFile() {
        return representativePeak.getDataFile();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDuration()
     */
    public double getDuration() {
        return representativePeak.getDuration();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getHeight()
     */
    public double getHeight() {
        return representativePeak.getHeight();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getMZ()
     */
    public double getMZ() {
        return representativePeak.getMZ();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getMaxMZ()
     */
    public double getMaxMZ() {
        return representativePeak.getMaxMZ();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getMaxRT()
     */
    public double getMaxRT() {
        return representativePeak.getMaxRT();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getMinMZ()
     */
    public double getMinMZ() {
        return representativePeak.getMinMZ();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getMinRT()
     */
    public double getMinRT() {
        return representativePeak.getMinRT();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getPeakStatus()
     */
    public PeakStatus getPeakStatus() {
        return representativePeak.getPeakStatus();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getRT()
     */
    public double getRT() {
        return representativePeak.getRT();
    }

    /**
     * @see net.sf.mzmine.data.Peak#getRawDatapoints(int)
     */
    public double[][] getRawDatapoints(int scanNumber) {
        return representativePeak.getRawDatapoints(scanNumber);
    }

    /**
     * @see net.sf.mzmine.data.Peak#getScanNumbers()
     */
    public int[] getScanNumbers() {
        return representativePeak.getScanNumbers();
    }

}