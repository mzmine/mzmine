/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

/**
 * Opt-in local benchmark for the exhaustive QC-RLSC candidate search; it is deliberately not a
 * timing assertion. Run after test compilation with this class as the main entry point.
 */
public final class DunnQcRlscBenchmark {

  private DunnQcRlscBenchmark() {
  }

  public static void main(final String[] args) {
    for (final int count : new int[]{10, 20, 40}) {
      final double[] x = new double[count];
      final double[] y = new double[count];
      for (int index = 0; index < count; index++) {
        x[index] = index;
        y[index] = 100d + 12d * Math.sin(index * .37d) + index * .8d;
      }
      for (int warmup = 0; warmup < 5; warmup++) {
        DunnQcRlsc.fit(x, y, 2, () -> false);
      }
      final int repetitions = 20;
      final long start = System.nanoTime();
      for (int repetition = 0; repetition < repetitions; repetition++) {
        DunnQcRlsc.fit(x, y, 2, () -> false);
      }
      final double milliseconds = (System.nanoTime() - start) / 1_000_000d / repetitions;
      System.out.printf("QC-RLSC n=%d: %.3f ms/fit%n", count, milliseconds);
    }
  }
}
