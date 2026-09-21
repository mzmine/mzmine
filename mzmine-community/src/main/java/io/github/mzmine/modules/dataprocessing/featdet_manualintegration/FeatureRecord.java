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

package io.github.mzmine.modules.dataprocessing.featdet_manualintegration;

import io.github.mzmine.datamodel.features.FeatureListRow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifies a {@link FeatureListRow} for reproducible manual integration. The {@link #rowId()} is
 * the primary key; {@link #mz()}, {@link #rt()} and {@link #mobility()} serve as a fallback match
 * when row IDs have shifted (e.g. after re-alignment).
 *
 * @param rowId    the row id at capture time
 * @param mz       the average m/z of the row
 * @param rt       the average retention time of the row
 * @param mobility the average mobility of the row, or null for non-IMS data
 */
public record FeatureRecord(int rowId, double mz, float rt, @Nullable Float mobility) {

  /**
   * Captures the identity of the given row.
   */
  public static @NotNull FeatureRecord of(@NotNull FeatureListRow row) {
    return new FeatureRecord(row.getID(), row.getAverageMZ(), row.getAverageRT(),
        row.getAverageMobility());
  }
}
