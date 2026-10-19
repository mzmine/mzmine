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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

/**
 * Per-charge scoring breakdown used to select the best charge and flag probable alternates. Every
 * component is bounded [0,1].
 *
 * @param coverage           share of the expected carbon envelope explained by any observed signal,
 *                           weighted by predicted intensity so missing a tail peak costs far less
 *                           than missing the apex.
 * @param carbonFit          cosine similarity of the isolated 13C ladder against the predicted
 *                           envelope at its best placement; 1 also means "too few 13C peaks to
 *                           assess", where coverage carries the detection.
 * @param selfConsistency    presence of the intermediate peaks a higher charge requires; always 1
 *                           for charge 1.
 * @param spacingConsistency how well one m/z spacing explains the on-grid positions. DIAGNOSTIC
 *                           only - not folded into {@code score} or the selection, because a naive
 *                           fold regressed polyhalogen combs.
 * @param intensityAgreement share of the observed intensity within the plausible predicted bound.
 * @param score              display value stored on the
 *                           {@link io.github.mzmine.datamodel.IsotopePattern}. It does NOT rank the
 *                           charges - {@code raw} does.
 * @param raw                the winner-selection score: quality x a peak-count reward, so a genuine
 *                           higher charge beats a lower one that fits only a subsample of the ladder.
 * @param probability        display-only quality share among the candidate charges. Alternates are
 *                           flagged by an absolute margin on the quality, not by this.
 */
public record ChargeScore(int charge, double coverage, double carbonFit, double selfConsistency,
                          double spacingConsistency, double intensityAgreement, double score,
                          double raw, double probability) {

}
