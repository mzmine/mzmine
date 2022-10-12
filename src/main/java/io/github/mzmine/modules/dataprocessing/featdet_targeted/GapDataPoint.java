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

package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;

/**
 * DataPoint implementation extended with retention time and scan number
 */
class GapDataPoint implements DataPoint {

  private Scan scan;
  private double mz, rt, intensity;

  /**
   */
  GapDataPoint(Scan scan, double mz, double rt, double intensity) {
    this.scan = scan;
    this.mz = mz;
    this.rt = rt;
    this.intensity = intensity;

  }

  Scan getScan() {
    return scan;
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
