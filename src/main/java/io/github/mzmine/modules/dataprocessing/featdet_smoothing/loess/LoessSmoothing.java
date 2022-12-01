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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.loess;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.FeatureFullDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.IonMobilityUtils;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoessSmoothing implements SmoothingAlgorithm {

  private final Integer mobilityWidth;
  private final Integer rtWidth;
  private final boolean smoothRt;
  private final boolean smoothMobility;

  public LoessSmoothing() {
    this.mobilityWidth = null;
    this.rtWidth = null;
    this.smoothRt = true;
    this.smoothMobility = true;
  }

  public  LoessSmoothing(boolean smoothRt, int rtWidth, boolean smoothMobility, int mobilityWidth) {
    this.smoothRt = smoothRt;
    this.rtWidth = rtWidth;
    this.smoothMobility = smoothMobility;
    this.mobilityWidth = mobilityWidth;
  }

  public LoessSmoothing(ParameterSet parameters) {
    this.mobilityWidth = parameters.getParameter(LoessSmoothingParameters.mobilitySmoothing)
        .getEmbeddedParameter().getValue();
    this.rtWidth = parameters.getParameter(LoessSmoothingParameters.rtSmoothing)
        .getEmbeddedParameter().getValue();

    smoothRt = parameters.getParameter(LoessSmoothingParameters.rtSmoothing).getValue();
    smoothMobility = parameters.getParameter(LoessSmoothingParameters.mobilitySmoothing).getValue();
  }

  @Override
  public @NotNull String getName() {
    return "Loess smoothing";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LoessSmoothingParameters.class;
  }

  @Override
  public <T extends Scan> @Nullable double[] smoothRt(@NotNull IonTimeSeries<T> series) {
    if (!smoothRt) {
      return null;
    }

    // bandwidth: fraction of source points, cannot be greater than 1
    final double rtBandwidth = Math.min((((double) this.rtWidth) / series.getNumberOfValues()), 1);
    final LoessInterpolator interpolator = new LoessInterpolator(rtBandwidth, 0);

    double[] intensities;
    if(series instanceof FeatureFullDataAccess access) {
      intensities = access.getIntensityValues();
    } else {
      intensities = new double[series.getNumberOfValues()];
      intensities = series.getIntensityValues(intensities);
    }
    double[] rts = new double[series.getNumberOfValues()];
    for (int i = 0; i < rts.length; i++) {
      rts[i] = series.getRetentionTime(i);
    }

    assert intensities.length == rts.length;

    double[] smoothed = interpolator.smooth(rts, intensities);
    for (int i = 0; i < intensities.length; i++) {
      if (Double.compare(intensities[i], 0d) <= 0) {
        smoothed[i] = 0d;
      }
    }
    return smoothed;
  }

  @Override
  @Nullable
  public <T extends IntensitySeries & MobilitySeries> double[] smoothMobility(
      @NotNull T mobilogram) {
    if (!smoothMobility) {
      return null;
    }

    // bandwidth: fraction of source points, cannot be greater than 1
    final double mobilityBandwidth = Math.min((((double) this.mobilityWidth) / mobilogram
        .getNumberOfValues()), 1);
    final LoessInterpolator interpolator = new LoessInterpolator(mobilityBandwidth, 0);

    double[] intensities = new double[mobilogram.getNumberOfValues()];
    mobilogram.getIntensityValues(intensities);
    double[] mobilities = IonMobilityUtils.extractMobilities(mobilogram);

    double[] smoothed = interpolator.smooth(mobilities, intensities);
    for (int i = 0; i < intensities.length; i++) {
      if (Double.compare(intensities[i], 0d) <= 0) {
        smoothed[i] = 0d;
      }
    }
    return smoothed;
  }


}
