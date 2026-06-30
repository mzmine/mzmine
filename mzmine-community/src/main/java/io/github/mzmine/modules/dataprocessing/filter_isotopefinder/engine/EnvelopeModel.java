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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy that predicts the isotope intensity envelope for a searched signal at a given charge.
 * This is the only part of the detection that differs between the signal-based (carbon-averagine)
 * and formula-prediction modes. Implementations are created by an {@link EnvelopeModelModule} so
 * they can be selected through a
 * {@link io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter}.
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
   * Estimated lower/upper bound of the expected M+1 / M (13C) relative intensity for the searched
   * neutral mass, used by the optional "require 13C" gate. Models that cannot estimate this (e.g.
   * formula prediction) return {@code null}, in which case the gate only checks 13C M+1 presence.
   *
   * @param observedMz the observed m/z of the searched signal.
   * @param charge     the hypothesized charge state (>= 1).
   * @param polarity   ion polarity.
   * @return {@code {low, high}} bounds of the M+1/M ratio, or {@code null} if not estimable.
   */
  default @Nullable double[] expectedM1RatioBounds(double observedMz, int charge,
      @NotNull PolarityType polarity) {
    return null;
  }
}
