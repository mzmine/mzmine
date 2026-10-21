/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.NotNull;

/** Immutable scientific settings for version 2 feature-specific QC drift normalization. */
public record FeatureQcSettings(double bandwidth, int robustnessIterations, int minimumUsableQcs,
                                double maxFactor, double globalMissingQcThreshold) {

  public static final int METHOD_VERSION = 2;

  public FeatureQcSettings {
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
    return new FeatureQcSettings(.75d, 2, 5, 10d, .5d);
  }

  public int methodVersion() {
    return METHOD_VERSION;
  }
}
