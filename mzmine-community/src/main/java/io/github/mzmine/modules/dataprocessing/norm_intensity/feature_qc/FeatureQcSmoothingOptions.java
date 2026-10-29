/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity.feature_qc;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import org.jetbrains.annotations.NotNull;

public enum FeatureQcSmoothingOptions implements ModuleOptionsEnum<FeatureQcSmoothingModule> {

  QC_RLSC, MANUAL_LOESS;

  /**
   * @param parameters the embedded parameters of this option
   */
  public @NotNull FeatureQcSmoothing createSmoothing(@NotNull final ParameterSet parameters) {
    return getModuleInstance().createSmoothing(parameters);
  }

  @Override
  public Class<? extends FeatureQcSmoothingModule> getModuleClass() {
    return switch (this) {
      case QC_RLSC -> QcRlscSmoothingModule.class;
      case MANUAL_LOESS -> ManualLoessSmoothingModule.class;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case QC_RLSC -> "QC-RLSC";
      case MANUAL_LOESS -> "Manual LOESS";
    };
  }

  @Override
  public String getStableId() {
    // do not change these values for save/load
    return switch (this) {
      case QC_RLSC -> "qc_rlsc";
      case MANUAL_LOESS -> "manual_loess";
    };
  }
}
