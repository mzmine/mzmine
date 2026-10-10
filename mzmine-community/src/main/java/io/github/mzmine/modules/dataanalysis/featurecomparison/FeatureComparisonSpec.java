/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package io.github.mzmine.modules.dataanalysis.featurecomparison;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Immutable, one-directional target-versus-reference comparison settings. */
public record FeatureComparisonSpec(@NotNull List<Integer> targetIndexes,
                                    @NotNull List<Integer> referenceIndexes,
                                    @NotNull List<Integer> blankIndexes,
                                    double minimumDetectionFraction, double minimumFoldChange,
                                    @Nullable Double minimumBlankRatio,
                                    boolean includeSampleOnly) {

  public FeatureComparisonSpec {
    targetIndexes = List.copyOf(targetIndexes);
    referenceIndexes = List.copyOf(referenceIndexes);
    blankIndexes = List.copyOf(blankIndexes);
    validateIndexes(targetIndexes, "targetIndexes");
    validateIndexes(referenceIndexes, "referenceIndexes");
    validateIndexes(blankIndexes, "blankIndexes");
    if (targetIndexes.isEmpty() || referenceIndexes.isEmpty()) {
      throw new IllegalArgumentException("Target and reference groups must not be empty");
    }
    if (!Double.isFinite(minimumDetectionFraction) || minimumDetectionFraction < 0d
        || minimumDetectionFraction > 1d) {
      throw new IllegalArgumentException("minimumDetectionFraction must be between zero and one");
    }
    if (!Double.isFinite(minimumFoldChange) || minimumFoldChange < 1d) {
      throw new IllegalArgumentException("minimumFoldChange must be finite and at least one");
    }
    if (minimumBlankRatio != null && (!Double.isFinite(minimumBlankRatio)
        || minimumBlankRatio < 0d)) {
      throw new IllegalArgumentException("minimumBlankRatio must be finite and non-negative");
    }
    assertDisjoint(targetIndexes, referenceIndexes, "Target and reference groups overlap");
    assertDisjoint(targetIndexes, blankIndexes, "Target and blank groups overlap");
    assertDisjoint(referenceIndexes, blankIndexes, "Reference and blank groups overlap");
  }

  private static void validateIndexes(@NotNull List<Integer> indexes, @NotNull String name) {
    final Set<Integer> unique = new HashSet<>();
    for (final Integer index : indexes) {
      if (index == null || index < 0 || !unique.add(index)) {
        throw new IllegalArgumentException(name + " must contain unique non-negative indexes");
      }
    }
  }

  private static void assertDisjoint(@NotNull List<Integer> first, @NotNull List<Integer> second,
      @NotNull String message) {
    final Set<Integer> remaining = new HashSet<>(first);
    remaining.retainAll(second);
    if (!remaining.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
  }
}
