/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_all.spectral_processor;

import io.github.mzmine.datamodel.Scan;
import java.util.Arrays;

/**
 * Data structure to represent spectral data in memory
 *
 * @param mzs should be sorted by mz ascending
 */
public record SimpleSpectralArrays(double[] mzs, double[] intensities) {

  public static final SimpleSpectralArrays EMPTY = new SimpleSpectralArrays(new double[0],
      new double[0]);

  public SimpleSpectralArrays(final Scan scan) {
    this(scan.getMzValues(new double[scan.getNumberOfDataPoints()]),
        scan.getIntensityValues(new double[scan.getNumberOfDataPoints()]));
  }

  public int getNumberOfDataPoints() {
    return mzs().length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleSpectralArrays that)) {
      return false;
    }

    return Arrays.equals(mzs, that.mzs) && Arrays.equals(intensities, that.intensities);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(mzs);
    result = 31 * result + Arrays.hashCode(intensities);
    return result;
  }
}
