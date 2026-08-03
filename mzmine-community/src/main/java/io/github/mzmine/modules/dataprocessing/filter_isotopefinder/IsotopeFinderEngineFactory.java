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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ElementAutoDetector;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngine;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngineConfig;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.LinkedHashSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 * Builds an {@link IsotopeFinderEngine} from an {@link IsotopeFinderParameters} parameter set.
 * Single source of truth for the engine wiring so both {@link IsotopeFinderTask} (the processing
 * run) and the on-demand diagnostics recompute (compound dashboard, see
 * {@link IsotopeFinderDiagnostics}) build an identically configured engine.
 */
public final class IsotopeFinderEngineFactory {

  private IsotopeFinderEngineFactory() {
  }

  /**
   * Build the engine configured from {@code parameters}.
   *
   * @param parameters      an {@link IsotopeFinderParameters} value set.
   * @param keepDiagnostics developer-only: retain rich per-charge scoring diagnostics on the result
   *                        (used by the compound dashboard review tooling). Off for the normal
   *                        run.
   * @return the configured engine.
   */
  public static @NotNull IsotopeFinderEngine create(@NotNull final ParameterSet parameters,
      final boolean keepDiagnostics) {
    final List<Element> elements = parameters.getValue(IsotopeFinderParameters.elements);
    final int maxCharge = parameters.getValue(IsotopeFinderParameters.maxCharge);
    final MZTolerance tol = parameters.getValue(IsotopeFinderParameters.isotopeMzTolerance);

    final ValueWithParameters<IsotopeFinderModeOptions> modeValue = parameters.getParameter(
        IsotopeFinderParameters.mode).getValueWithParameters();
    final EnvelopeContext ctx = new EnvelopeContext(elements, tol);
    final EnvelopeModel model = IsotopeFinderModeOptions.createModel(modeValue, ctx);

    final boolean requireC13 = parameters.getValue(IsotopeFinderParameters.requireC13);
    final ElementDetectionMode elementDetectionMode = parameters.getValue(
        IsotopeFinderParameters.elementDetectionMode);
    final List<String> autoCandidates = autoCandidates(elementDetectionMode, elements);

    final boolean explainableOnly = parameters.getValue(
        IsotopeFinderParameters.explainableSignalsOnly);

    return new IsotopeFinderEngine(
        IsotopeFinderEngineConfig.of(elements, maxCharge, tol, model, modeValue.value().toString(),
                requireC13).withElementDetection(elementDetectionMode, autoCandidates)
            .withExplainableSignalsOnly(explainableOnly).withDiagnostics(keepDiagnostics));
  }

  /**
   * @return the heavy-element symbols the auto-detector may infer for the given detection mode.
   */
  public static @NotNull List<String> autoCandidates(@NotNull final ElementDetectionMode mode,
      @NotNull final List<Element> elements) {
    return switch (mode) {
      case USER_DEFINED -> List.of();
      case AUTO_DETECT -> ElementAutoDetector.DEFAULT_CANDIDATES;
      case USER_PLUS_AUTO -> {
        final LinkedHashSet<String> set = new LinkedHashSet<>();
        for (final Element el : elements) {
          final String s = el.getSymbol();
          if (!"C".equals(s) && !"H".equals(s)) {
            set.add(s);
          }
        }
        set.addAll(ElementAutoDetector.DEFAULT_CANDIDATES);
        yield List.copyOf(set);
      }
    };
  }
}
