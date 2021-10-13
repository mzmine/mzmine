/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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

    final double rtBandwidth = (((double) this.rtWidth) / series.getNumberOfValues());
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

    final double mobilityBandwidth = (((double) this.mobilityWidth) / mobilogram
        .getNumberOfValues());
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
