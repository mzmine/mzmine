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

/**
 * Aggregated accuracy metrics for one benchmark axis (or the synthetic {@code ALL} row). One row is
 * produced per stressor axis plus an overall row; the set of rows is the committed baseline.
 * <p>
 * Rates are fractions in {@code [0,1]} unless noted. Metrics whose per-case value is undefined for
 * every case in the axis (e.g. {@link #borderlineRecall} when no case carries borderline peaks, or
 * {@link #aucCharge} when the axis has no charge errors) are reported as {@link Double#NaN}.
 *
 * @param axis                  stressor axis label, or {@code ALL} for the overall row
 * @param nCases                number of cases contributing to this row
 * @param chargeTop1            fraction of cases where the predicted best charge equals the true
 *                              charge
 * @param chargeRecallAlt       fraction of cases where the true charge is among the flagged charge
 *                              scores (winner + alternates)
 * @param chargeStartInvariance fraction of cases where the winning charge is identical across all
 *                              tested start signals (monoisotopic / base / top peak) - the
 *                              position-agnostic property
 * @param patternPrecision      mean per-case precision of the best detected pattern's peaks vs. the
 *                              true isotope peaks
 * @param patternRecall         mean per-case recall of the best detected pattern's peaks vs. the
 *                              true isotope peaks
 * @param patternF1             mean per-case F1 of pattern precision/recall
 * @param borderlineRecall      mean per-case recall of weak (borderline) true peaks; cases without
 *                              borderline peaks are excluded from the mean
 * @param noiseLeak             mean per-case fraction of injected false peaks that leaked into the
 *                              detected pattern; cases without false peaks are excluded from the
 *                              mean
 * @param elementPrecision      mean per-case precision of the (placeholder) detected heavy-element
 *                              set restricted to {Cl,Br,S,Si}; undefined cases excluded
 * @param elementRecall         mean per-case recall of the (placeholder) detected heavy-element set
 *                              restricted to {Cl,Br,S,Si}; undefined cases excluded
 * @param scoreMargin           mean over cases with a detection of (best correct-charge score −
 *                              best incorrect-charge score); positive is good separation
 * @param aucCharge             separation AUC: fraction of (correct-charge, incorrect-charge) case
 *                              pairs where the correct case's winning score exceeds the incorrect
 *                              case's (ties = 0.5); {@link Double#NaN} if the axis has no charge
 *                              errors
 * @param medianDetectMs        median wall time of the {@code engine.detect(...)} call in
 *                              milliseconds
 */
public record MetricRow(@NotNull String axis, int nCases, double chargeTop1, double chargeRecallAlt,
                        double chargeStartInvariance, double patternPrecision, double patternRecall,
                        double patternF1, double borderlineRecall, double noiseLeak,
                        double elementPrecision, double elementRecall, double scoreMargin,
                        double aucCharge, double medianDetectMs) {

}
