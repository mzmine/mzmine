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

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.BenchmarkReport;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.BenchmarkRunner;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.CaseMetrics;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.ChargeConfusionMatrix;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.GroundTruthCase;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.IsotopeCorpus;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.IsotopeMetrics;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.MetricRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Full-corpus accuracy guard: runs the current engine over every committed case and diffs the
 * per-axis metrics against both committed baselines, one test each. This is what makes those
 * baselines guards rather than documentation.
 * <p>
 * Tagged {@code benchmark}, so it is excluded from the default {@code test} task - each test runs
 * ~5900 cases x 4 detections, far too slow for CI, where {@link IsotopeAccuracyTest} guards instead.
 * <p>
 * When a change intentionally moves the numbers, regenerate BOTH baselines
 * ({@code isotopeBenchmark}, and the same task with {@code --args="<path> requireC13"}).
 */
@Tag("benchmark")
class IsotopeBenchmarkRegressionTest {

  private static final Logger logger = Logger.getLogger(
      IsotopeBenchmarkRegressionTest.class.getName());

  /**
   * Allowed absolute drop below the baseline. Well above the four-decimal rounding noise: it
   * absorbs incidental jitter, not a real regression.
   */
  private static final double TOLERANCE = 0.01;

  /**
   * Metrics where higher is better, i.e. the asserted ones. Deliberately excluded:
   * {@code scoreMargin} / {@code aucCharge} are separation diagnostics that legitimately shift with
   * the scoring formula, {@code medianDetectMs} is machine-dependent, and
   * {@code elementPrecision} / {@code elementSetSize} move with the size of the reported ambiguity
   * set rather than with detection quality - asserting precision would fail every deliberate
   * widening. {@code elementContainment} is the guarded element property; watch the rest in the
   * printed table.
   */
  private static final List<Metric> HIGHER_IS_BETTER = List.of(
      new Metric("chargeTop1", MetricRow::chargeTop1),
      new Metric("chargeRecallAlt", MetricRow::chargeRecallAlt),
      new Metric("chargeStartInvariance", MetricRow::chargeStartInvariance),
      new Metric("patternPrecision", MetricRow::patternPrecision),
      new Metric("patternRecall", MetricRow::patternRecall),
      new Metric("patternF1", MetricRow::patternF1),
      new Metric("borderlineRecall", MetricRow::borderlineRecall),
      new Metric("elementRecall", MetricRow::elementRecall),
      new Metric("elementContainment", MetricRow::elementContainment));

  private static final List<Metric> LOWER_IS_BETTER = List.of(
      new Metric("noiseLeak", MetricRow::noiseLeak));

  @Test
  void doesNotRegressAgainstCommittedBaseline() {
    assertNoRegression(false, BenchmarkReport.BASELINE_RESOURCE);
  }

  /**
   * The same guard for the require-13C mode. Its gate and gap-truncation are a separate code path
   * the default run never exercises, so without this the companion baseline would be documentation
   * only and a change to the ladder walk or the M+1/M gate could regress unnoticed.
   */
  @Test
  void requireC13DoesNotRegressAgainstCommittedBaseline() {
    assertNoRegression(true, BenchmarkReport.REQUIRE_C13_BASELINE_RESOURCE);
  }

  /**
   * Run the whole corpus and diff every guarded per-axis metric against {@code baselineResource}.
   */
  private static void assertNoRegression(final boolean requireC13,
      @NotNull final String baselineResource) {
    final List<GroundTruthCase> cases = IsotopeCorpus.all();
    Assertions.assertFalse(cases.isEmpty(), "benchmark corpus is empty");

    BenchmarkRunner.warmUp(cases, requireC13);
    final ChargeConfusionMatrix confusion = new ChargeConfusionMatrix();
    final List<CaseMetrics> metrics = BenchmarkRunner.run(cases, requireC13, confusion);
    final List<MetricRow> current = IsotopeMetrics.aggregateByAxis(metrics);

    logger.info(
        System.lineSeparator() + "requireC13=" + requireC13 + System.lineSeparator()
            + BenchmarkReport.renderConsole(current));
    logger.info(System.lineSeparator() + confusion.render());
    logger.info(String.format(Locale.ROOT,
        "Charge error rates: harmonic=%.4f (z read as 2z or z/2), neighbour=%.4f (z read as z+-1)",
        confusion.harmonicRate(), confusion.neighbourRate()));

    final Map<String, MetricRow> baseline = BenchmarkReport.byAxis(
        BenchmarkReport.readBaseline(baselineResource));
    final Map<String, MetricRow> now = BenchmarkReport.byAxis(current);

    // collect every violation, so one run reports the full picture rather than the first failure
    final List<String> problems = new ArrayList<>();

    for (final String axis : baseline.keySet()) {
      final MetricRow expected = baseline.get(axis);
      final MetricRow actual = now.get(axis);
      if (actual == null) {
        problems.add(
            "axis '" + axis + "' is in the baseline but not in the current run - regenerate the "
                + "baseline after changing the corpus");
        continue;
      }
      if (expected.nCases() != actual.nCases()) {
        problems.add(String.format(Locale.ROOT,
            "%-18s nCases %d -> %d (corpus changed; regenerate the baseline)", axis,
            expected.nCases(), actual.nCases()));
      }
      for (final Metric m : HIGHER_IS_BETTER) {
        check(problems, axis, m, expected, actual, true);
      }
      for (final Metric m : LOWER_IS_BETTER) {
        check(problems, axis, m, expected, actual, false);
      }
    }
    for (final String axis : now.keySet()) {
      if (!baseline.containsKey(axis)) {
        problems.add("axis '" + axis
            + "' is new in this run but missing from the baseline - regenerate the baseline");
      }
    }

    Assertions.assertTrue(problems.isEmpty(), () -> String.format(Locale.ROOT,
        "Isotope finder accuracy regressed against %s (tolerance %.3f).%n"
            + "Regenerate with ./gradlew :mzmine-community:isotopeBenchmark%s if the change is "
            + "intended.%n%s", baselineResource, TOLERANCE,
        requireC13 ? " --args=\"<path> requireC13\"" : "",
        String.join(System.lineSeparator(), problems)));
  }

  /**
   * Record a violation when {@code actual} moved the wrong way by more than {@link #TOLERANCE}. A
   * NaN baseline means the metric was undefined for the whole axis and is skipped, but a NaN NOW
   * where the baseline had a value is itself a regression - the metric stopped being measurable.
   */
  private static void check(@NotNull final List<String> problems, @NotNull final String axis,
      @NotNull final Metric metric, @NotNull final MetricRow expected,
      @NotNull final MetricRow actual, final boolean higherIsBetter) {
    final double want = metric.extractor().applyAsDouble(expected);
    if (Double.isNaN(want)) {
      return;
    }
    final double got = metric.extractor().applyAsDouble(actual);
    if (Double.isNaN(got)) {
      problems.add(String.format(Locale.ROOT, "%-18s %-22s %.4f -> NaN (no longer measurable)", axis,
          metric.name(), want));
      return;
    }
    final double delta = higherIsBetter ? want - got : got - want;
    if (delta > TOLERANCE) {
      problems.add(
          String.format(Locale.ROOT, "%-18s %-22s %.4f -> %.4f (%+.4f)", axis, metric.name(), want,
              got, got - want));
    }
  }

  private record Metric(@NotNull String name, @NotNull ToDoubleFunction<MetricRow> extractor) {

  }
}
