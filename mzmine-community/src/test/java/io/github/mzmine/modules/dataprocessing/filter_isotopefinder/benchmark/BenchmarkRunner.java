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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngine;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngineConfig;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single source of truth for running the isotope finder over benchmark cases, shared by the
 * baseline regenerator ({@link IsotopeBenchmarkMain}) and the regression test
 * ({@code IsotopeBenchmarkRegressionTest}) so both measure an identically configured engine.
 */
public final class BenchmarkRunner {

  private BenchmarkRunner() {
  }

  /**
   * Build a fresh engine for one case in signal / carbon-averagine mode (standalone, no
   * MZmineCore).
   *
   * @param c          the case, which carries the elements, tolerance and max charge.
   * @param requireC13 whether to enable the require-13C gate (and its gap-truncation).
   * @return the configured engine.
   */
  @NotNull
  public static IsotopeFinderEngine buildEngine(@NotNull final GroundTruthCase c,
      final boolean requireC13) {
    final EnvelopeModel model = new CarbonAveragineEnvelopeModel(
        CarbonAveragineEnvelopeParameters.createDefault(),
        new EnvelopeContext(c.elements(), c.tol()));
    return new IsotopeFinderEngine(
        IsotopeFinderEngineConfig.of(c.elements(), c.maxCharge(), c.tol(), model, "benchmark",
            requireC13));
  }

  /**
   * Run the engine over every case and compute the per-case metrics.
   * <p>
   * Every case is additionally re-detected from each start signal (monoisotopic / base / top true
   * peak) so {@link CaseMetrics#chargeStartInvariant()} measures the position-agnostic property
   * across the corpus rather than only from the base peak.
   *
   * @param cases      the cases to run.
   * @param requireC13 whether to enable the require-13C gate.
   * @param confusion  optional charge confusion matrix to fill, or null.
   * @return one {@link CaseMetrics} per case, in input order.
   */
  @NotNull
  public static List<CaseMetrics> run(@NotNull final List<GroundTruthCase> cases,
      final boolean requireC13, @Nullable final ChargeConfusionMatrix confusion) {
    final List<CaseMetrics> metrics = new ArrayList<>(cases.size());
    for (final GroundTruthCase c : cases) {
      final IsotopeFinderEngine engine = buildEngine(c, requireC13);
      final long t0 = System.nanoTime();
      final DetectionResult result = engine.detect(c.spectrum(), c.seedMz(), c.seedHeight(),
          c.polarity());
      final double ms = (System.nanoTime() - t0) / 1_000_000d;

      final List<double[]> seeds = IsotopeMetrics.startSeeds(c);
      final int[] seedCharges = new int[seeds.size()];
      for (int i = 0; i < seeds.size(); i++) {
        final DetectionResult r = engine.detect(c.spectrum(), seeds.get(i)[0], seeds.get(i)[1],
            c.polarity());
        seedCharges[i] = r == null ? 0 : r.bestCharge();
      }

      metrics.add(IsotopeMetrics.computeCase(c, result, seedCharges, ms));
      if (confusion != null) {
        confusion.add(c.trueCharge(), result == null ? 0 : result.bestCharge());
      }
    }
    return metrics;
  }

  /**
   * Run every case once, untimed, so the timed pass in {@link #run} reflects steady-state (JIT
   * warmed) cost.
   */
  public static void warmUp(@NotNull final List<GroundTruthCase> cases, final boolean requireC13) {
    for (final GroundTruthCase c : cases) {
      buildEngine(c, requireC13).detect(c.spectrum(), c.seedMz(), c.seedHeight(), c.polarity());
    }
  }
}
