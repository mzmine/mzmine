/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General License as published by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * License for more details.
 *
 * You should have received a copy of the GNU General License along with MZmine; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.data;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimplePeakInformation;
import java.util.Objects;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface defines the properties of a detected feature
 */
public interface Feature {

  /**
   * This method returns the status of the feature
   */
  @Nonnull
  FeatureStatus getFeatureStatus();

  /**
   * This method returns raw M/Z value of the feature
   */
  double getMZ();

  /**
   * This method returns raw retention time of the feature in minutes
   */
  float getRT();

  /**
   * This method returns the raw height of the feature
   */
  float getHeight();

  /**
   * This method returns the raw area of the feature
   */
  float getArea();

  /**
   * Returns raw data file where this feature is present
   */
  @Nonnull
  RawDataFile getRawDataFile();

  /**
   * This method returns numbers of scans that contain this feature
   */
  @Nonnull
  ObservableList<Integer> getScanNumbers();

  /**
   * This method returns the number of most representative scan of this feature
   */
  int getRepresentativeScanNumber();

  /**
   * This method returns the best scan
   */
  default @Nonnull
  Scan getRepresentativeScan() {
    return Objects.requireNonNull(getRawDataFile().getScan(getRepresentativeScanNumber()));
  };

  /**
   * This method returns m/z and intensity of this feature in a given scan. This m/z and intensity does
   * not need to match any actual raw data point. May return null, if there is no data point in
   * given scan.
   */
  @Nullable
  DataPoint getDataPoint(int scanNumber);

  /**
   * Returns all data points.
   */
  ObservableList<DataPoint> getDataPoints();

  /**
   * Returns the retention time range of all raw data points used to detect this feature
   */
  @Nonnull
  Range<Float> getRawDataPointsRTRange();

  /**
   * Returns the range of m/z values of all raw data points used to detect this feature
   */
  @Nonnull
  Range<Double> getRawDataPointsMZRange();

  /**
   * Returns the range of intensity values of all raw data points used to detect this feature
   */
  @Nonnull
  Range<Float> getRawDataPointsIntensityRange();

  /**
   * Returns the number of scan that represents the fragmentation of this feature in MS2 level.
   */
  int getMostIntenseFragmentScanNumber();

  /**
   * Returns all scan numbers that represent fragmentations of this feature in MS2 level.
   */
  ObservableList<Integer> getAllMS2FragmentScanNumbers();

  /**
   * Sets raw M/Z value of the feature
   */
  void setMZ(double mz);

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
  //void setAllMS2FragmentScanNumbers(List<Integer> allMS2FragmentScanNumbers); ?
  void setAllMS2FragmentScanNumbers(ObservableList<Integer> allMS2FragmentScanNumbers);

  /**
   * Returns the isotope pattern of this feature or null if no pattern is attached
   */
  @Nullable
  IsotopePattern getIsotopePattern();

  /**
   * Sets the isotope pattern of this feature
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
   * This method returns the full width at half maximum (FWHM) of the feature
   */
  float getFWHM();

  /**
   * This method returns the tailing factor of the feature
   */
  float getTailingFactor();

  /**
   * This method returns the asymmetry factor of the feature
   */
  float getAsymmetryFactor();

  /**
   * Sets the full width at half maximum (FWHM)
   */
  void setFWHM(double fwhm);

  /**
   * Sets the tailing factor
   */
  void setTailingFactor(double tf);

  /**
   * Sets the asymmetry factor
   */
  void setAsymmetryFactor(double af);

  // dulab Edit
  void outputChromToFile();

  void setPeakInformation(SimplePeakInformation peakInfo);

  SimplePeakInformation getPeakInformation();
  // End dulab Edit

  @Nullable
  default Integer getParentChromatogramRowID() {
    return null;
  }

  @Nullable
  FeatureList getFeatureList();

  void setFeatureList(@Nonnull FeatureList featureList);

}
