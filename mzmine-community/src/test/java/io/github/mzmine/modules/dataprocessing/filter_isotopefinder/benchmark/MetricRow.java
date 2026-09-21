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
 * Aggregated accuracy metrics for one benchmark axis, or the synthetic {@code ALL} row. The set of
 * rows is the committed baseline.
 * <p>
 * Each component is the mean - or, for the boolean primitives, the fraction - of the corresponding
 * {@link CaseMetrics} component, which is where the individual measures are documented. A case
 * where a metric is undefined is EXCLUDED from its mean rather than counted as 0, so a metric
 * undefined across the whole axis comes out as {@link Double#NaN}.
 *
 * @param axis               stressor axis label, or {@code ALL} for the overall row
 * @param elementContainment fraction of cases where EVERY true heavy element was reported - the
 *                           target metric for a set of possibilities. Below 1.0 means the heavy
 *                           upper bound was too tight somewhere.
 * @param elementSetSize     mean number of reported heavy elements, i.e. the COST of containment
 *                           (1.0 = one element per case, 4.0 = every candidate)
 * @param aucCharge          separation AUC over (correct, incorrect) case pairs, ties counting
 *                           0.5; {@link Double#NaN} when the axis has no charge errors
 */
public record MetricRow(@NotNull String axis, int nCases, double chargeTop1, double chargeRecallAlt,
                        double chargeStartInvariance, double patternPrecision, double patternRecall,
                        double patternF1, double borderlineRecall, double noiseLeak,
                        double elementPrecision, double elementRecall, double elementContainment,
                        double elementSetSize, double scoreMargin, double aucCharge,
                        double medianDetectMs) {

}
