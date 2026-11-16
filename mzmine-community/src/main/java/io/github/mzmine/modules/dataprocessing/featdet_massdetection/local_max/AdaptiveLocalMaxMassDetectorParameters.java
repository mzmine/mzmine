/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Parameters of the {@link AdaptiveLocalMaxMassDetector}: the local maximum parameters and the
 * detector saturation threshold.
 */
public class AdaptiveLocalMaxMassDetectorParameters extends SimpleParameterSet {

  public static final DoubleParameter noiseLevel = LocalMaxMassDetectorParameters.noiseLevel.cloneParameter();
  public static final IntegerParameter minNumberOfDp = LocalMaxMassDetectorParameters.minNumberOfDp.cloneParameter();
  public static final AbundanceMeasureParameter intensityCalculation = LocalMaxMassDetectorParameters.intensityCalculation.cloneParameter();
  public static final ModuleOptionsEnumComboParameter<LocalMaxSmoothingOptions> smoothing = LocalMaxMassDetectorParameters.smoothing.cloneParameter();
  public static final SaturationThresholdParameter saturationThreshold = new SaturationThresholdParameter();

  public AdaptiveLocalMaxMassDetectorParameters() {
    this(saturationThreshold);
  }

  /**
   * @param saturation the saturation threshold parameter, may offer fewer options, see
   *                   {@link AdaptiveLocalMaxImportParameters}
   */
  protected AdaptiveLocalMaxMassDetectorParameters(
      @NotNull final SaturationThresholdParameter saturation) {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_mass_detection/adaptive-local-max.html",
        noiseLevel, minNumberOfDp, intensityCalculation, smoothing, saturation);
  }

  @Override
  public int getVersion() {
    // same version as the local maximum parameters this set was split from
    return 2;
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams,
      final int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);
    // smoothing was added without a version bump - disable it for configs that did not store it
    if (!loadedParams.containsKey(smoothing.getName())) {
      setParameter(smoothing, LocalMaxSmoothingOptions.NONE);
    }
  }
}
