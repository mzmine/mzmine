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
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */
package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageDataPoint implements DataPoint {

  private final double mz;
  private final double intensity;
  private final ImagingScan scanNumber;
  private final double xWorld;
  private final double yWorld;
  private final double zWorld;
  private final double dataPointHeigth;
  private final double dataPointWidth;
  private final PaintScale paintScale;

  public ImageDataPoint(double mz, double intensity, ImagingScan scanNumber, double xWorld, double yWorld,
      double zWorld, double dataPointHeigth, double dataPointWidth, PaintScale paintScale) {
    super();
    this.mz = mz;
    this.intensity = intensity;
    this.scanNumber = scanNumber;
    this.xWorld = xWorld;
    this.yWorld = yWorld;
    this.zWorld = zWorld;
    this.dataPointHeigth = dataPointHeigth;
    this.dataPointWidth = dataPointWidth;
    this.paintScale = paintScale;
  }

  public double getMZ() {
    return mz;
  }

  public double getIntensity() {
    return intensity;
  }

  public ImagingScan getScanNumber() {
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

  public PaintScale getPaintScale() {
    return paintScale;
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
    result = prime * result + scanNumber.getScanNumber();
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