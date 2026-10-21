package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.NotNull;

/** Scientific evidence for the frozen functions; contains no duplicate factor matrix. */
public record FeatureQcProvenance(@NotNull String sourceRevision,
                                  @NotNull String modeledAbundance,
                                  @NotNull FeatureQcDiagnostics diagnostics) {
}
