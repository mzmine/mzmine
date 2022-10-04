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
