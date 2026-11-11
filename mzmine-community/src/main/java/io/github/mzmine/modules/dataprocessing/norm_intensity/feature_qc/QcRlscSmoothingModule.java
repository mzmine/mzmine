/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity.feature_qc;

import io.github.mzmine.modules.dataprocessing.norm_intensity.feature_qc.FeatureQcSmoothing.QcRlsc;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Automatic QC-RLSC smoothing (Dunn et al. 2011), selects span and degree by leave-one-out
 * cross-validation. No parameters.
 */
public final class QcRlscSmoothingModule implements FeatureQcSmoothingModule {

  @Override
  public @NotNull FeatureQcSmoothing createSmoothing(@NotNull final ParameterSet parameters) {
    return QcRlsc.DEFAULT;
  }

  @Override
  public @NotNull String getName() {
    return FeatureQcSmoothingOptions.QC_RLSC.toString();
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return QcRlscSmoothingParameters.class;
  }
}
