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

package io.github.mzmine.modules.dataprocessing.featdet_gridmass;

import io.github.mzmine.datamodel.DataPoint;

class Datum implements Comparable<Datum> {
  double mz = 0;
  double intensity = 0;
  int spotId = 0;
  int scan = 0;
  boolean available = true;
  boolean neighbours = false;
  boolean included = true;
  double mzOriginal = 0;
  double intensityOriginal = 0;

  Datum(DataPoint dp, int iScan, DataPoint dpOriginal) {
    mz = dp.getMZ();
    intensity = dp.getIntensity();
    scan = iScan;
    mzOriginal = dpOriginal.getMZ();
    intensityOriginal = dpOriginal.getIntensity();
  }

  public int compareTo(Datum other) {
    if (this.intensity > other.intensity)
      return -1;
    if (this.intensity < other.intensity)
      return 1;

    // equal intensities, then sort by lower mz
    if (this.mz < other.mz)
      return -1;
    if (this.mz > other.mz)
      return 1;

    // otherwise they are equal in intensity and mz
    return 0;
  }

  public String toString() {
    return "MZ=" + Math.round(mz * 10000) / 10000 + " | Int=" + intensity + " | Scan=" + scan
        + " | spotId=" + spotId;
  }
}
