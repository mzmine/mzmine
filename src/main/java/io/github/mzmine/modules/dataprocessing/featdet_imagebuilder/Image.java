/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */


package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import java.util.List;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class Image implements IImage {

  private double mz;
  private ImagingParameters imagingParameters;
  private PaintScale paintScale;
  private double maximumIntensity;
  private Range<Double> mzRange;
  private Range<Double> intensityRange;
  private List<ImageDataPoint> dataPoints;
  private List<Scan> scanNumbers;
  private Scan representativeScan;
  private String representativeString;
  private FeatureList featureList;

  public Image(double mz, ImagingParameters imagingParameters, PaintScale paintScale,
      double maximumIntensity, Range<Double> mzRange) {
    this.mz = mz;
    this.imagingParameters = imagingParameters;
    this.paintScale = paintScale;
    this.maximumIntensity = maximumIntensity;
    this.mzRange = mzRange;
  }

  public Image(double mz, ImagingParameters imagingParameters, PaintScale paintScale,
      double maximumIntensity, Range<Double> mzRange, Range<Double> intensityRange,
      List<ImageDataPoint> dataPoints, Scan representativeScan, List<Scan> scanNumbers,
      String representativeString, FeatureList featureList) {
    this.mz = mz;
    this.imagingParameters = imagingParameters;
    this.paintScale = paintScale;
    this.maximumIntensity = maximumIntensity;
    this.mzRange = mzRange;
    this.intensityRange = intensityRange;
    this.dataPoints = dataPoints;
    this.scanNumbers = scanNumbers;
    this.representativeScan = representativeScan;
    this.representativeString = representativeString;
    this.featureList = featureList;
  }

  public double getMz() {
    return mz;
  }

  public void setMz(double mz) {
    this.mz = mz;
  }

  public ImagingParameters getImagingParameters() {
    return imagingParameters;
  }

  public void setImagingParameters(ImagingParameters imagingParameters) {
    this.imagingParameters = imagingParameters;
  }

  public PaintScale getPaintScale() {
    return paintScale;
  }

  public void setPaintScale(PaintScale paintScale) {
    this.paintScale = paintScale;
  }

  public double getMaximumIntensity() {
    return maximumIntensity;
  }

  public void setMaximumIntensity(double maximumIntensity) {
    this.maximumIntensity = maximumIntensity;
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

  public List<ImageDataPoint> getDataPoints() {
    return dataPoints;
  }

  public void setDataPoints(List<ImageDataPoint> dataPoints) {
    this.dataPoints = dataPoints;
  }

  public List<Scan> getScanNumbers() {
    return scanNumbers;
  }

  public void setScanNumbers(List<Scan> scanNumbers) {
    this.scanNumbers = scanNumbers;
  }

  public Scan getRepresentativeScan() {
    return representativeScan;
  }

  public void setRepresentativeScan(Scan representativeScan) {
    this.representativeScan = representativeScan;
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
    result = prime * result + ((imagingParameters == null) ? 0 : imagingParameters.hashCode());
    result = prime * result + ((intensityRange == null) ? 0 : intensityRange.hashCode());
    long temp;
    temp = Double.doubleToLongBits(maximumIntensity);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mz);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((mzRange == null) ? 0 : mzRange.hashCode());
    result =
        prime * result + ((representativeString == null) ? 0 : representativeString.hashCode());
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
    Image other = (Image) obj;
    if (dataPoints == null) {
      if (other.dataPoints != null)
        return false;
    } else if (!dataPoints.equals(other.dataPoints))
      return false;
    if (imagingParameters == null) {
      if (other.imagingParameters != null)
        return false;
    } else if (!imagingParameters.equals(other.imagingParameters))
      return false;
    if (intensityRange == null) {
      if (other.intensityRange != null)
        return false;
    } else if (!intensityRange.equals(other.intensityRange))
      return false;
    if (Double.doubleToLongBits(maximumIntensity) != Double
        .doubleToLongBits(other.maximumIntensity))
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
    if (scanNumbers == null) {
      if (other.scanNumbers != null)
        return false;
    } else if (!scanNumbers.equals(other.scanNumbers))
      return false;
    return true;
  }

}
