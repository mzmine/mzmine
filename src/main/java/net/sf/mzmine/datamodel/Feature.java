/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General License as published by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General License for more details.
 * 
 * You should have received a copy of the GNU General License along with MZmine 2; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
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

  enum FeatureStatus {

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
  @Nonnull
  FeatureStatus getFeatureStatus();

  /**
   * This method returns raw M/Z value of the peak
   */
  double getMZ();

  /**
   * This method returns raw retention time of the peak in minutes
   */
  double getRT();

  /**
   * This method returns the raw height of the peak
   */
  double getHeight();

  /**
   * This method returns the raw area of the peak
   */
  double getArea();

  /**
   * Returns raw data file where this peak is present
   */
  @Nonnull
  RawDataFile getDataFile();

  /**
   * This method returns numbers of scans that contain this peak
   */
  @Nonnull
  int[] getScanNumbers();

  /**
   * This method returns the number of most representative scan of this peak
   */
  int getRepresentativeScanNumber();

  /**
   * This method returns the best scan
   */
  default @Nonnull Scan getRepresentativeScan() {
    return getDataFile().getScan(getRepresentativeScanNumber());
  };

  /**
   * This method returns m/z and intensity of this peak in a given scan. This m/z and intensity does
   * not need to match any actual raw data point. May return null, if there is no data point in
   * given scan.
   */
  @Nullable
  DataPoint getDataPoint(int scanNumber);

  /**
   * Returns the retention time range of all raw data points used to detect this peak
   */
  @Nonnull
  Range<Double> getRawDataPointsRTRange();

  /**
   * Returns the range of m/z values of all raw data points used to detect this peak
   */
  @Nonnull
  Range<Double> getRawDataPointsMZRange();

  /**
   * Returns the range of intensity values of all raw data points used to detect this peak
   */
  @Nonnull
  Range<Double> getRawDataPointsIntensityRange();

  /**
   * Returns the number of scan that represents the fragmentation of this peak in MS2 level.
   */
  int getMostIntenseFragmentScanNumber();

  /**
   * Returns all scan numbers that represent fragmentations of this peak in MS2 level.
   */
  int[] getAllMS2FragmentScanNumbers();


  /**
   * Set best fragment scan numbers
   * 
   * @param fragmentScanNumber
   */
  void setFragmentScanNumber(int fragmentScanNumber);

  /**
   * Set all fragment scan numbers
   * 
   * @param allMS2FragmentScanNumbers
   */
  void setAllMS2FragmentScanNumbers(int[] allMS2FragmentScanNumbers);

  /**
   * Returns the isotope pattern of this peak or null if no pattern is attached
   */
  @Nullable
  IsotopePattern getIsotopePattern();

  /**
   * Sets the isotope pattern of this peak
   */
  void setIsotopePattern(@Nonnull IsotopePattern isotopePattern);

  /**
   * Returns the charge of this ion. If the charge is unknown, returns 0.
   */
  int getCharge();

  /**
   * Sets the charge of this ion
   */
  void setCharge(int charge);

  /**
   * This method returns the full width at half maximum (FWHM) of the peak
   */
  Double getFWHM();

  /**
   * This method returns the tailing factor of the peak
   */
  Double getTailingFactor();

  /**
   * This method returns the asymmetry factor of the peak
   */
  Double getAsymmetryFactor();

  /**
   * Sets the full width at half maximum (FWHM)
   */
  void setFWHM(Double fwhm);

  /**
   * Sets the tailing factor
   */
  void setTailingFactor(Double tf);

  /**
   * Sets the asymmetry factor
   */
  void setAsymmetryFactor(Double af);

  // dulab Edit
  void outputChromToFile();

  void setPeakInformation(SimplePeakInformation peakInfoIn);

  SimplePeakInformation getPeakInformation();
  // End dulab Edit

  @Nullable
  default Integer getParentChromatogramRowID() {
    return null;
  }
}
