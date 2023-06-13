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

package io.github.mzmine.modules.io.import_rawdata_mzml.spectral_processor;

import io.github.mzmine.datamodel.Scan;
import java.util.Arrays;

public class CropMzMsProcessor implements MsProcessor {


  private final double min;
  private final double max;

  public CropMzMsProcessor(double min, double max) {
    this.min = min;
    this.max = max;
  }

  public SimpleSpectralArrays processScan(final Scan scan, final SimpleSpectralArrays spectrum) {
    // returns the index of the value>=min/max
    // requires sorted values, is ensured as this is always the first step in loading/processing
    int lower = Math.abs(Arrays.binarySearch(spectrum.mzs(), min));
    int upper = Math.abs(Arrays.binarySearch(spectrum.mzs(), max));
    var mzs = Arrays.copyOfRange(spectrum.mzs(), lower, upper);
    var intensities = Arrays.copyOfRange(spectrum.intensities(), lower, upper);
    return new SimpleSpectralArrays(mzs, intensities);
  }
}
