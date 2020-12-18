/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import java.util.Set;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.FeatureList;

public class IonMobilityTrace implements IIonMobilityTrace {

  private double mz;
  private Float retentionTime;
  private double mobility;
  private double maximumIntensity;
  private Range<Float> retentionTimeRange;
  private Range<Double> mobilityRange;
  private Range<Double> mzRange;
  private Range<Double> intensityRange;
  private Set<RetentionTimeMobilityDataPoint> dataPoints;
  private Set<Integer> frameNumbers;
  private Set<Integer> scanNumbers;
  private MobilityType mobilityType;
  private String representativeString;
  private FeatureList featureList;

  public IonMobilityTrace(double mz, Float retentionTime, double mobility, double maximumIntensity,
      Range<Double> mzRange) {
    this.mz = mz;
    this.retentionTime = retentionTime;
    this.mobility = mobility;
    this.maximumIntensity = maximumIntensity;
    this.mzRange = mzRange;
  }

  public IonMobilityTrace(double mz, Float retentionTime, double mobility, double maximumIntensity,
      Range<Float> retentionTimeRange, Range<Double> mobilityRange, Range<Double> mzRange,
      Range<Double> intensityRange, Set<RetentionTimeMobilityDataPoint> dataPoints,
      Set<Integer> frameNumbers, Set<Integer> scanNumbers, MobilityType mobilityType,
      String representativeString, FeatureList featureList) {
    this.mz = mz;
    this.retentionTime = retentionTime;
    this.mobility = mobility;
    this.maximumIntensity = maximumIntensity;
    this.retentionTimeRange = retentionTimeRange;
    this.mobilityRange = mobilityRange;
    this.mzRange = mzRange;
    this.intensityRange = intensityRange;
    this.dataPoints = dataPoints;
    this.frameNumbers = frameNumbers;
    this.scanNumbers = scanNumbers;
    this.mobilityType = mobilityType;
    this.representativeString = representativeString;
    this.featureList = featureList;
  }

  public double getMz() {
    return mz;
  }

  public void setMz(double mz) {
    this.mz = mz;
  }

  public Float getRetentionTime() {
    return retentionTime;
  }

  public void setRetentionTime(Float retentionTime) {
    this.retentionTime = retentionTime;
  }

  public double getMobility() {
    return mobility;
  }

  public void setMobility(double mobility) {
    this.mobility = mobility;
  }

  public double getMaximumIntensity() {
    return maximumIntensity;
  }

  public void setMaximumIntensity(double maximumIntensity) {
    this.maximumIntensity = maximumIntensity;
  }

  public Range<Float> getRetentionTimeRange() {
    return retentionTimeRange;
  }

  public void setRetentionTimeRange(Range<Float> retentionTimeRange) {
    this.retentionTimeRange = retentionTimeRange;
  }

  public Range<Double> getMobilityRange() {
    return mobilityRange;
  }

  public void setMobilityRange(Range<Double> mobilityRange) {
    this.mobilityRange = mobilityRange;
  }

  public Range<Double> getMzRange() {
    return mzRange;
  }

  public void setMzRange(Range<Double> mzRange) {
    this.mzRange = mzRange;
  }

  public Range<Double> getIntensityRange() {
    return intensityRange;
  }

  public void setIntensityRange(Range<Double> intensityRange) {
    this.intensityRange = intensityRange;
  }

  public Set<RetentionTimeMobilityDataPoint> getDataPoints() {
    return dataPoints;
  }

  public void setDataPoints(Set<RetentionTimeMobilityDataPoint> dataPoints) {
    this.dataPoints = dataPoints;
  }

  public Set<Integer> getFrameNumbers() {
    return frameNumbers;
  }

  public void setFrameNumbers(Set<Integer> frameNumbers) {
    this.frameNumbers = frameNumbers;
  }

  public Set<Integer> getScanNumbers() {
    return scanNumbers;
  }

  public void setScanNumbers(Set<Integer> scanNumbers) {
    this.scanNumbers = scanNumbers;
  }

  public MobilityType getMobilityType() {
    return mobilityType;
  }

  public void setMobilityType(MobilityType mobilityType) {
    this.mobilityType = mobilityType;
  }

  public String getRepresentativeString() {
    return representativeString;
  }

  public void setRepresentativeString(String representativeString) {
    this.representativeString = representativeString;
  }

  public FeatureList getFeatureList() {
    return featureList;
  }

  public void setFeatureList(FeatureList featureList) {
    this.featureList = featureList;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dataPoints == null) ? 0 : dataPoints.hashCode());
    result = prime * result + ((frameNumbers == null) ? 0 : frameNumbers.hashCode());
    result = prime * result + ((intensityRange == null) ? 0 : intensityRange.hashCode());
    long temp;
    temp = Double.doubleToLongBits(maximumIntensity);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mobility);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((mobilityRange == null) ? 0 : mobilityRange.hashCode());
    result = prime * result + ((mobilityType == null) ? 0 : mobilityType.hashCode());
    temp = Double.doubleToLongBits(mz);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((mzRange == null) ? 0 : mzRange.hashCode());
    result =
        prime * result + ((representativeString == null) ? 0 : representativeString.hashCode());
    result = prime * result + ((retentionTime == null) ? 0 : retentionTime.hashCode());
    result = prime * result + ((retentionTimeRange == null) ? 0 : retentionTimeRange.hashCode());
    result = prime * result + ((scanNumbers == null) ? 0 : scanNumbers.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IonMobilityTrace other = (IonMobilityTrace) obj;
    if (dataPoints == null) {
      if (other.dataPoints != null)
        return false;
    } else if (!dataPoints.equals(other.dataPoints))
      return false;
    if (frameNumbers == null) {
      if (other.frameNumbers != null)
        return false;
    } else if (!frameNumbers.equals(other.frameNumbers))
      return false;
    if (intensityRange == null) {
      if (other.intensityRange != null)
        return false;
    } else if (!intensityRange.equals(other.intensityRange))
      return false;
    if (Double.doubleToLongBits(maximumIntensity) != Double
        .doubleToLongBits(other.maximumIntensity))
      return false;
    if (Double.doubleToLongBits(mobility) != Double.doubleToLongBits(other.mobility))
      return false;
    if (mobilityRange == null) {
      if (other.mobilityRange != null)
        return false;
    } else if (!mobilityRange.equals(other.mobilityRange))
      return false;
    if (mobilityType != other.mobilityType)
      return false;
    if (Double.doubleToLongBits(mz) != Double.doubleToLongBits(other.mz))
      return false;
    if (mzRange == null) {
      if (other.mzRange != null)
        return false;
    } else if (!mzRange.equals(other.mzRange))
      return false;
    if (representativeString == null) {
      if (other.representativeString != null)
        return false;
    } else if (!representativeString.equals(other.representativeString))
      return false;
    if (retentionTime == null) {
      if (other.retentionTime != null)
        return false;
    } else if (!retentionTime.equals(other.retentionTime))
      return false;
    if (retentionTimeRange == null) {
      if (other.retentionTimeRange != null)
        return false;
    } else if (!retentionTimeRange.equals(other.retentionTimeRange))
      return false;
    if (scanNumbers == null) {
      if (other.scanNumbers != null)
        return false;
    } else if (!scanNumbers.equals(other.scanNumbers))
      return false;
    return true;
  }

}
