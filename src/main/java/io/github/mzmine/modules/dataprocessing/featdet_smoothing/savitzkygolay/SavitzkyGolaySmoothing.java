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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SavitzkyGolaySmoothing implements SmoothingAlgorithm {

  //  private final ZeroHandlingType zht;
  private final double[] mobilityWeights;
  private final double[] rtWeights;
  private final boolean smoothRt;
  private final boolean smoothMobility;

  public SavitzkyGolaySmoothing(ParameterSet parameters) {
//    this.zht = zeroHandlingType;
    this.mobilityWeights = SavitzkyGolayFilter.getNormalizedWeights(
        parameters.getParameter(SavitzkyGolayParameters.mobilitySmoothing).getEmbeddedParameter()
            .getValue());
    this.rtWeights = SavitzkyGolayFilter.getNormalizedWeights(
        parameters.getParameter(SavitzkyGolayParameters.rtSmoothing).getEmbeddedParameter()
            .getValue());

    smoothRt = parameters.getParameter(SavitzkyGolayParameters.rtSmoothing).getValue();
    smoothMobility = parameters.getParameter(SavitzkyGolayParameters.mobilitySmoothing).getValue();
  }

  /**
   * Initialises this module with null parameters. Not to be used if the instance shall be used for
   * smoothing.
   */
  public SavitzkyGolaySmoothing() {
    mobilityWeights = null;
    rtWeights = null;
    smoothRt = true;
    smoothMobility = true;
  }

  /**
   * @param access The intensity series to be smoothed. Ideally an instance of {@link
   *               io.github.mzmine.datamodel.data_access.EfficientDataAccess} for best
   *               performance.
   * @return
   */
  public double[] smooth(@NotNull final IntensitySeries access, double[] normWeights) {
    if (normWeights == null) {
      throw new IllegalArgumentException(
          "No smoothing weights specified. Was the smoother initialised correctly?");
    }

    // Initialise.
    final int numPoints = access.getNumberOfValues();
    final int fullWidth = normWeights.length;
    final int halfWidth = (fullWidth - 1) / 2;

    double[] smoothed = new double[numPoints];
    for (int i = 0; i < numPoints; i++) {
      final int k = i - halfWidth;
      for (int j = Math.max(0, -k); j < Math.min(fullWidth, numPoints - k); j++) {
        smoothed[i] += access.getIntensity(k + j) * normWeights[j];
      }

      if (smoothed[i] < 0d) {
        smoothed[i] = 0d;
      }

//      if (/*zht == ZeroHandlingType.KEEP &&*/ Double.compare(access.getIntensity(i), 0d) == 0) {
      // if values that were previously 0 shall remain 0, we process that here.
      if (Double.compare(access.getIntensity(i), 0d) == 0) {
        smoothed[i] = 0;
      }
    }

    return smoothed;
  }

  @Override
  @Nullable
  public <T extends Scan> double[] smoothRt(@NotNull IonTimeSeries<T> series) {
    return smoothRt ? smooth(series, rtWeights) : null;
  }

  @Override
  @Nullable
  public <T extends IntensitySeries & MobilitySeries> double[] smoothMobility(
      @NotNull T mobilogram) {
    return smoothMobility ? smooth(mobilogram, mobilityWeights) : null;
  }

  @Override
  public @NotNull String getName() {
    return "Savitzky Golay";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return SavitzkyGolayParameters.class;
  }
}
