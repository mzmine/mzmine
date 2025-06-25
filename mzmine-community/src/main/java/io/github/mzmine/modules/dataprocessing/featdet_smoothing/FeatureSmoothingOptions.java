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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.loess.LoessSmoothing;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolaySmoothing;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;

public enum FeatureSmoothingOptions implements ModuleOptionsEnum {
  LOESS, SAVITZKY_GOLAY;

  public static SmoothingAlgorithm createSmoother(final ParameterSet parameters) {
    var algorithm = parameters.getValue(SmoothingParameters.smoothingAlgorithm);
    var algoParams = parameters.getEmbeddedParameterValue(SmoothingParameters.smoothingAlgorithm);
    return switch (algorithm) {
      case SAVITZKY_GOLAY -> new SavitzkyGolaySmoothing(algoParams);
      case LOESS -> new LoessSmoothing(algoParams);
    };
  }

  @Override
  public String toString() {
    return getStableId();
  }

  @Override
  public Class<? extends MZmineModule> getModuleClass() {
    return switch (this) {
      case LOESS -> LoessSmoothing.class;
      case SAVITZKY_GOLAY -> SavitzkyGolaySmoothing.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case LOESS -> "Loess smoothing";
      case SAVITZKY_GOLAY -> "Savitzky Golay";
    };
  }
}
