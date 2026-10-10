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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatchExecutionBudgetTest {

  @Test
  void reservesExactlyTheConfiguredNumberOfBatches() {
    final BatchExecutionBudget budget = new BatchExecutionBudget(2);

    Assertions.assertEquals(0, budget.count());
    Assertions.assertEquals(1, budget.reserve());
    Assertions.assertEquals(2, budget.reserve());
    Assertions.assertThrows(BatchExecutionLimitReachedException.class, budget::reserve);
    Assertions.assertEquals(2, budget.count(), "a rejected reservation must not overshoot");
  }

  @Test
  void rejectsANonPositiveBudget() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new BatchExecutionBudget(0));
  }
}
