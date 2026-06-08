/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalMaxMassDetectorParameters extends SimpleParameterSet {

  public static final DoubleParameter noiseLevel = CentroidMassDetectorParameters.noiseLevel.cloneParameter();
  public static final IntegerParameter minNumberOfDp = new IntegerParameter(
      "Minimum non-zero points", "Minimum number of data points >0 intensity", 3);
  public static final AbundanceMeasureParameter intensityCalculation = new AbundanceMeasureParameter(
      "Intensity calculation", "", AbundanceMeasure.values(), AbundanceMeasure.Height);

  public LocalMaxMassDetectorParameters() {
    super(noiseLevel, minNumberOfDp, intensityCalculation);
  }

  public static LocalMaxMassDetectorParameters create(final double noiseLevel,
      @NotNull final AbundanceMeasure intensityCalculation) {
    ParameterSet param = new LocalMaxMassDetectorParameters().cloneParameterSet();
    param.setParameter(LocalMaxMassDetectorParameters.noiseLevel, noiseLevel);
    param.setParameter(LocalMaxMassDetectorParameters.intensityCalculation, intensityCalculation);
    return (LocalMaxMassDetectorParameters) param;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 ->
          "The algorithm of the local maximum mass detector was updated. It no longer uses the "
              + "highest data point but a weighted average. The centroid intensity calculation "
              + "can be configured to sum or use the highest value.";
      default -> null;
    };
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);

    if (loadedVersion < 2) {
      setParameter(intensityCalculation, AbundanceMeasure.Height);
      setParameter(minNumberOfDp, 3);
    }
  }
}
