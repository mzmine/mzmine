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

package io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CropMzMsProcessor implements MsProcessor {


  private final double min;
  private final double max;

  public CropMzMsProcessor(double min, double max) {
    this.min = min;
    this.max = max;
  }

  public @NotNull SimpleSpectralArrays processScan(final @Nullable Scan metadataOnlyScan,
      final @NotNull SimpleSpectralArrays spectrum) {
    // only crop MS1 scans
    if (metadataOnlyScan != null && metadataOnlyScan.getMSLevel() != 1) {
      return spectrum;
    }

    IndexRange range = BinarySearch.indexRange(spectrum.mzs(), min, max);
    if (range.size() == spectrum.getNumberOfDataPoints()) {
      return spectrum;
    }
    // filter
    double[] mzs = range.subarray(spectrum.mzs());
    double[] intensities = range.subarray(spectrum.intensities());
    return new SimpleSpectralArrays(mzs, intensities);
  }

  @Override
  public @NotNull String description() {
    var format = MZmineCore.getConfiguration().getGuiFormats();
    return "Crop m/z to " + format.mz(min) + " - " + format.mz(max);
  }
}
