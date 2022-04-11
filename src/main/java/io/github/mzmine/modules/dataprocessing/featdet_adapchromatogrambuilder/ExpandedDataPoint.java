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



/*
 * Created by Owen Myers (Oweenm@gmail.com)
 */


package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;


/**
 * DataPoint implementation extended with scan number
 */
public class ExpandedDataPoint implements DataPoint {

  private Scan scan = null;
  private double mz, intensity;

  /**
   */
  public ExpandedDataPoint(double mz, double intensity, Scan scan) {

    this.scan = scan;
    this.mz = mz;
    this.intensity = intensity;

  }

  /**
   * Constructor which copies the data from another DataPoint
   */
  public ExpandedDataPoint(DataPoint dp) {
    this.mz = dp.getMZ();
    this.intensity = dp.getIntensity();
  }

  /**
   * Constructor which copies the data from another DataPoint and takes the scan number
   */
  public ExpandedDataPoint(DataPoint dp, Scan scanNumIn) {
    this.mz = dp.getMZ();
    this.intensity = dp.getIntensity();
    this.scan = scanNumIn;
  }

  public ExpandedDataPoint() {
    this.mz = 0.0;
    this.intensity = 0.0;
    this.scan = null;
  }

  @Override
  public double getIntensity() {
    return intensity;
  }

  @Override
  public double getMZ() {
    return mz;
  }

  public Scan getScan() {
    return scan;
  }
}
