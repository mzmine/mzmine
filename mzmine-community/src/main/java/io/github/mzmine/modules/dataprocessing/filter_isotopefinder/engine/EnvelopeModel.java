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

import io.github.mzmine.datamodel.PolarityType;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Predicts the isotope intensity envelope for a searched signal at a given charge - the seam a
 * future alternative model (formula prediction, say) would replace. Today
 * {@link io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonEnvelopeModel}
 * is the only implementation.
 */
public interface EnvelopeModel {

  /**
   * @param observedMz any signal of the pattern, not necessarily the monoisotopic.
   * @param polarity   used for the (minor) ionization mass correction.
   * @return the predicted envelope, possibly near-empty when nothing can be predicted.
   */
  @NotNull IsotopeEnvelope buildEnvelope(double observedMz, int charge,
      @NotNull PolarityType polarity);

  /**
   * @param detectedHeavyCounts element symbol -> atom count for the heavy upper bound, or null for
   *                            the model's own default.
   * @param includeUserHeavies  whether to model the user-configured heavy elements as well.
   */
  @NotNull IsotopeEnvelope buildEnvelope(double observedMz, int charge,
      @NotNull PolarityType polarity, @Nullable Map<String, Integer> detectedHeavyCounts,
      boolean includeUserHeavies);

  /**
   * Bounds of the expected M+1/M (13C) relative intensity, read by the require-13C gate and the
   * carbon-ratio plausibility penalty.
   *
   * @return {@code {low, high}} bounds of the M+1/M ratio.
   */
  double @NotNull [] expectedM1RatioBounds(double observedMz, int charge,
      @NotNull PolarityType polarity);
}
