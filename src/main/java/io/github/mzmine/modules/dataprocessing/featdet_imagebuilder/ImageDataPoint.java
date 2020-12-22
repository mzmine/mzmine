package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import io.github.mzmine.datamodel.DataPoint;

public class ImageDataPoint implements DataPoint {

  private final double mz;
  private final double intensity;
  private final int scanNumber;
  private final double xWorld;
  private final double yWorld;
  private final double zWorld;
  private final double dataPointHeigth;
  private final double dataPointWidth;

  public ImageDataPoint(double mz, double intensity, int scanNumber, double xWorld, double yWorld,
      double zWorld, double dataPointHeigth, double dataPointWidth) {
    this.mz = mz;
    this.intensity = intensity;
    this.scanNumber = scanNumber;
    this.xWorld = xWorld;
    this.yWorld = yWorld;
    this.zWorld = zWorld;
    this.dataPointHeigth = dataPointHeigth;
    this.dataPointWidth = dataPointWidth;
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

  public double getxWorld() {
    return xWorld;
  }

  public double getyWorld() {
    return yWorld;
  }

  public double getzWorld() {
    return zWorld;
  }

  public double getDataPointHeigth() {
    return dataPointHeigth;
  }

  public double getDataPointWidth() {
    return dataPointWidth;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
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
    temp = Double.doubleToLongBits(xWorld);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(yWorld);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(zWorld);
    result = prime * result + (int) (temp ^ (temp >>> 32));
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
    if (Double.doubleToLongBits(xWorld) != Double.doubleToLongBits(other.xWorld))
      return false;
    if (Double.doubleToLongBits(yWorld) != Double.doubleToLongBits(other.yWorld))
      return false;
    if (Double.doubleToLongBits(zWorld) != Double.doubleToLongBits(other.zWorld))
      return false;
    return true;
  }

}
