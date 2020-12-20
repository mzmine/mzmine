package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;

public class ImageDataPoint implements DataPoint {

  private final double mz;
  private final double intensity;
  private final int scanNumber;
  private final double dataPointWidth;
  private final double dataPointHeigth;
  private final Coordinates coordinates;

  public ImageDataPoint(double mz, double intensity, int scanNumber, Coordinates coordinates,
      double dataPointWidth, double dataPointHeigth) {
    this.mz = mz;
    this.intensity = intensity;
    this.scanNumber = scanNumber;
    this.coordinates = coordinates;
    this.dataPointWidth = dataPointWidth;
    this.dataPointHeigth = dataPointHeigth;
  }

  public double getMZ() {
    return mz;
  }

  public double getIntensity() {
    return intensity;
  }

  public int getScanNumber() {
    return scanNumber;
  }

  public double getDataPointWidth() {
    return dataPointWidth;
  }

  public double getDataPointHeigth() {
    return dataPointHeigth;
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
    long temp;
    temp = Double.doubleToLongBits(dataPointHeigth);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(dataPointWidth);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(intensity);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mz);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + scanNumber;
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
    ImageDataPoint other = (ImageDataPoint) obj;
    if (coordinates == null) {
      if (other.coordinates != null)
        return false;
    } else if (!coordinates.equals(other.coordinates))
      return false;
    if (Double.doubleToLongBits(dataPointHeigth) != Double.doubleToLongBits(other.dataPointHeigth))
      return false;
    if (Double.doubleToLongBits(dataPointWidth) != Double.doubleToLongBits(other.dataPointWidth))
      return false;
    if (Double.doubleToLongBits(intensity) != Double.doubleToLongBits(other.intensity))
      return false;
    if (Double.doubleToLongBits(mz) != Double.doubleToLongBits(other.mz))
      return false;
    if (scanNumber != other.scanNumber)
      return false;
    return true;
  }

}
