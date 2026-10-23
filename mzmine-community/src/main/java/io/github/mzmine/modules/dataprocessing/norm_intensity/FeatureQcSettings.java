/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.NotNull;

/** Settings for one QC-RLSC pipeline, with automatic or fixed smoothing. */
public record FeatureQcSettings(boolean automaticSmoothing, double bandwidth, int polynomialDegree,
                                int robustnessIterations, int minimumUsableQcs,
                                double maxFactor, double globalMissingQcThreshold,
                                int initialQcsToExclude) {

  public FeatureQcSettings {
    if (initialQcsToExclude < 0) {
      throw new IllegalArgumentException("initial_qcs_to_exclude must be nonnegative.");
    }
    if (!Double.isFinite(bandwidth) || bandwidth <= 0d || bandwidth > 1d) {
      throw new IllegalArgumentException("bandwidth must be in (0, 1].");
    }
    if (polynomialDegree < 1 || polynomialDegree > 2) {
      throw new IllegalArgumentException("polynomial_degree must be 1 or 2.");
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
    return new FeatureQcSettings(true, .75d, 1, 2, 5, 10d, .5d, 0);
  }

  public @NotNull String description() {
    return automaticSmoothing ? "QC-RLSC (automatic smoothing)" : "QC-RLSC (fixed smoothing)";
  }
}
