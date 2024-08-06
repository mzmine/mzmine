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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters;

import io.github.mzmine.modules.dataprocessing.filter_scanfilters.mean.MeanFilter;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.resample.ResampleFilter;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.roundresample.RndResampleFilter;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.savitzkygolay.SGFilter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;

public enum ScanFilters implements ModuleOptionsEnum<ScanFilter> {

  SAVITZKY_GOLAY, MEAN_FILTER, RESAMPLE, ROUND_RESAMPLE;

  public static ScanFilter createFilter(final ValueWithParameters<ScanFilters> param) {
    return switch (param.value()) {
      case SAVITZKY_GOLAY -> new SGFilter(param.parameters());
      case MEAN_FILTER -> new MeanFilter(param.parameters());
      case RESAMPLE -> new ResampleFilter(param.parameters());
      case ROUND_RESAMPLE -> new RndResampleFilter(param.parameters());
    };
  }

  @Override
  public String toString() {
    return getStableId();
  }

  @Override
  public Class<? extends ScanFilter> getModuleClass() {
    return switch (this) {
      case SAVITZKY_GOLAY -> SGFilter.class;
      case MEAN_FILTER -> MeanFilter.class;
      case RESAMPLE -> ResampleFilter.class;
      case ROUND_RESAMPLE -> RndResampleFilter.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case SAVITZKY_GOLAY -> "Savitzky-Golay filter";
      case MEAN_FILTER -> "Mean filter";
      case RESAMPLE -> "Resampling filter";
      case ROUND_RESAMPLE -> "Round resampling filter";
    };
  }
}
