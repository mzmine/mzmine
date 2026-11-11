/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.main.MZmineCore;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;

class BatchOptimizationMainTaskInitialUserSettingsTest {

  @BeforeAll
  static void initializeConfiguration() {
    MZmineCore.getConfiguration().getPreferences();
  }

  @Test
  void simpleOptimizerSetupDoesNotSpendAnExtraBaselineBatch() {
    final OptimizerParameters parameters = OptimizerParameters.create(
        new SimpleOptimizerParameters());

    assertFalse(parameters.getValue(OptimizerParameters.evaluateInitialUserSettings));
  }

  @Test
  void retainsCompletedObservationsWhenTheOptimizerStopsAtTheBatchBoundary() {
    final Solution estimate = new Solution(0, 2);
    estimate.setObjectiveValue(0, 42d);
    estimate.setObjectiveValue(1, 10d);
    final Solution completedObservation = new Solution(0, 2);
    completedObservation.setObjectiveValue(0, 43d);
    completedObservation.setObjectiveValue(1, 9d);

    final var front = BatchOptimizationMainTask.createSearchFront(List.of(estimate),
        List.of(completedObservation));

    assertEquals(2, front.size());
    assertTrue(front.contains(estimate));
    assertTrue(front.contains(completedObservation));
  }
}
