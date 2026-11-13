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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureListRow;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureToOtherRowPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The outcome of a manual correlation of an MS feature to an other detector feature, see
 * {@link OtherFeatureManualUtils#correlateManually}.
 *
 * @param aligned          the aligned row and the feature in this row that is now correlated
 * @param replaced         the feature of the same raw data file that was replaced in the row, null
 *                         if the cell was empty or already held the same peak
 * @param createdRow       true if a new row was created for the feature
 * @param movedFrom        the row the peak was moved from together with its correlations (a
 *                         misaligned peak), null if it was not moved
 * @param removedSourceRow true if {@code movedFrom} was removed from the aligned list afterwards
 * @param changed          false if the MS row was already correlated to this peak in this row and
 *                         nothing changed
 */
public record ManualOtherCorrelationResult(@NotNull OtherFeatureToOtherRowPair aligned,
                                           @Nullable OtherFeature replaced, boolean createdRow,
                                           @Nullable OtherFeatureListRow movedFrom,
                                           boolean removedSourceRow, boolean changed) {

}
