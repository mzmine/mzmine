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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reserves full batch executions without allowing a generational optimizer to exceed its budget.
 */
final class BatchExecutionBudget {

  private final int maximum;
  private final AtomicInteger executed = new AtomicInteger();

  BatchExecutionBudget(int maximum) {
    if (maximum <= 0) {
      throw new IllegalArgumentException("maximum must be positive");
    }
    this.maximum = maximum;
  }

  /**
   * Reserves the next batch execution before it is launched.
   *
   * @return the one-based batch execution index
   * @throws BatchExecutionLimitReachedException if the complete budget is already reserved
   */
  int reserve() {
    final int previous = executed.getAndUpdate(
        current -> current < maximum ? current + 1 : current);
    if (previous >= maximum) {
      throw new BatchExecutionLimitReachedException(maximum);
    }
    return previous + 1;
  }

  int count() {
    return executed.get();
  }

  int maximum() {
    return maximum;
  }
}
