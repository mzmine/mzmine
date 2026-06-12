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
 * @param spacingFraction fraction of expected isotope offsets that were actually observed (0..1).
 * @param envelopeFit     one-sided plausibility of the observed relative intensities against the
 *                        predicted upper bound (1 = all within bounds, lower = implausibly high
 *                        signals present).
 * @param selfConsistency requirement that the intermediate (e.g. half-spacing) peaks of a higher
 *                        charge are present (1 = consistent, lower = missing required peaks). For
 *                        charge 1 this is always 1.
 * @param raw             combined raw score (product of the weighted sub-scores).
 * @param probability     pseudo-probability that this is the major charge (softmax over raw scores
 *                        of all candidate charges).
 */
public record ChargeScore(int charge, double spacingFraction, double envelopeFit,
                          double selfConsistency, double raw, double probability) {

}
