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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * Renders benchmark {@link MetricRow}s to a committed CSV baseline and to an aligned console
 * table.
 * <p>
 * Values are written with a fixed decimal format under {@link Locale#ROOT} so the baseline is
 * stable across locales and produces small diffs. Timing ({@link MetricRow#medianDetectMs()}) is
 * inherently machine-dependent and is informational only; no test asserts equality against it.
 */
public final class BenchmarkReport {

  /**
   * Default classpath-relative location of the committed baseline CSV.
   */
  public static final Path DEFAULT_BASELINE = Path.of("mzmine-community", "src", "test",
      "resources", "isotopefinder", "baseline", "metrics_baseline.csv");

  private static final String[] HEADER = {"axis", "nCases", "chargeTop1", "chargeRecallAlt",
      "chargeStartInvariance", "patternPrecision", "patternRecall", "patternF1", "borderlineRecall",
      "noiseLeak", "elementPrecision", "elementRecall", "scoreMargin", "aucCharge",
      "medianDetectMs"};

  private BenchmarkReport() {
  }

  /**
   * Write the rows to {@code path} as CSV (header + one row per axis, {@code ALL} last), creating
   * parent directories as needed.
   */
  public static void writeCsv(@NotNull final List<MetricRow> rows, @NotNull final Path path) {
    final StringBuilder sb = new StringBuilder();
    sb.append(String.join(",", HEADER)).append('\n');
    for (final MetricRow r : rows) {
      sb.append(r.axis()).append(',');
      sb.append(r.nCases()).append(',');
      sb.append(num(r.chargeTop1())).append(',');
      sb.append(num(r.chargeRecallAlt())).append(',');
      sb.append(num(r.chargeStartInvariance())).append(',');
      sb.append(num(r.patternPrecision())).append(',');
      sb.append(num(r.patternRecall())).append(',');
      sb.append(num(r.patternF1())).append(',');
      sb.append(num(r.borderlineRecall())).append(',');
      sb.append(num(r.noiseLeak())).append(',');
      sb.append(num(r.elementPrecision())).append(',');
      sb.append(num(r.elementRecall())).append(',');
      sb.append(num(r.scoreMargin())).append(',');
      sb.append(num(r.aucCharge())).append(',');
      sb.append(time(r.medianDetectMs())).append('\n');
    }
    try {
      final Path parent = path.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to write baseline CSV: " + path, e);
    }
  }

  /**
   * Render the rows as an aligned, fixed-width console table (headers abbreviated to keep the width
   * readable).
   */
  @NotNull
  public static String renderConsole(@NotNull final List<MetricRow> rows) {
    final String fmt = "%-18s%6s%8s%8s%8s%8s%8s%8s%8s%8s%8s%8s%9s%8s%9s%n";
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format(fmt, "axis", "n", "chgT1", "chgAlt", "invar", "patP", "patR", "patF1",
        "bordR", "noise", "elemP", "elemR", "scMargin", "auc", "medMs"));
    for (final MetricRow r : rows) {
      sb.append(String.format(fmt, r.axis(), Integer.toString(r.nCases()), cell(r.chargeTop1()),
          cell(r.chargeRecallAlt()), cell(r.chargeStartInvariance()), cell(r.patternPrecision()),
          cell(r.patternRecall()), cell(r.patternF1()), cell(r.borderlineRecall()),
          cell(r.noiseLeak()), cell(r.elementPrecision()), cell(r.elementRecall()),
          cell(r.scoreMargin()), cell(r.aucCharge()), timeCell(r.medianDetectMs())));
    }
    return sb.toString();
  }

  @NotNull
  private static String num(final double v) {
    return Double.isNaN(v) ? "NaN" : String.format(Locale.ROOT, "%.4f", v);
  }

  @NotNull
  private static String time(final double v) {
    return Double.isNaN(v) ? "NaN" : String.format(Locale.ROOT, "%.3f", v);
  }

  @NotNull
  private static String cell(final double v) {
    return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.3f", v);
  }

  @NotNull
  private static String timeCell(final double v) {
    return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.2f", v);
  }
}
