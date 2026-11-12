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
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Row-level correlation of one MS feature row to one aligned other-detector row
 * ({@link OtherFeatureListRow}). It carries the individual per-file {@link CorrelatedOtherFeature}s
 * (one per raw file where the trace correlated). This is the element type of the row-level
 * "correlated traces" column; the per-file result is used at the feature level.
 * <p>
 * There is intentionally no aggregate correlation <i>type</i> here: individual files may differ
 * (calculated / manual / aligned). {@code bestCorrelation} is the best (highest) Pearson score across
 * the per-file results, or null if none has a score.
 *
 * @param otherRowId      the {@link OtherFeatureListRow#getID()} of the correlated aligned other-row
 * @param otherRow        the correlated aligned other-row (trace identity via its {@link TraceKey})
 * @param bestCorrelation the highest per-file Pearson score, or null
 * @param perFileResults  the per-file correlation results
 */
public record MsOtherCorrelationRowResult(int otherRowId, @NotNull OtherFeatureListRow otherRow,
                                          @Nullable Float bestCorrelation,
                                          @NotNull List<CorrelatedOtherFeature> perFileResults) {

  @Override
  public String toString() {
    final NumberFormat score = ConfigService.getGuiFormats().scoreFormat();
    return "%s (%s)".formatted(otherRow.getTraceKey().toString(),
        bestCorrelation != null ? score.format(bestCorrelation) : "?");
  }
}
