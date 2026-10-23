/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.NotNull;

/** Immutable settings; the method identifies the scientific procedure, not a release number. */
public record FeatureQcSettings(@NotNull FeatureQcMethod method, double bandwidth,
                                int robustnessIterations, int minimumUsableQcs,
                                double maxFactor, double globalMissingQcThreshold,
                                int initialQcsToExclude) {

  public FeatureQcSettings {
    java.util.Objects.requireNonNull(method, "QC correction method");
    if (initialQcsToExclude < 0) {
      throw new IllegalArgumentException("initial_qcs_to_exclude must be nonnegative.");
    }
    if (!Double.isFinite(bandwidth) || bandwidth <= 0d || bandwidth > 1d) {
      throw new IllegalArgumentException("bandwidth must be in (0, 1].");
    }
    if (robustnessIterations < 0) {
      throw new IllegalArgumentException("robustness_iterations must be nonnegative.");
    }
    if (minimumUsableQcs < 5) {
      throw new IllegalArgumentException("minimum_usable_qcs must be at least 5.");
    }
    if (!Double.isFinite(maxFactor) || maxFactor <= 1d) {
      throw new IllegalArgumentException("max_factor must be finite and greater than one.");
    }
    if (!Double.isFinite(globalMissingQcThreshold) || globalMissingQcThreshold < 0d
        || globalMissingQcThreshold > 1d) {
      throw new IllegalArgumentException("global_missing_qc_threshold must be in [0, 1].");
    }
  }

  public static @NotNull FeatureQcSettings defaults() {
    return new FeatureQcSettings(FeatureQcMethod.DUNN_QC_RLSC, .75d, 2, 5, 10d, .5d, 0);
  }

  /** Explicit fixed-bandwidth log method, also used by its numerical reference fixtures. */
  public FeatureQcSettings(final double bandwidth, final int robustnessIterations,
      final int minimumUsableQcs, final double maxFactor, final double globalMissingQcThreshold) {
    this(FeatureQcMethod.COMMON_SCALE_LOG_LOESS, bandwidth, robustnessIterations, minimumUsableQcs,
        maxFactor, globalMissingQcThreshold, 0);
  }
}
