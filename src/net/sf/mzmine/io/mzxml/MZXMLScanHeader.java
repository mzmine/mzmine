/*
 * Copyright 2006 Okinawa Institute of Science and Technology
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

/**
 * 
 */
package net.sf.mzmine.io.mzxml;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.ScanHeader;

/**
 *
 */
class MZXMLScanHeader implements ScanHeader {

    private int scanNumber;
    private MZXMLFile rawDataFile;
    
    private int msLevel;
    
    private double precursorRT, precursorMZ;
    private int precursorScanNumber;
    
    private double scanAcquisitionTime, scanDuration;
    
    private double MZRangeMin;
    private double MZRangeMax;
    
    private double basePeakMZ;
    private double basePeakIntensity;
    
    private double totalIonCurrent;
    
 
    /**
     * 
     */
    public MZXMLScanHeader(MZXMLFile rawDataFile, int scanNumber) {
        this.rawDataFile = rawDataFile;
        this.scanNumber = scanNumber;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getRawData()
     */
    public RawDataFile getRawData() {
        return rawDataFile;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getScanNumber()
     */
    public int getScanNumber() {
        return scanNumber;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getMSLevel()
     */
    public int getMSLevel() {
        return msLevel;
    }




    /**
     * @return Returns the basePeakIntensity.
     */
    public double getBasePeakIntensity() {
        return basePeakIntensity;
    }

    /**
     * @return Returns the basePeakMZ.
     */
    public double getBasePeakMZ() {
        return basePeakMZ;
    }


    /**
     * @return Returns the mZRangeMax.
     */
    public double getMZRangeMax() {
        return MZRangeMax;
    }

    /**
     * @return Returns the mZRangeMin.
     */
    public double getMZRangeMin() {
        return MZRangeMin;
    }

    /**
     * @return Returns the precursorMZ.
     */
    public double getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @return Returns the precursorRT.
     */
    public double getPrecursorRT() {
        return precursorRT;
    }

    /**
     * @return Returns the precursorScanNumber.
     */
    public int getPrecursorScanNumber() {
        return precursorScanNumber;
    }

    
    /**
     * @return Returns the scanAcquisitionTime.
     */
    public double getScanAcquisitionTime() {
        return scanAcquisitionTime;
    }

    /**
     * @return Returns the scanDuration.
     */
    public double getScanDuration() {
        return scanDuration;
    }

    /**
     * @return Returns the totalIonCurrent.
     */
    public double getTotalIonCurrent() {
        return totalIonCurrent;
    }



}
