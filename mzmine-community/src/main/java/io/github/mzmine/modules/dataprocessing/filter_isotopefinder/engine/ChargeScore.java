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
 * Per-charge scoring breakdown used to select the best charge and flag probable alternates.
 *
 * @param charge          the charge hypothesis (>= 1).
 * @param coverage        fraction of expected carbon offsets explained by any observed signal,
 *                        including heavy isotopes (0..1).
 * @param carbonFit       bounded cosine similarity of the isolated 13C ladder against the predicted
 *                        carbon envelope at its best placement (0..1; 1 = perfect or too few 13C
 *                        peaks to assess, in which case coverage carries the detection).
 * @param selfConsistency   requirement that the intermediate (e.g. half-spacing) peaks of a higher
 *                          charge are present (1 = consistent, lower = missing required peaks). For
 *                          charge 1 this is always 1.
 * @param intensityAgreement fraction of the observed intensity that stays within the plausible
 *                          predicted upper bound (1 = all describable, lower = implausibly large
 *                          signals present).
 * @param score             bounded [0,1] quality (carbonFit x coverage x intensityAgreement, gated
 *                          by selfConsistency for higher charges). This is the value stored on the
 *                          {@link io.github.mzmine.datamodel.IsotopePattern} to rank charges.
 * @param raw               combined raw score (bounded quality x weak peak-count tie-breaker).
 * @param probability       pseudo-probability that this is the major charge (linear share of the raw
 *                          scores of all candidate charges).
 */
public record ChargeScore(int charge, double coverage, double carbonFit, double selfConsistency,
                          double intensityAgreement, double score, double raw, double probability) {

}
