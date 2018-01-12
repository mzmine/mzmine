/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.datamodel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;

/**
 * This interface defines the properties of a detected peak
 */
public interface Feature {

    public enum FeatureStatus {

	/**
	 * Peak was not found
	 */
	UNKNOWN,

	/**
	 * Peak was found in primary peak picking
	 */
	DETECTED,

	/**
	 * Peak was estimated in secondary peak picking
	 */
	ESTIMATED,

	/**
	 * Peak was defined manually
	 */
	MANUAL

    }

    /**
     * This method returns the status of the peak
     */
    public @Nonnull FeatureStatus getFeatureStatus();

    /**
     * This method returns raw M/Z value of the peak
     */
    public double getMZ();

    /**
     * This method returns raw retention time of the peak in minutes
     */
    public double getRT();

    /**
     * This method returns the raw height of the peak
     */
    public double getHeight();

    /**
     * This method returns the raw area of the peak
     */
    public double getArea();

    /**
     * Returns raw data file where this peak is present
     */
    public @Nonnull RawDataFile getDataFile();

    /**
     * This method returns numbers of scans that contain this peak
     */
    public @Nonnull int[] getScanNumbers();

    /**
     * This method returns number of most representative scan of this peak
     */
    public int getRepresentativeScanNumber();

    /**
     * This method returns m/z and intensity of this peak in a given scan. This
     * m/z and intensity does not need to match any actual raw data point. May
     * return null, if there is no data point in given scan.
     */
    public @Nullable DataPoint getDataPoint(int scanNumber);

    /**
     * Returns the retention time range of all raw data points used to detect
     * this peak
     */
    public @Nonnull Range<Double> getRawDataPointsRTRange();

    /**
     * Returns the range of m/z values of all raw data points used to detect
     * this peak
     */
    public @Nonnull Range<Double> getRawDataPointsMZRange();

    /**
     * Returns the range of intensity values of all raw data points used to
     * detect this peak
     */
    public @Nonnull Range<Double> getRawDataPointsIntensityRange();

    /**
     * Returns the number of scan that represents the fragmentation of this peak
     * in MS2 level.
     */
    public int getMostIntenseFragmentScanNumber();

    /**
     * Returns the isotope pattern of this peak or null if no pattern is
     * attached
     */
    public @Nullable IsotopePattern getIsotopePattern();

    /**
     * Sets the isotope pattern of this peak
     */
    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern);

    /**
     * Returns the charge of this ion. If the charge is unknown, returns 0.
     */
    public int getCharge();

    /**
     * Sets the charge of this ion
     */
    public void setCharge(int charge);

   /**
    * This method returns the full width at half maximum (FWHM) of the peak
    */
    public Double getFWHM();

    /**
     * This method returns the tailing factor of the peak
     */
    public Double getTailingFactor();

    /**
     * This method returns the asymmetry factor of the peak
     */
    public Double getAsymmetryFactor();

    /**
     * Sets the full width at half maximum (FWHM)
     */
    public void setFWHM(Double fwhm);

    /**
     * Sets the tailing factor
     */
    public void setTailingFactor(Double tf);

    /**
     * Sets the asymmetry factor
     */
    public void setAsymmetryFactor(Double af);

    //dulab Edit
    public void outputChromToFile();
    public void setPeakInformation(SimplePeakInformation peakInfoIn);
    public SimplePeakInformation getPeakInformation();
    //End dulab Edit

}
