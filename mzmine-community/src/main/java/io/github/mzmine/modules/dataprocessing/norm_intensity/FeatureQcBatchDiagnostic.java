/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Coverage, fit, validation, and correction magnitude for one feature in one batch. */
public record FeatureQcBatchDiagnostic(String batch, int totalQcs, int usableQcs,
                                       @Nullable Double usableQcCoordinateMinimum,
                                       @Nullable Double usableQcCoordinateMaximum,
                                       int boundarySampleCount, int factorCount,
                                       @Nullable FeatureQcUnavailableReason unavailableReason,
                                       @Nullable Double beforeQcRsd, @Nullable Double afterQcRsd,
                                       int rsdObservationCount, @Nullable Double leaveOneOutMse,
                                       int leaveOneOutPredictionCount, int leaveOneOutFailureCount,
                                       @Nullable String validationUnavailableReason,
                                       @Nullable Double minimumFactor,
                                       @Nullable Double maximumFactor,
                                       @NotNull List<FeatureQcFitPoint> fitPoints,
                                       @Nullable Integer selectedDegree,
                                       @Nullable Integer selectedNeighborhoodSize,
                                       @Nullable Double selectedSpan,
                                       @Nullable Double correctionTarget,
                                       @NotNull String validationScale) {

  public FeatureQcBatchDiagnostic {
    fitPoints = List.copyOf(fitPoints);
  }

  public boolean successful() {
    return unavailableReason == null;
  }
}
