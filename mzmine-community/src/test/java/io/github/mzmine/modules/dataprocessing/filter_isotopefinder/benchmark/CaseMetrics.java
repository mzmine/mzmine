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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-case accuracy primitives for a single {@link GroundTruthCase} scored against one engine
 * {@code DetectionResult}. These are aggregated by {@link IsotopeMetrics} into a {@link MetricRow}
 * per axis.
 * <p>
 * Boxed {@link Double} fields are nullable on purpose: a {@code null} means the metric is undefined
 * for this case and must be excluded from the axis mean (rather than counted as {@code 0}).
 *
 * @param axis                 the case's stressor axis
 * @param trueCharge           ground-truth charge
 * @param predictedCharge      engine best charge, or {@code 0} when the engine returned no
 *                             detection
 * @param chargeTop1           predicted best charge equals the true charge
 * @param chargeRecallAlt      true charge is among the flagged charge scores (winner + alternates)
 * @param chargeStartInvariant the winning charge is identical when the finder is seeded from every
 *                             tested start signal (monoisotopic / base / top peak) - the
 *                             position-agnostic property; independent of whether that charge is
 *                             correct
 * @param patternPrecision     precision of the best detected pattern vs. the true isotope peaks
 * @param patternRecall        recall of the best detected pattern vs. the true isotope peaks
 * @param patternF1            harmonic mean of pattern precision/recall
 * @param borderlineRecall     recall of weak (borderline) true peaks, or {@code null} if none
 *                             exist
 * @param noiseLeak            fraction of injected false peaks that leaked in, or {@code null} if
 *                             none
 * @param elementPrecision     placeholder heavy-element precision (Cl/Br/S/Si), or {@code null}
 * @param elementRecall        placeholder heavy-element recall (Cl/Br/S/Si), or {@code null}
 * @param scoreMargin          best correct-charge score − best incorrect-charge score, or
 *                             {@code null} if there was no detection
 * @param winningScore         the best-first (winner) charge score, or {@code 0} if no detection
 * @param detectMs             wall time of the {@code detect(...)} call in milliseconds
 */
public record CaseMetrics(@NotNull String axis, int trueCharge, int predictedCharge,
                          boolean chargeTop1, boolean chargeRecallAlt, boolean chargeStartInvariant,
                          double patternPrecision, double patternRecall, double patternF1,
                          @Nullable Double borderlineRecall, @Nullable Double noiseLeak,
                          @Nullable Double elementPrecision, @Nullable Double elementRecall,
                          @Nullable Double scoreMargin, double winningScore, double detectMs) {

}
