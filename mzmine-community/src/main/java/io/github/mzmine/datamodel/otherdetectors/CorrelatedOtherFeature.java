/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.main.ConfigService;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A resolved <b>view</b> of one MS-feature-to-other-detector correlation, built on demand from
 * {@link MsOtherCorrelationMaps}. It is no longer stored or serialized (persistence lives in the
 * maps, keyed by ID); it exists so table columns and consumers can read the correlated
 * {@link OtherFeature} together with the correlation origin and score.
 *
 * This is a single <b>per-file</b> result. For the row-level grouping of per-file results across an
 * aligned other-row, see {@link MsOtherCorrelationRowResult}.
 *
 * @param otherFeature the correlated other-detector feature in one raw data file
 * @param type         whether the correlation was calculated, set manually, or only aligned
 * @param correlation  the per-file Pearson score, or null when unavailable
 */
public record CorrelatedOtherFeature(@NotNull OtherFeature otherFeature,
                                     @NotNull MsOtherCorrelationType type,
                                     @Nullable Float correlation) {

  @Override
  public String toString() {
    final NumberFormat score = ConfigService.getGuiFormats().scoreFormat();

    return "%s, %s (%s)".formatted(otherFeature.toString(), type.toString(),
        correlation != null ? score.format(correlation) : "?");
  }
}
