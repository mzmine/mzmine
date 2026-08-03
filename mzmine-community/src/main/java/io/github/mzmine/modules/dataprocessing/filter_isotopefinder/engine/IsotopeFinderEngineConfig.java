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

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.ElementDetectionMode;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 * Full configuration of an {@link IsotopeFinderEngine}. Built with {@link #of} for the required
 * settings and refined with the {@code with*} methods, so an optional setting - notably
 * {@link #keepDiagnostics()}, which is developer-only - is named at the call site instead of being a
 * positional boolean.
 *
 * @param elements             the elements whose isotope m/z differences seed the candidate search.
 * @param maxCharge            highest charge hypothesis to score.
 * @param tol                  m/z tolerance for matching signals.
 * @param model                the predicted-envelope model to score against.
 * @param modeLabel            short label of the envelope mode, used in pattern descriptions.
 * @param requireC13           whether a gap-free 13C ladder is required to accept a charge.
 * @param elementDetectionMode whether and how heavy elements are auto-detected.
 * @param autoCandidates       heavy-element symbols the auto-detector may infer (empty when
 *                             detection is off).
 * @param explainableSignalsOnly whether to drop emitted signals whose mass defect matches neither
 *                               the 13C grid nor a combination of the elements' isotopes. Off by
 *                               default - it lowers the noise leak at the cost of pattern
 *                               completeness, see
 *                               {@code IsotopeFinderParameters#explainableSignalsOnly}.
 * @param keepDiagnostics      developer-only: retain rich per-charge scoring diagnostics on the
 *                             {@link DetectionResult}. Off in the normal processing run.
 */
public record IsotopeFinderEngineConfig(@NotNull List<Element> elements, int maxCharge,
                                        @NotNull MZTolerance tol, @NotNull EnvelopeModel model,
                                        @NotNull String modeLabel, boolean requireC13,
                                        @NotNull ElementDetectionMode elementDetectionMode,
                                        @NotNull List<String> autoCandidates,
                                        boolean explainableSignalsOnly, boolean keepDiagnostics) {

  /**
   * @return a configuration with element auto-detection, the explainable-signals filter and
   * diagnostics off.
   */
  public static @NotNull IsotopeFinderEngineConfig of(@NotNull final List<Element> elements,
      final int maxCharge, @NotNull final MZTolerance tol, @NotNull final EnvelopeModel model,
      @NotNull final String modeLabel, final boolean requireC13) {
    return new IsotopeFinderEngineConfig(elements, maxCharge, tol, model, modeLabel, requireC13,
        ElementDetectionMode.USER_DEFINED, List.of(), false, false);
  }

  /**
   * @param mode       the element detection mode.
   * @param candidates heavy-element symbols the auto-detector may infer.
   * @return a copy with element auto-detection configured.
   */
  public @NotNull IsotopeFinderEngineConfig withElementDetection(
      @NotNull final ElementDetectionMode mode, @NotNull final List<String> candidates) {
    return new IsotopeFinderEngineConfig(elements, maxCharge, tol, model, modeLabel, requireC13,
        mode, candidates, explainableSignalsOnly, keepDiagnostics);
  }

  /**
   * @param only whether to emit only signals attributable to the 13C grid or an isotope defect.
   * @return a copy with the emitted-signal attribution filter set.
   */
  public @NotNull IsotopeFinderEngineConfig withExplainableSignalsOnly(final boolean only) {
    return new IsotopeFinderEngineConfig(elements, maxCharge, tol, model, modeLabel, requireC13,
        elementDetectionMode, autoCandidates, only, keepDiagnostics);
  }

  /**
   * @param keep whether to retain the per-charge scoring diagnostics (developer-only).
   * @return a copy with the diagnostics flag set.
   */
  public @NotNull IsotopeFinderEngineConfig withDiagnostics(final boolean keep) {
    return new IsotopeFinderEngineConfig(elements, maxCharge, tol, model, modeLabel, requireC13,
        elementDetectionMode, autoCandidates, explainableSignalsOnly, keep);
  }
}
