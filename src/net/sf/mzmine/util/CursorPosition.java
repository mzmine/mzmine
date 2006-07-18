/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.util;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;


/**
 *
 */
public class CursorPosition {

    private double retentionTime, mzValue, intensityValue;
    private OpenedRawDataFile dataFile;
    private int scanNumber;

    /**
     * @param retentionTime
     * @param mzValue
     * @param intensityValue
     * @param rawDataFile
     * @param scanNumber
     */
    public CursorPosition(double retentionTime, double mzValue, double intensityValue, OpenedRawDataFile dataFile, int scanNumber) {
        this.retentionTime = retentionTime;
        this.mzValue = mzValue;
        this.intensityValue = intensityValue;
        this.dataFile = dataFile;
        this.scanNumber = scanNumber;
    }


    /**
     * @return Returns the intensityValue.
     */
    public double getIntensityValue() {
        return intensityValue;
    }

    
    /**
     * @param intensityValue The intensityValue to set.
     */
    public void setIntensityValue(double intensityValue) {
        this.intensityValue = intensityValue;
    }

    
    /**
     * @return Returns the mzValue.
     */
    public double getMzValue() {
        return mzValue;
    }

    
    /**
     * @param mzValue The mzValue to set.
     */
    public void setMzValue(double mzValue) {
        this.mzValue = mzValue;
    }

    
    /**
     * @return Returns the retentionTime.
     */
    public double getRetentionTime() {
        return retentionTime;
    }

    
    /**
     * @param retentionTime The retentionTime to set.
     */
    public void setRetentionTime(double retentionTime) {
        this.retentionTime = retentionTime;
    }


    
    /**
     * @return Returns the dataFile.
     */
    public OpenedRawDataFile getDataFile() {
        return dataFile;
    }


    
    /**
     * @param dataFile The dataFile to set.
     */
    public void setDataFile(OpenedRawDataFile dataFile) {
        this.dataFile = dataFile;
    }


    
    /**
     * @return Returns the scanNumber.
     */
    public int getScanNumber() {
        return scanNumber;
    }


    
    /**
     * @param scanNumber The scanNumber to set.
     */
    public void setScanNumber(int scanNumber) {
        this.scanNumber = scanNumber;
    }
    
}
