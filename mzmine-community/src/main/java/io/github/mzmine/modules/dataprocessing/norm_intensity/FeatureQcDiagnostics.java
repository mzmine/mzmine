/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

/** Typed diagnostics produced by one immutable feature-QC calculation. */
public record FeatureQcDiagnostics(@NotNull String solver, @NotNull String coordinatePolicy,
                                   @NotNull String scalePolicy,
                                   @NotNull FeatureQcSettings settings,
                                   @NotNull FeatureQcAvailability availability,
                                   @NotNull Map<Integer, FeatureQcRowDiagnostic> rows) {

  public FeatureQcDiagnostics {
    rows = Map.copyOf(rows);
  }
}
