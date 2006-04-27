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


/**
 *
 */
package net.sf.mzmine.io.netcdf;

import net.sf.mzmine.io.Scan;

/**
 *
 */
class NetCDFScan implements Scan {

    private int scanNumber;
    //private int msLevel;
    private double mzValues[], intensityValues[];
    //private double precursorMZ;
    private double retentionTime;
    private double mzRangeMin, mzRangeMax;
    private double basePeakMZ, basePeakIntensity;

	NetCDFScan(int scanNumber, double retentionTime, double mzValues[], double intensityValues[]) {
		this.scanNumber = scanNumber;
		this.retentionTime = retentionTime;
		this.mzValues = mzValues;
		this.intensityValues = intensityValues;

		// pickup m/z range min and max value
		if (mzValues.length>0) {
			mzRangeMin = mzValues[0];
			mzRangeMax = mzValues[mzValues.length-1];
		}

        // find the base peak
        for (int i = 0; i < intensityValues.length; i++) {
            if (intensityValues[i] > basePeakIntensity) {
                basePeakIntensity = intensityValues[i];
                basePeakMZ = mzValues[i];
            }
        }

	}

    /**
     * @return Returns the intensityValues.
     */
    public double[] getIntensityValues() {
        return intensityValues;
    }

    /**
     * @return Returns the mZValues.
     */
    public double[] getMZValues() {
        return mzValues;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getNumberOfDataPoints()
     */
    public int getNumberOfDataPoints() {
        return mzValues.length;
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
        return 1;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getPrecursorMZ()
     */
    public double getPrecursorMZ() {
        return 0.0;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getScanAcquisitionTime()
     */
    public double getRetentionTime() {
        return retentionTime;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getMZRangeMin()
     */
    public double getMZRangeMin() {
        return mzRangeMin;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getMZRangeMax()
     */
    public double getMZRangeMax() {
        return mzRangeMax;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getBasePeakMZ()
     */
    public double getBasePeakMZ() {
        return basePeakMZ;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getBasePeakIntensity()
     */
    public double getBasePeakIntensity() {
        return basePeakIntensity;
    }

    /**
     * @see net.sf.mzmine.io.Scan#isCentroided()
     */
    public boolean isCentroided() {
		return false;
	}


}
