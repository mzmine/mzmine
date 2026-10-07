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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;
import org.moeaframework.core.population.NondominatedPopulation;

/**
 * Reports the evaluated baseline and optimization results.
 */
public final class OptimizationResultLogger {

  private static final Logger logger = Logger.getLogger(OptimizationResultLogger.class.getName());

  private OptimizationResultLogger() {
  }

  public static void logResults(@NotNull Solution solution, @NotNull PreparedParameterSet estimates,
      @NotNull List<SweepMetric> enabledMetrics) {
    final StringBuilder sb = new StringBuilder("Single-pass parameter estimation results:\n");
    sb.append("  Estimated parameters:\n");
    sb.append(estimates.describe()).append("\n");
    sb.append("  Metric scores:\n");
    for (int i = 0; i < enabledMetrics.size(); i++) {
      sb.append(
          "    %s = %.6f%n".formatted(enabledMetrics.get(i).name(), solution.getObjectiveValue(i)));
    }
    logger.info(sb.toString());
  }

  /**
   * Logs comparison between single-pass results and the best optimized solution, selected the same
   * way as in the results window, see {@link FrontSolutionRanker#selectBest(List, List)}.
   */
  public static void logComparison(@NotNull Solution singlePass,
      @NotNull NondominatedPopulation result, @NotNull List<SweepMetric> enabledMetrics) {
    final Solution best = FrontSolutionRanker.selectBest(result.asList(), enabledMetrics);

    final StringBuilder sb = new StringBuilder("\n=== Single-pass vs optimized comparison ===\n");
    sb.append("  %-30s  %15s  %15s%n".formatted("Metric", "Single-pass", "Optimized best"));
    sb.append("  %-30s  %15s  %15s%n".formatted("-".repeat(30), "-".repeat(15), "-".repeat(15)));
    for (int i = 0; i < enabledMetrics.size(); i++) {
      final double spValue = singlePass.getObjectiveValue(i);
      final double bestValue = best != null ? best.getObjectiveValue(i) : Double.NaN;
      sb.append(
          "  %-30s  %15.4f  %15.4f%n".formatted(enabledMetrics.get(i).name(), spValue, bestValue));
    }
    sb.append("  Best: average rank across all metrics, ties by %s%n".formatted(
        enabledMetrics.get(FrontSolutionRanker.tieBreakObjectiveIndex(enabledMetrics)).name()));
    sb.append("  Pareto front size: %d%n".formatted(result.size()));
    logger.info(sb.toString());
  }

}
