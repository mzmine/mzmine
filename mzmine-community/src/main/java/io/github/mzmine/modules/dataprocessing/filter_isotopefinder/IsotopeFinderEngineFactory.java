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
 * Builds an {@link IsotopeFinderEngine} from a {@link CarbonAveragineAlgorithmParameters} setup.
 * Single source of truth for the engine wiring so both {@link IsotopeFinderTask} (the processing run)
 * and the on-demand diagnostics recompute (compound dashboard, see
 * {@link IsotopeFinderDiagnostics}) build an identically configured engine.
 */
public final class IsotopeFinderEngineFactory {

  private IsotopeFinderEngineFactory() {
  }

  /**
   * Rebuild the engine of a finished run from its stored top-level parameters. Only for the
   * developer-only diagnostics recompute - the normal run gets its parameters from the algorithm that
   * created the task, see
   * {@link IsotopeFinderAlgorithmModule#createTasks(io.github.mzmine.datamodel.MZmineProject,
   * io.github.mzmine.datamodel.features.ModularFeatureList[], ParameterSet, ParameterSet,
   * java.time.Instant)}.
   *
   * @param parameters      an {@link IsotopeFinderParameters} value set.
   * @param keepDiagnostics developer-only: retain rich per-charge scoring diagnostics on the result.
   * @return the configured engine. The switch below is exhaustive on purpose: an algorithm that this
   * carbon-averagine engine cannot reproduce must decide here what the diagnostics should do.
   */
  public static @NotNull IsotopeFinderEngine createForDiagnostics(
      @NotNull final ParameterSet parameters, final boolean keepDiagnostics) {
    final ValueWithParameters<IsotopeFinderModeOptions> modeValue = parameters.getParameter(
        IsotopeFinderParameters.mode).getValueWithParameters();
    final CarbonAveragineAlgorithmParameters algo = switch (modeValue.value()) {
      case AUTOMATIC -> AutomaticIsotopeFinderParameters.toCarbonAveragineParameters(
          modeValue.parameters());
      case CARBON_AVERAGINE ->
          (CarbonAveragineAlgorithmParameters) modeValue.parameters().cloneParameterSet();
    };
    return create(algo, modeValue.value().toString(), keepDiagnostics);
  }

  /**
   * Build the engine from the full algorithm parameters.
   *
   * @param algo            the full carbon-averagine setup.
   * @param algorithmName   name of the selected algorithm, only used for reporting.
   * @param keepDiagnostics developer-only: retain rich per-charge scoring diagnostics on the result
   *                        (used by the compound dashboard review tooling). Off for the normal run.
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
