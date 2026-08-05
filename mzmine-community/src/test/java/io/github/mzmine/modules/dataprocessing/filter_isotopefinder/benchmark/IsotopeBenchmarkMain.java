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

import java.nio.file.Path;
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
    BenchmarkRunner.warmUp(cases, requireC13);

    final ChargeConfusionMatrix confusion = new ChargeConfusionMatrix();
    final List<CaseMetrics> metrics = BenchmarkRunner.run(cases, requireC13, confusion);

    final List<MetricRow> rows = IsotopeMetrics.aggregateByAxis(metrics);
    BenchmarkReport.writeCsv(rows, out);

    LOGGER.info("Wrote baseline CSV to " + out.toAbsolutePath());
    LOGGER.info(System.lineSeparator() + BenchmarkReport.renderConsole(rows));
    LOGGER.info(System.lineSeparator() + confusion.render());
    LOGGER.info(String.format(Locale.ROOT,
        "Charge error rates: harmonic=%.4f (z read as 2z or z/2), neighbour=%.4f (z read as z+-1)",
        confusion.harmonicRate(), confusion.neighbourRate()));
  }
}
