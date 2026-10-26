package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.NotNull;

/** Scientific evidence for the normalized result; contains no duplicate factor matrix. */
public record FeatureQcProvenance(@NotNull String modeledAbundance,
                                  @NotNull FeatureQcDiagnostics diagnostics) {
}
