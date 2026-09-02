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
 * Strategy that predicts the isotope intensity envelope for a searched signal at a given charge.
 * This is the part of the detection a future alternative model (e.g. formula prediction) would
 * replace; today {@link io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel}
 * is the only implementation and models are built directly by the module-package
 * {@code IsotopeFinderEngineFactory}.
 */
public interface EnvelopeModel {

  /**
   * @param observedMz the observed m/z of the searched signal (any signal in the pattern).
   * @param charge     the hypothesized charge state (>= 1).
   * @param polarity   ion polarity, used for the (minor) ionization mass correction.
   * @return the predicted envelope for this charge hypothesis. Never null; may be a near-empty
   * envelope if nothing meaningful can be predicted.
   */
  @NotNull IsotopeEnvelope buildEnvelope(double observedMz, int charge,
      @NotNull PolarityType polarity);

  /**
   * Build the envelope using per-element detected heavy-atom counts.
   *
   * @param detectedHeavyCounts element symbol -> atom count to model for the heavy upper bound, or
   *                            null to use the model's own default. Overrides/extends the
   *                            user-configured heavy elements.
   * @param includeUserHeavies  whether to also model the user-configured heavy elements (at the
   *                            model's default estimated count) in addition to
   *                            {@code detectedHeavyCounts}.
   */
  @NotNull IsotopeEnvelope buildEnvelope(double observedMz, int charge,
      @NotNull PolarityType polarity, @Nullable Map<String, Integer> detectedHeavyCounts,
      boolean includeUserHeavies);

  /**
   * Estimated lower/upper bound of the expected M+1 / M (13C) relative intensity for the searched
   * neutral mass, used by the optional "require 13C" gate and by the carbon-ratio plausibility
   * penalty.
   *
   * @param observedMz the observed m/z of the searched signal.
   * @param charge     the hypothesized charge state (>= 1).
   * @param polarity   ion polarity.
   * @return {@code {low, high}} bounds of the M+1/M ratio.
   */
  double @NotNull [] expectedM1RatioBounds(double observedMz, int charge,
      @NotNull PolarityType polarity);
}
