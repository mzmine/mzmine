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
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Baseline regenerator for the isotope-finder benchmark: runs the CURRENT engine (signal /
 * carbon-averagine mode, {@code requireC13 = false}) over the whole committed corpus, measures the
 * accuracy metrics, and writes the committed baseline CSV. Also prints an aligned metrics table,
 * the charge confusion matrix, and the harmonic/neighbour charge-error rates.
 * <p>
 * Run via the {@code isotopeBenchmark} Gradle task (which supplies the output path and the test
 * runtime classpath with {@code --enable-preview}). The first program argument overrides the output
 * CSV path; without it {@link BenchmarkReport#DEFAULT_BASELINE} is used. An optional second argument
 * {@code "requireC13"} enables the require-13C gate (and its gap-truncation) so a companion baseline
 * can be produced and diffed against the default one.
 */
public final class IsotopeBenchmarkMain {

  private static final Logger LOGGER = Logger.getLogger(IsotopeBenchmarkMain.class.getName());

  private IsotopeBenchmarkMain() {
  }

  public static void main(@NotNull final String[] args) {
    final Path out = args.length > 0 ? Path.of(args[0]) : BenchmarkReport.DEFAULT_BASELINE;
    final boolean requireC13 = args.length > 1 && "requireC13".equalsIgnoreCase(args[1]);

    final List<GroundTruthCase> cases = IsotopeCorpus.all();
    LOGGER.info(
        "Loaded " + cases.size() + " benchmark cases; running the current engine" + (requireC13
            ? " with requireC13=true." : "."));

    // JIT warmup: run the whole corpus once untimed so the timed pass reflects steady-state cost
    for (final GroundTruthCase c : cases) {
      buildEngine(c, requireC13).detect(c.spectrum(), c.seedMz(), c.seedHeight(), c.polarity());
    }

    final List<CaseMetrics> metrics = new ArrayList<>(cases.size());
    final ChargeConfusionMatrix confusion = new ChargeConfusionMatrix();
    for (final GroundTruthCase c : cases) {
      final IsotopeFinderEngine engine = buildEngine(c, requireC13);
      final long t0 = System.nanoTime();
      final DetectionResult result = engine.detect(c.spectrum(), c.seedMz(), c.seedHeight(),
          c.polarity());
      final double ms = (System.nanoTime() - t0) / 1_000_000d;

      // start-signal invariance: re-run the finder seeded from each start signal (mono / base / top
      // true peak) and record the winning charge from each, so the position-agnostic property is
      // measured across the whole corpus rather than only from the base peak.
      final List<double[]> seeds = IsotopeMetrics.startSeeds(c);
      final int[] seedCharges = new int[seeds.size()];
      for (int i = 0; i < seeds.size(); i++) {
        final DetectionResult r = engine.detect(c.spectrum(), seeds.get(i)[0], seeds.get(i)[1],
            c.polarity());
        seedCharges[i] = r == null ? 0 : r.bestCharge();
      }

      metrics.add(IsotopeMetrics.computeCase(c, result, seedCharges, ms));
      confusion.add(c.trueCharge(), result == null ? 0 : result.bestCharge());
    }

    final List<MetricRow> rows = IsotopeMetrics.aggregateByAxis(metrics);
    BenchmarkReport.writeCsv(rows, out);

    LOGGER.info("Wrote baseline CSV to " + out.toAbsolutePath());
    LOGGER.info(System.lineSeparator() + BenchmarkReport.renderConsole(rows));
    LOGGER.info(System.lineSeparator() + confusion.render());
    LOGGER.info(String.format(Locale.ROOT,
        "Charge error rates: harmonic=%.4f (z read as 2z or z/2), neighbour=%.4f (z read as z+-1)",
        confusion.harmonicRate(), confusion.neighbourRate()));
  }

  /**
   * Build a fresh engine for one case in signal / carbon-averagine mode, exactly as
   * {@code IsotopeFinderEngineTest} does (standalone, no MZmineCore).
   *
   * @param requireC13 whether to enable the require-13C gate (and its gap-truncation).
   */
  @NotNull
  private static IsotopeFinderEngine buildEngine(@NotNull final GroundTruthCase c,
      final boolean requireC13) {
    final EnvelopeModel model = new CarbonAveragineEnvelopeModel(
        CarbonAveragineEnvelopeParameters.createDefault(),
        new EnvelopeContext(c.elements(), c.tol()));
    return new IsotopeFinderEngine(c.elements(), c.maxCharge(), c.tol(), model, "benchmark",
        requireC13);
  }
}
