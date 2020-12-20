package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import java.util.Set;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;

public class Image implements IImage {

  private double mz;
  private ImagingParameters imagingParameters;
  private Coordinates mostIntensCoordinate;
  private double maximumIntensity;
  private Range<Double> mzRange;
  private Range<Double> intensityRange;
  private Set<ImageDataPoint> dataPoints;
  private Set<Integer> scanNumbers;
  private String representativeString;
  private FeatureList featureList;

  public Image(double mz, ImagingParameters imagingParameters, Coordinates mostIntensCoordinate,
      double maximumIntensity, Range<Double> mzRange) {
    this.mz = mz;
    this.imagingParameters = imagingParameters;
    this.mostIntensCoordinate = mostIntensCoordinate;
    this.maximumIntensity = maximumIntensity;
    this.mzRange = mzRange;
  }

  public Image(double mz, ImagingParameters imagingParameters, Coordinates mostIntensCoordinate,
      double maximumIntensity, Range<Double> mzRange, Range<Double> intensityRange,
      Set<ImageDataPoint> dataPoints, Set<Integer> scanNumbers, String representativeString,
      FeatureList featureList) {
    this.mz = mz;
    this.imagingParameters = imagingParameters;
    this.mostIntensCoordinate = mostIntensCoordinate;
    this.maximumIntensity = maximumIntensity;
    this.mzRange = mzRange;
    this.intensityRange = intensityRange;
    this.dataPoints = dataPoints;
    this.scanNumbers = scanNumbers;
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

  public Coordinates getMostIntensCoordinate() {
    return mostIntensCoordinate;
  }

  public void setMostIntensCoordinate(Coordinates mostIntensCoordinate) {
    this.mostIntensCoordinate = mostIntensCoordinate;
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

  public Set<ImageDataPoint> getDataPoints() {
    return dataPoints;
  }

  public void setDataPoints(Set<ImageDataPoint> dataPoints) {
    this.dataPoints = dataPoints;
  }

  public Set<Integer> getScanNumbers() {
    return scanNumbers;
  }

  public void setScanNumbers(Set<Integer> scanNumbers) {
    this.scanNumbers = scanNumbers;
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
    result =
        prime * result + ((mostIntensCoordinate == null) ? 0 : mostIntensCoordinate.hashCode());
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
    if (mostIntensCoordinate == null) {
      if (other.mostIntensCoordinate != null)
        return false;
    } else if (!mostIntensCoordinate.equals(other.mostIntensCoordinate))
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
