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

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import java.util.List;
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
  Double getMZ();

  /**
   * Sets raw M/Z value of the feature
   */
  void setMZ(Double mz);

  /**
   * This method returns raw retention time of the feature in minutes
   */
  Float getRT();

  /**
   * Sets retention time of the feature
   */
  void setRT(Float rt);

  /**
   * This method returns the raw height of the feature
   */
  Double getHeight();

  /**
   * Sets height of the feature
   */
  void setHeight(Double height);

  /**
   * This method returns the raw area of the feature
   */
  Double getArea();

  /**
   * Sets area of the feature
   */
  void setArea(Double area);

  /**
   * Returns raw data file where this feature is present
   */
  @Nullable
  RawDataFile getRawDataFile();

  /**
   * This method returns numbers of scans that contain this feature
   */
  @Nonnull
  List<Scan> getScanNumbers();

  /**
   * Used to loop over scans and data points in combination with ({@link #getDataPointAtIndex(int)}
   *
   * @param i index
   * @return scan at index or null
   */
  @Nullable
  default Scan getScanAtIndex(int i) {
    List<Scan> scans = getScanNumbers();
    return scans == null ? null : scans.get(i);
  }

  /**
   * Used to loop over retention time, scans, and data points in combination with ({@link
   * #getDataPointAtIndex(int)}
   *
   * @param i index
   * @return retention time at index or null
   */
  @Nullable
  default Float getRetentionTimeAtIndex(int i) {
    List<Scan> scans = getScanNumbers();
    return scans == null ? null : scans.get(i).getRetentionTime();
  }

  /**
   * Used to loop over scans and data points in combination with ({@link #getDataPointAtIndex(int)}
   *
   * @param i index
   * @return data point at index
   */
  @Deprecated
  @Nullable
  default DataPoint getDataPointAtIndex(int i) {
    List<DataPoint> dataPoints = getDataPoints();
    return dataPoints == null ? null : dataPoints.get(i);
  }

  /**
   * This method returns the best scan (null if no raw file is attached)
   */
  @Nullable
  Scan getRepresentativeScan();

  /**
   * The representative scan of this feature
   *
   * @param scan MS1 scan at highest intensity
   */
  void setRepresentativeScan(Scan scan);

  /**
   * This method returns m/z and intensity of this feature in a given scan. This m/z and intensity
   * does not need to match any actual raw data point. May return null, if there is no data point in
   * given scan. Tip: Better loop over the data points and scans with an index to retrieve all
   * information
   */
  @Nullable
  @Deprecated
  DataPoint getDataPoint(Scan scan);

  /**
   * Returns all data points.
   */
  @Deprecated
  List<DataPoint> getDataPoints();

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
  Range<Double> getRawDataPointsIntensityRange();

  /**
   * Returns the number of scan that represents the fragmentation of this feature in MS2 level.
   */
  Scan getMostIntenseFragmentScan();

  /**
   * Returns all scan numbers that represent fragmentations of this feature in MS2 level.
   */
  ObservableList<Scan> getAllMS2FragmentScans();

  /**
   * Set all fragment scan numbers
   *
   * @param allMS2FragmentScanNumbers
   */
  void setAllMS2FragmentScans(ObservableList<Scan> allMS2FragmentScanNumbers);

  /**
   * @return The mobility if no mobility was set. Note that mobility can have different units.
   * @see Feature#getMobilityUnit()
   */
  @Nullable
  Float getMobility();

  /**
   * Sets the mobility of this feature. Note that mobility has a unit, which should be set by {@link
   * Feature#setMobilityUnit(MobilityType)}.
   *
   * @param mobility The mobility.
   */
  void setMobility(Float mobility);

  /**
   * @return The unit of the mobility of this feature or null, if no mobility unit was set.
   */
  @Nullable
  MobilityType getMobilityUnit();

  /**
   * Sets the {@link MobilityType} of this feature.
   *
   * @param mobilityUnit define mobility unit
   */
  void setMobilityUnit(MobilityType mobilityUnit);

  /**
   * @return The ccs value or null, if no value was set.
   */
  @Nullable
  Float getCCS();

  /**
   * Sets the collision cross section of this feature.
   *
   * @param ccs The ccs value.
   */
  void setCCS(Float ccs);

  /**
   * @return The mobility range of this feature or null, if no range was set.
   */
  @Nullable
  Range<Float> getMobilityRange();

  /**
   * Sets the mobiltiy range
   */
  void setMobilityRange(Range<Float> range);

  /**
   * Set best fragment scan
   *
   * @param fragmentScan set best fragmentation scan
   */
  void setFragmentScan(Scan fragmentScan);

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
  Float getFWHM();

  /**
   * Sets the full width at half maximum (FWHM)
   */
  void setFWHM(Float fwhm);

  /**
   * This method returns the tailing factor of the feature
   */
  Float getTailingFactor();

  /**
   * Sets the tailing factor
   */
  void setTailingFactor(Float tf);

  /**
   * This method returns the asymmetry factor of the feature
   */
  Float getAsymmetryFactor();

  /**
   * Sets the asymmetry factor
   */
  void setAsymmetryFactor(Float af);

  // dulab Edit
  void outputChromToFile();

  SimpleFeatureInformation getFeatureInformation();

  void setFeatureInformation(SimpleFeatureInformation featureInfo);
  // End dulab Edit

  @Nullable
  default Integer getParentChromatogramRowID() {
    return null;
  }

  @Nullable
  FeatureList getFeatureList();

  void setFeatureList(@Nonnull FeatureList featureList);

  default int getNumberOfDataPoints() {
    List<DataPoint> dp = getDataPoints();
    return dp == null ? -1 : dp.size();
  }


  /**
   * The detected data points of this feature/chromatogram
   *
   * @return
   */
  default IonTimeSeries<? extends Scan> getFeatureData() {
    throw new UnsupportedOperationException(
        "Get feature data is not implemented for this sub class. Use ModularFeature or implement");
  }
}
