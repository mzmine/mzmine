/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import org.jetbrains.annotations.Nullable;

/** One compact, native-solver prediction point for a feature-QC batch curve. */
public record FeatureQcFitPoint(double coordinate, @Nullable Double observedQc, double fittedQc) {
}
