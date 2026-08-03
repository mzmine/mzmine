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

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Rich per-charge scoring diagnostics captured only when the engine runs with
 * {@code keepDiagnostics}. These are NOT persisted; the compound dashboard recomputes them on
 * demand (from the isotope finder applied-method parameters) to visualise how the algorithm scored
 * a charge hypothesis: the predicted envelope actually used, where the sliding template placed the
 * monoisotopic anchor, per-signal element attribution, and the exported {@link ChargeScore}.
 *
 * @param charge              the charge hypothesis (>= 1).
 * @param baseMz              observed base peak m/z used as the grid origin (offset 0).
 * @param baseIntensity       observed base peak intensity (the envelope is normalised to the base
 *                            peak, so multiply {@link #envelopeExpected()} by this to overlay).
 * @param placement           predicted offset (relative to the monoisotopic) aligned to the
 *                            observed base peak by the sliding carbon fit. The monoisotopic
 *                            therefore sits at observed offset {@code -placement}.
 * @param spacingDa           m/z spacing between consecutive offsets (13C distance / charge).
 * @param envelopeExpected    predicted relative intensity per offset (offset 0 = monoisotopic),
 *                            base peak normalised to 1.0. Drives the fit/shape overlay.
 * @param envelopeUpperBound  maximum plausible relative intensity per offset. Drives the
 *                            plausibility colouring (observed within vs exceeding the bound).
 * @param m1Bounds            carbon M+1/M ratio bounds {@code [lo, hi]} for the require-13C gate.
 * @param signals             per kept signal attribution (13C ladder / heavy isotope), ordered by
 *                            m/z.
 * @param score               the exported per-charge {@link ChargeScore} breakdown.
 */
public record ChargeDiagnostics(int charge, double baseMz, double baseIntensity, int placement,
                                double spacingDa, @NotNull double[] envelopeExpected,
                                @NotNull double[] envelopeUpperBound, @NotNull double[] m1Bounds,
                                @NotNull List<IsotopeSignalAttribution> signals,
                                @NotNull ChargeScore score) {

  /**
   * @return the m/z of the (predicted) monoisotopic peak (predicted offset 0).
   */
  public double monoMz() {
    return baseMz - placement * spacingDa;
  }

  /**
   * @param predictedOffset predicted offset relative to the monoisotopic (0 = mono, 1 = M+1, ...).
   * @return the m/z at which that predicted offset falls on the observed grid.
   */
  public double mzForPredictedOffset(final int predictedOffset) {
    return baseMz + (predictedOffset - placement) * spacingDa;
  }

  /**
   * @return the highest predicted offset in the envelope (inclusive).
   */
  public int maxPredictedOffset() {
    return envelopeExpected.length - 1;
  }
}
