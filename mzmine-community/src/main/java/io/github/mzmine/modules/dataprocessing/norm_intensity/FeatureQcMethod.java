package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.NotNull;

/** Scientific method, independent of the diagnostics serialization format. */
public enum FeatureQcMethod {
  DUNN_QC_RLSC("QC-RLSC (Dunn et al., 2011)", "dunn_qc_rlsc"),
  COMMON_SCALE_LOG_LOESS("Common-scale log LOESS (fixed bandwidth)", "common_scale_log_loess");

  private final String label;
  private final String stableId;

  FeatureQcMethod(@NotNull final String label, @NotNull final String stableId) {
    this.label = label;
    this.stableId = stableId;
  }

  public @NotNull String stableId() {
    return stableId;
  }

  @Override
  public String toString() {
    return label;
  }
}
