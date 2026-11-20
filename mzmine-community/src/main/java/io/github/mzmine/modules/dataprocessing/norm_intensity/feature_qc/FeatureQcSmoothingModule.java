/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity.feature_qc;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Smoothing option of the feature specific QC drift correction, see
 * {@link FeatureQcSmoothingOptions}.
 */
public interface FeatureQcSmoothingModule extends MZmineModule {

  /**
   * @param parameters the embedded parameters of this option
   */
  @NotNull FeatureQcSmoothing createSmoothing(@NotNull ParameterSet parameters);
}
