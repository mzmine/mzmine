/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.Nullable;

/** Aggregate availability of a feature-QC result. */
public record FeatureQcAvailability(int fullyCorrectedRows, int partiallyCorrectedRows,
                                    int uncorrectedRows, int globallySkippedRows,
                                    long correctedSamplePairs, long unavailableSamplePairs) {

  public boolean hasCorrectablePairs() {
    return correctedSamplePairs > 0;
  }

  public @Nullable String warning() {
    return hasCorrectablePairs() ? null
        : "No feature/sample pairs were eligible for feature-QC normalization.";
  }
}
