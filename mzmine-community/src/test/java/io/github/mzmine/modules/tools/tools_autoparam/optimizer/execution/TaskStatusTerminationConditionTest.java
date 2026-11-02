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

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.TwoRealProblem;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.algorithm.NSGAII;

class TaskStatusTerminationConditionTest {

  @Test
  void terminatesAtTheFullBatchBudget() {
    final AtomicInteger batches = new AtomicInteger();
    final AtomicReference<TaskStatus> status = new AtomicReference<>(TaskStatus.PROCESSING);
    final TaskStatusTerminationCondition condition = new TaskStatusTerminationCondition(3, 10,
        batches::get, status::get);
    final NSGAII algorithm = new NSGAII(new TwoRealProblem());
    condition.initialize(algorithm);

    batches.set(2);
    Assertions.assertFalse(condition.shouldTerminate(algorithm));
    Assertions.assertEquals(200d / 3d, condition.getPercentComplete(algorithm), 1e-9);

    batches.set(3);
    Assertions.assertTrue(condition.shouldTerminate(algorithm));
    Assertions.assertEquals(100d, condition.getPercentComplete(algorithm));
  }

  @Test
  void duplicateProposalsDoNotConsumeTheBatchBudget() {
    final AtomicInteger batches = new AtomicInteger(1);
    final TaskStatusTerminationCondition condition = new TaskStatusTerminationCondition(2, 5,
        batches::get, () -> TaskStatus.PROCESSING);
    final TwoRealProblem problem = new TwoRealProblem();
    final NSGAII algorithm = new NSGAII(problem);
    condition.initialize(algorithm);

    for (int i = 0; i < 4; i++) {
      algorithm.evaluate(problem.newSolution());
    }
    Assertions.assertFalse(condition.shouldTerminate(algorithm),
        "cheap proposals must not masquerade as full batches");

    algorithm.evaluate(problem.newSolution());
    Assertions.assertTrue(condition.shouldTerminate(algorithm),
        "the independent proposal cap must still stop a duplicate loop");
    Assertions.assertEquals(1, batches.get());
  }

  @Test
  void cancellationAndErrorsTerminateImmediately() {
    final AtomicReference<TaskStatus> status = new AtomicReference<>(TaskStatus.PROCESSING);
    final TaskStatusTerminationCondition condition = new TaskStatusTerminationCondition(3, 10,
        () -> 0, status::get);
    final NSGAII algorithm = new NSGAII(new TwoRealProblem());
    condition.initialize(algorithm);

    status.set(TaskStatus.CANCELED);
    Assertions.assertTrue(condition.shouldTerminate(algorithm));

    status.set(TaskStatus.ERROR);
    Assertions.assertTrue(condition.shouldTerminate(algorithm));
  }

  @Test
  void userStopTerminatesWithoutCancelingTheTask() {
    final AtomicBoolean stopRequested = new AtomicBoolean();
    final TaskStatusTerminationCondition condition = new TaskStatusTerminationCondition(3, 10,
        () -> 1, () -> TaskStatus.PROCESSING, stopRequested::get);
    final NSGAII algorithm = new NSGAII(new TwoRealProblem());
    condition.initialize(algorithm);

    Assertions.assertFalse(condition.shouldTerminate(algorithm));
    stopRequested.set(true);
    Assertions.assertTrue(condition.shouldTerminate(algorithm));
  }

  @Test
  void proposalCapCannotBeLowerThanTheBatchBudget() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new TaskStatusTerminationCondition(3, 2, () -> 0, () -> TaskStatus.PROCESSING));
  }
}
