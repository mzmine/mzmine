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

import org.jetbrains.annotations.Nullable;

/** Descriptive status and abundance summary for one explicitly selected sample group. */
public record GroupSummary(int sampleCount, int detectedCount, int estimatedCount,
                           int aggregatedCount, int missingCount, int quantifiedCount,
                           @Nullable Double mean, @Nullable Double cv) {
}
