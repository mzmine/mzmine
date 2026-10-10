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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.termination.MaxFunctionEvaluations;

public class TaskStatusTerminationCondition extends MaxFunctionEvaluations {

  private final int maxBatchExecutions;
  private final @NotNull IntSupplier batchExecutionSupplier;
  private final @NotNull Supplier<TaskStatus> statusSupplier;
  private final @NotNull BooleanSupplier stopSearchRequestedSupplier;

  /**
   * Stops at the real batch budget, a proposal safety cap, cancellation, or task failure.
   *
   * @param maxBatchExecutions     maximum number of uncached full batch executions
   * @param maxProposals           maximum number of calls accepted by the algorithm
   * @param batchExecutionSupplier current number of full batch executions
   * @param statusSupplier         current task status
   */
  public TaskStatusTerminationCondition(int maxBatchExecutions, int maxProposals,
      @NotNull IntSupplier batchExecutionSupplier, @NotNull Supplier<TaskStatus> statusSupplier) {
    this(maxBatchExecutions, maxProposals, batchExecutionSupplier, statusSupplier, () -> false);
  }

  /**
   * Stops at the real batch budget, a proposal safety cap, cancellation, task failure, or a
   * user-requested graceful stop.
   *
   * @param maxBatchExecutions          maximum number of uncached full batch executions
   * @param maxProposals                maximum number of calls accepted by the algorithm
   * @param batchExecutionSupplier      current number of full batch executions
   * @param statusSupplier              current task status
   * @param stopSearchRequestedSupplier whether the user requested a graceful stop
   */
  public TaskStatusTerminationCondition(int maxBatchExecutions, int maxProposals,
      @NotNull IntSupplier batchExecutionSupplier, @NotNull Supplier<TaskStatus> statusSupplier,
      @NotNull BooleanSupplier stopSearchRequestedSupplier) {
    super(maxProposals);
    if (maxBatchExecutions <= 0) {
      throw new IllegalArgumentException("maxBatchExecutions must be positive");
    }
    if (maxProposals < maxBatchExecutions) {
      throw new IllegalArgumentException("maxProposals must not be lower than maxBatchExecutions");
    }
    this.maxBatchExecutions = maxBatchExecutions;
    this.batchExecutionSupplier = batchExecutionSupplier;
    this.statusSupplier = statusSupplier;
    this.stopSearchRequestedSupplier = stopSearchRequestedSupplier;
  }

  @Override
  public boolean shouldTerminate(@NotNull Algorithm algorithm) {
    final TaskStatus status = statusSupplier.get();
    return batchExecutionSupplier.getAsInt() >= maxBatchExecutions || super.shouldTerminate(
        algorithm) || stopSearchRequestedSupplier.getAsBoolean() || status == TaskStatus.CANCELED
        || status == TaskStatus.ERROR;
  }

  @Override
  public double getPercentComplete(@NotNull Algorithm algorithm) {
    return Math.min(100d, 100d * batchExecutionSupplier.getAsInt() / (double) maxBatchExecutions);
  }
}
