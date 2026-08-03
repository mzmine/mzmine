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
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.LinkedHashSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 * Builds an {@link IsotopeFinderEngine} from an {@link IsotopeFinderParameters} parameter set. Single
 * source of truth for the engine wiring so both {@link IsotopeFinderTask} (the processing run) and
 * the on-demand diagnostics recompute (compound dashboard, see {@link IsotopeFinderDiagnostics})
 * build an identically configured engine.
 */
public final class IsotopeFinderEngineFactory {

  private IsotopeFinderEngineFactory() {
  }

  /**
   * Resolve the selected algorithm to its full carbon-averagine setup. Simplified options map their
   * few parameters onto the full set with defaults for the rest.
   *
   * @param parameters an {@link IsotopeFinderParameters} value set.
   * @return the resolved, self-contained algorithm parameters.
   */
  public static @NotNull CarbonAveragineAlgorithmParameters resolveAlgorithmParameters(
      @NotNull final ParameterSet parameters) {
    final ValueWithParameters<IsotopeFinderModeOptions> modeValue = parameters.getParameter(
        IsotopeFinderParameters.mode).getValueWithParameters();
    return IsotopeFinderModeOptions.resolve(modeValue);
  }

  /**
   * @param parameters an {@link IsotopeFinderParameters} value set.
   * @return the name of the selected algorithm, for reporting.
   */
  public static @NotNull String algorithmName(@NotNull final ParameterSet parameters) {
    return parameters.getValue(IsotopeFinderParameters.mode).toString();
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
    return create(resolveAlgorithmParameters(parameters), algorithmName(parameters),
        keepDiagnostics);
  }

  /**
   * Build the engine from the already resolved algorithm parameters.
   *
   * @param algo            the full carbon-averagine setup.
   * @param algorithmName   name of the selected algorithm, only used for reporting.
   * @param keepDiagnostics developer-only, see {@link #create(ParameterSet, boolean)}.
   * @return the configured engine.
   */
  public static @NotNull IsotopeFinderEngine create(
      @NotNull final CarbonAveragineAlgorithmParameters algo, @NotNull final String algorithmName,
      final boolean keepDiagnostics) {
    final List<Element> elements = algo.getValue(CarbonAveragineAlgorithmParameters.elements);
    final int maxCharge = algo.getValue(CarbonAveragineAlgorithmParameters.maxCharge);
    final MZTolerance tol = algo.getValue(CarbonAveragineAlgorithmParameters.isotopeMzTolerance);

    final EnvelopeContext ctx = new EnvelopeContext(elements, tol);
    final EnvelopeModel model = new CarbonAveragineEnvelopeModel(
        algo.getParameter(CarbonAveragineAlgorithmParameters.envelope).getEmbeddedParameters(), ctx);

    final boolean requireC13 = algo.getValue(CarbonAveragineAlgorithmParameters.requireC13);
    final ElementDetectionMode elementDetectionMode = algo.getValue(
        CarbonAveragineAlgorithmParameters.elementDetectionMode);
    final List<String> autoCandidates = autoCandidates(elementDetectionMode, elements);

    final boolean explainableOnly = algo.getValue(
        CarbonAveragineAlgorithmParameters.explainableSignalsOnly);

    return new IsotopeFinderEngine(
        IsotopeFinderEngineConfig.of(elements, maxCharge, tol, model, algorithmName, requireC13)
            .withElementDetection(elementDetectionMode, autoCandidates)
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
