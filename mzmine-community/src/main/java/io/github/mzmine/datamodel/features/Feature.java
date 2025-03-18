/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.util.scans.FragmentScanSorter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface defines the properties of a detected feature
 */
public interface Feature {

  /**
   * This method returns the status of the feature
   */
  @NotNull FeatureStatus getFeatureStatus();

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
  void setRT(float rt);

  /**
   * This method returns the raw height of the feature
   */
  Float getHeight();

  /**
   * Sets height of the feature
   */
  void setHeight(Float height);

  /**
   * This method returns the raw area of the feature
   */
  Float getArea();

  /**
   * Sets area of the feature
   */
  void setArea(float area);

  /**
   * Returns raw data file where this feature is present
   */
  @Nullable RawDataFile getRawDataFile();

  /**
   * This method returns numbers of scans that contain this feature
   */
  @NotNull List<Scan> getScanNumbers();

  /**
   * Used to loop over scans and data points in combination with ({@link #getDataPointAtIndex(int)}
   *
   * @param i index
   * @return
   */
  @Nullable
  default Scan getScanAtIndex(int i) {
    List<Scan> scans = getScanNumbers();
    return scans.get(i);
  }

  /**
   * Used to loop over retention time, scans, and data points in combination with
   * ({@link #getDataPointAtIndex(int)}
   *
   * @param i index
   * @return
   */
  @Nullable
  default float getRetentionTimeAtIndex(int i) {
    List<Scan> scans = getScanNumbers();
    return scans.get(i).getRetentionTime();
  }

  /**
   * Used to loop over scans and data points in combination with ({@link #getDataPointAtIndex(int)}
   *
   * @param i
   * @return
   */
  @Deprecated
  @Nullable DataPoint getDataPointAtIndex(int i);

  /**
   * This method returns the best scan (null if no raw file is attached)
   */
  @Nullable Scan getRepresentativeScan();

  /**
   * The representative scan of this feature
   *
   * @param scan
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
  @NotNull Range<Float> getRawDataPointsRTRange();

  /**
   * Returns the range of m/z values of all raw data points used to detect this feature
   */
  @NotNull Range<Double> getRawDataPointsMZRange();

  /**
   * Returns the range of intensity values of all raw data points used to detect this feature
   */
  @NotNull Range<Float> getRawDataPointsIntensityRange();

  /**
   * Returns the scan that represents the fragmentation of this feature in MS2 level. The first in
   * the list of all fragment scans
   */
  Scan getMostIntenseFragmentScan();

  /**
   * Sorted list of all fragmentation scans of this feature. First is the representative ("best")
   * fragmentation spectrum.
   */
  @NotNull List<Scan> getAllMS2FragmentScans();

  /**
   * Set all fragmentation scans. First element is "best" representative scan. No sorting is
   * applied.
   *
   * @param allMS2FragmentScanNumbers usually sorted by most intense scans first (represantative
   *                                  scan as first element)
   */
  void setAllMS2FragmentScans(List<Scan> allMS2FragmentScanNumbers);


  /**
   * Set all fragmentation scans. First element is "best" representative scan. Option to apply the
   * default sorting.
   *
   * @param allFragmentScans    usually sorted by most intense scans first (represantative scan as
   *                            first element)
   * @param applyDefaultSorting applies the default sorting to the list (in place)
   */
  default void setAllMS2FragmentScans(List<Scan> allFragmentScans, boolean applyDefaultSorting) {
    if (allFragmentScans == null || allFragmentScans.isEmpty()) {
      setAllMS2FragmentScans(allFragmentScans);
      return;
    }

    if (applyDefaultSorting) {
      // in case list is immutable
      allFragmentScans = new ArrayList<>(allFragmentScans);
      allFragmentScans.sort(FragmentScanSorter.DEFAULT_TIC);
    }
    setAllMS2FragmentScans(allFragmentScans);
  }

  /**
   * @return The mobility or null if no mobility was set. Note that mobility can have different
   * units.
   * @see Feature#getMobilityUnit()
   */
  @Nullable Float getMobility();

  /**
   * Sets the mobility of this feature. Note that mobility has a unit, which should be set by
   * {@link Feature#setMobilityUnit(MobilityType)}.
   *
   * @param mobility The mobility.
   */
  void setMobility(Float mobility);

  /**
   * @return The unit of the mobility of this feature or null, if no mobility unit was set.
   */
  @Nullable MobilityType getMobilityUnit();

  /**
   * Sets the {@link MobilityType} of this feature.
   *
   * @param mobilityUnit
   */
  void setMobilityUnit(MobilityType mobilityUnit);

  /**
   * @return The ccs value or null, if no value was set.
   */
  @Nullable Float getCCS();

  /**
   * Sets the collision cross section of this feature.
   *
   * @param ccs The ccs value.
   */
  void setCCS(Float ccs);

  /**
   * @return The mobility range of this feature or null, if no range was set.
   */
  @Nullable Range<Float> getMobilityRange();

  /**
   * Sets the mobiltiy range
   */
  void setMobilityRange(Range<Float> range);

  /**
   * Returns the isotope pattern of this feature or null if no pattern is attached
   *
   * @return
   */
  IsotopePattern getIsotopePattern();

  /**
   * Sets the isotope pattern of this feature
   */
  void setIsotopePattern(@NotNull IsotopePattern isotopePattern);

  /**
   * Returns the charge of this ion. If the charge is unknown, returns 0.
   */
  Integer getCharge();

  /**
   * Sets the charge of this ion
   */
  void setCharge(Integer charge);

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

  FeatureInformation getFeatureInformation();

  void setFeatureInformation(FeatureInformation featureInfo);
  // End dulab Edit

  @NotNull FeatureList getFeatureList();

  int getNumberOfDataPoints();

  /**
   * The detected data points of this feature/chromatogram
   */
  default IonTimeSeries<? extends Scan> getFeatureData() {
    throw new UnsupportedOperationException(
        "Get feature data is not implemented for this sub class. Use ModularFeature or implement");
  }

  /**
   * The FeatureListRow that contains this feature
   *
   * @return a feature list row or null if not assigned to a row
   */
  @Nullable FeatureListRow getRow();

  /**
   * Set the parent row
   *
   * @param row parent row
   */
  void setRow(@Nullable FeatureListRow row);

  /**
   * @return The polarity of the scan obtained by {@link Feature#getRepresentativeScan()}
   */
  @Nullable
  default PolarityType getRepresentativePolarity() {
    final Scan representativeScan = getRepresentativeScan();
    return representativeScan == null ? null : representativeScan.getPolarity();
  }

  default boolean hasMs2Fragmentation() {
    return !getAllMS2FragmentScans().isEmpty();
  }

  boolean isMrm();
}
