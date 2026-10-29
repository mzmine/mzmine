/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.dataprocessing.norm_intensity.FeatureQcSmoothing.ManualLoess;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * LOESS with fixed bandwidth, polynomial degree and robustness iterations for all features.
 */
public final class ManualLoessSmoothingModule implements FeatureQcSmoothingModule {

  @Override
  public @NotNull FeatureQcSmoothing createSmoothing(@NotNull final ParameterSet parameters) {
    return new ManualLoess(parameters.getValue(ManualLoessSmoothingParameters.bandwidth),
        parameters.getValue(ManualLoessSmoothingParameters.polynomialDegree),
        parameters.getValue(ManualLoessSmoothingParameters.robustnessIterations));
  }

  @Override
  public @NotNull String getName() {
    return FeatureQcSmoothingOptions.MANUAL_LOESS.toString();
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ManualLoessSmoothingParameters.class;
  }
}
