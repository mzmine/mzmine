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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Renders benchmark {@link MetricRow}s to a committed CSV baseline and to an aligned console table,
 * and reads a committed baseline back so a regression test can diff against it.
 * <p>
 * Values are written with a fixed decimal format under {@link Locale#ROOT} so the baseline is
 * stable across locales and produces small diffs. Timing ({@link MetricRow#medianDetectMs()}) is
 * inherently machine-dependent and is informational only; no test asserts equality against it.
 */
public final class BenchmarkReport {

  /**
   * Default repository-root-relative location of the committed baseline CSV, used when
   * {@code IsotopeBenchmarkMain} is run without an explicit output path. Prefer
   * {@link #BASELINE_RESOURCE} when only reading - it does not depend on the working directory.
   */
  public static final Path DEFAULT_BASELINE = Path.of("mzmine-community", "src", "test",
      "resources", "isotopefinder", "baseline", "metrics_baseline.csv");

  /**
   * Classpath location of the committed baseline CSV (working-directory independent).
   */
  public static final String BASELINE_RESOURCE = "isotopefinder/baseline/metrics_baseline.csv";

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
   * Read the committed baseline from the test classpath ({@link #BASELINE_RESOURCE}).
   *
   * @return the baseline rows in file order ({@code ALL} last).
   */
  @NotNull
  public static List<MetricRow> readBaseline() {
    final InputStream in = BenchmarkReport.class.getClassLoader()
        .getResourceAsStream(BASELINE_RESOURCE);
    if (in == null) {
      throw new IllegalStateException("Baseline CSV not found on classpath: " + BASELINE_RESOURCE);
    }
    try (final BufferedReader reader = new BufferedReader(
        new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return parseCsv(reader);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read baseline CSV: " + BASELINE_RESOURCE, e);
    }
  }

  /**
   * Read a baseline CSV written by {@link #writeCsv(List, Path)} from a filesystem path.
   */
  @NotNull
  public static List<MetricRow> readCsv(@NotNull final Path path) {
    try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return parseCsv(reader);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read baseline CSV: " + path, e);
    }
  }

  /**
   * Index rows by their axis label, preserving file order.
   */
  @NotNull
  public static Map<String, MetricRow> byAxis(@NotNull final List<MetricRow> rows) {
    final Map<String, MetricRow> map = new LinkedHashMap<>();
    for (final MetricRow r : rows) {
      map.put(r.axis(), r);
    }
    return map;
  }

  @NotNull
  private static List<MetricRow> parseCsv(@NotNull final BufferedReader reader) throws IOException {
    final List<MetricRow> rows = new ArrayList<>();
    String line = reader.readLine(); // header
    if (line == null) {
      throw new IllegalStateException("Baseline CSV is empty");
    }
    while ((line = reader.readLine()) != null) {
      if (line.isBlank()) {
        continue;
      }
      final String[] parts = line.split(",", -1);
      if (parts.length != HEADER.length) {
        throw new IllegalStateException(
            "Baseline CSV row has " + parts.length + " columns, expected " + HEADER.length + ": "
                + line);
      }
      rows.add(new MetricRow(parts[0], Integer.parseInt(parts[1].trim()), val(parts[2]),
          val(parts[3]), val(parts[4]), val(parts[5]), val(parts[6]), val(parts[7]), val(parts[8]),
          val(parts[9]), val(parts[10]), val(parts[11]), val(parts[12]), val(parts[13]),
          val(parts[14])));
    }
    return rows;
  }

  /**
   * Parse one CSV cell written by {@link #num(double)} / {@link #time(double)}; {@code "NaN"} maps
   * back to {@link Double#NaN}.
   */
  private static double val(@NotNull final String cell) {
    final String trimmed = cell.trim();
    return "NaN".equals(trimmed) ? Double.NaN : Double.parseDouble(trimmed);
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
