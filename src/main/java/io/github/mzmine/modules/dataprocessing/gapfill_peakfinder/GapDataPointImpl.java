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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;

/**
 * DataPoint implementation extended with retention time and scan number.
 * <p></p>
 * Used during gap filling of regular LC-MS files.
 */
class GapDataPointImpl implements GapDataPoint {

  private Scan scanNumber;
  private double mz, rt, intensity;

  /**
   *
   */
  GapDataPointImpl(Scan scanNumber, double mz, double rt, double intensity) {
    if (scanNumber instanceof ScanDataAccess access) {
      this.scanNumber = access.getCurrentScan();
    } else {
      this.scanNumber = scanNumber;
    }
    this.mz = mz;
    this.rt = rt;
    this.intensity = intensity;
  }

  public Scan getScan() {
    return scanNumber;
  }

  public double getIntensity() {
    return intensity;
  }

  public double getMZ() {
    return mz;
  }

  public double getRT() {
    return rt;
  }

}
