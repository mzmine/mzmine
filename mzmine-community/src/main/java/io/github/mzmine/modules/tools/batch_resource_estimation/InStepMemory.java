package io.github.mzmine.modules.tools.batch_resource_estimation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Heap of a thread limited step on top of its retained data, split into the working memory of the
 * parallel tasks and the data that grows with the files while the step runs. The input and the
 * results of a step are both alive until the step removes its input, so this part grows with all
 * files even though only a limited number of tasks run in parallel.
 *
 * @param parallelGB working memory when all threads are busy
 * @param perFileGB  heap per file equivalent that is alive during the step in addition to the
 *                   retained data
 */
public record InStepMemory(double parallelGB, double perFileGB) {

  /**
   * Least squares line {@code y = parallelGB + perFileGB * x} with non-negative parameters.
   *
   * @param x file equivalents of runs that kept all threads busy
   * @param y heap of the step above the retained data and the baseline
   * @return null with fewer than two different x
   */
  static @Nullable InStepMemory fit(final double @NotNull [] x, final double @NotNull [] y) {
    final int n = x.length;
    double meanX = 0;
    double meanY = 0;
    for (int i = 0; i < n; i++) {
      meanX += x[i];
      meanY += y[i];
    }
    meanX /= n;
    meanY /= n;
    double covariance = 0;
    double varianceX = 0;
    for (int i = 0; i < n; i++) {
      covariance += (x[i] - meanX) * (y[i] - meanY);
      varianceX += (x[i] - meanX) * (x[i] - meanX);
    }
    if (n < 2 || varianceX == 0) {
      return null;
    }
    final double slope = covariance / varianceX;
    if (slope <= 0) {
      // the heap does not grow with the files, all of it is working memory of the threads
      return new InStepMemory(Math.max(0, meanY), 0);
    }
    final double intercept = meanY - slope * meanX;
    if (intercept < 0) {
      // decision: no negative working memory, the line goes through the origin instead
      double sumXY = 0;
      double sumXX = 0;
      for (int i = 0; i < n; i++) {
        sumXY += x[i] * y[i];
        sumXX += x[i] * x[i];
      }
      return new InStepMemory(0, sumXY / sumXX);
    }
    return new InStepMemory(intercept, slope);
  }

  /**
   * @param fileEquivalents dataset size in mean files
   * @param files           number of files, limits the parallel tasks
   * @param numThreads      number of threads
   */
  public double predict(final double fileEquivalents, final int files, final int numThreads) {
    // fewer files than threads only use some of the threads
    final double busyThreads = Math.min(1, files / (double) numThreads);
    return parallelGB * busyThreads + perFileGB * fileEquivalents;
  }
}
