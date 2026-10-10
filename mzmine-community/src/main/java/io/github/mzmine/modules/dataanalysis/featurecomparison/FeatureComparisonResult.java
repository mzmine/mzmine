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

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A deterministic comparison result with human- and machine-readable exclusion reasons. */
public record FeatureComparisonResult(int rowId, @NotNull GroupSummary target,
                                      @NotNull GroupSummary reference,
                                      @Nullable GroupSummary blank,
                                      @Nullable Double foldChange, @Nullable Double blankRatio,
                                      @NotNull String differenceStatus, @NotNull String blankStatus,
                                      boolean passes, @NotNull List<String> reasons) {

  public FeatureComparisonResult {
    reasons = List.copyOf(reasons);
  }
}
