/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Feature-level eligibility and batch diagnostics. */
public record FeatureQcRowDiagnostic(int rowId, int totalQcs, int usableQcs,
                                     @Nullable FeatureQcUnavailableReason unavailableReason,
                                     @NotNull Map<String, FeatureQcBatchDiagnostic> batches) {

  public FeatureQcRowDiagnostic {
    batches = Map.copyOf(batches);
  }

  public boolean globallyAvailable() {
    return unavailableReason == null;
  }
}
