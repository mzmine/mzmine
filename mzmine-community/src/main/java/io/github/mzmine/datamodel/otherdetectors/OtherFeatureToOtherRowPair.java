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

import org.jetbrains.annotations.NotNull;

/**
 * An {@link OtherFeature} together with the aligned {@link OtherFeatureListRow} that holds it. This
 * is the unit used to link MS features to other detector features: correlations are always stored
 * by {@link OtherFeatureListRow#getID()}, so a candidate or a user selection carries its row and
 * never needs to be resolved by object identity or trace key.
 * <p>
 * This is only an intermediate pairing, created on demand (e.g., the candidates of the MS-to-other
 * correlation or a selection in the correlation dashboard). It is never stored in the data model
 * and not used to persist anything; correlations persist the row IDs in
 * {@link MsOtherCorrelationMaps}. The pairing is needed because, unlike a ModularFeature, an
 * {@link OtherFeature} has no reference to its row.
 *
 * @param row     the aligned other-row that holds {@code feature}
 * @param feature the other detector feature of one raw data file in {@code row}
 */
public record OtherFeatureToOtherRowPair(@NotNull OtherFeatureListRow row,
                                         @NotNull OtherFeature feature) {

  @Override
  public String toString() {
    return feature.toString();
  }
}
