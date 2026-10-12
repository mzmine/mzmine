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

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jetbrains.annotations.NotNull;

/**
 * Accumulates a {@code (trueCharge -> predictedBestCharge)} confusion matrix over the benchmark
 * cases. A predicted charge of {@code 0} means the engine returned no detection.
 * <p>
 * Beyond the raw table it exposes two diagnostics that target the classic isotope-finder failure
 * modes: {@link #harmonicRate()} (calling a z as its double or half) and {@link #neighbourRate()}
 * (off-by-one charge, common for high-charge protein envelopes at a given resolution).
 */
public final class ChargeConfusionMatrix {

  // counts[trueZ][predZ]
  private final Map<Integer, Map<Integer, Integer>> counts = new TreeMap<>();
  private long total = 0L;

  /**
   * Record one case outcome.
   *
   * @param trueZ the true charge (>= 1 for real cases)
   * @param predZ the predicted best charge, or 0 when the engine returned no detection
   */
  public void add(final int trueZ, final int predZ) {
    counts.computeIfAbsent(trueZ, k -> new TreeMap<>()).merge(predZ, 1, Integer::sum);
    total++;
  }

  private int count(final int trueZ, final int predZ) {
    final Map<Integer, Integer> row = counts.get(trueZ);
    if (row == null) {
      return 0;
    }
    return row.getOrDefault(predZ, 0);
  }

  /**
   * Fraction of all cases where the prediction is a harmonic of the truth: predicted == 2*true or
   * predicted == true/2 (only for even true charges). Requires trueZ > 0.
   */
  public double harmonicRate() {
    if (total == 0L) {
      return 0d;
    }
    long harmonic = 0L;
    for (final var trueEntry : counts.entrySet()) {
      final int trueZ = trueEntry.getKey();
      if (trueZ <= 0) {
        continue;
      }
      for (final var predEntry : trueEntry.getValue().entrySet()) {
        final int predZ = predEntry.getKey();
        final boolean isDouble = predZ == 2 * trueZ;
        final boolean isHalf = trueZ % 2 == 0 && predZ == trueZ / 2;
        if (predZ > 0 && (isDouble || isHalf)) {
          harmonic += predEntry.getValue();
        }
      }
    }
    return (double) harmonic / total;
  }

  /**
   * Fraction of all cases where the prediction is an off-by-one neighbour of the truth: predicted
   * == true ± 1. Requires trueZ > 0 and predZ > 0.
   */
  public double neighbourRate() {
    if (total == 0L) {
      return 0d;
    }
    long neighbour = 0L;
    for (final var trueEntry : counts.entrySet()) {
      final int trueZ = trueEntry.getKey();
      if (trueZ <= 0) {
        continue;
      }
      for (final var predEntry : trueEntry.getValue().entrySet()) {
        final int predZ = predEntry.getKey();
        if (predZ > 0 && Math.abs(predZ - trueZ) == 1) {
          neighbour += predEntry.getValue();
        }
      }
    }
    return (double) neighbour / total;
  }

  /**
   * Render the matrix as an aligned text table with true charges as rows and predicted charges as
   * columns (column {@code z=0} = no detection). The diagonal is the correct count.
   */
  @NotNull
  public String render() {
    // union of all predicted columns across all rows (plus every true charge, so the diagonal shows)
    final TreeSet<Integer> cols = new TreeSet<>();
    for (final var trueEntry : counts.entrySet()) {
      cols.add(trueEntry.getKey());
      cols.addAll(trueEntry.getValue().keySet());
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("Charge confusion matrix (rows = true z, cols = predicted z; z=0 = no detection)\n");
    sb.append(String.format("%8s", "true\\pred"));
    for (final int c : cols) {
      sb.append(String.format("%6s", "z=" + c));
    }
    sb.append(String.format("%8s", "n"));
    sb.append('\n');

    for (final var trueEntry : counts.entrySet()) {
      final int trueZ = trueEntry.getKey();
      sb.append(String.format("%8s", "z=" + trueZ));
      int rowTotal = 0;
      for (final int c : cols) {
        final int v = count(trueZ, c);
        rowTotal += v;
        sb.append(String.format("%6d", v));
      }
      sb.append(String.format("%8d", rowTotal));
      sb.append('\n');
    }
    return sb.toString();
  }
}
