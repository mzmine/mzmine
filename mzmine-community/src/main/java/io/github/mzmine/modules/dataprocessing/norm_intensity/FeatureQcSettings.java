/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.dataprocessing.norm_intensity.FeatureQcSmoothing.QcRlsc;
import org.jetbrains.annotations.NotNull;

/** Settings for one QC drift correction pipeline, with QC-RLSC or manual LOESS smoothing. */
public record FeatureQcSettings(@NotNull FeatureQcSmoothing smoothing, int minimumUsableQcs,
                                double maxFactor, double globalMissingQcThreshold) {

  public FeatureQcSettings {
    if (smoothing == null) {
      throw new IllegalArgumentException("smoothing must be set.");
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
    return new FeatureQcSettings(QcRlsc.DEFAULT, 5, 10d, .5d);
  }

  /**
   * @return true if span and degree are selected by QC-RLSC cross-validation
   */
  public boolean automaticSmoothing() {
    return smoothing instanceof QcRlsc;
  }

  public int robustnessIterations() {
    return smoothing.robustnessIterations();
  }

  public @NotNull String description() {
    return smoothing.description();
  }
}
