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

package io.github.mzmine.modules.batchmode.order;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies batch steps that can act as anchors for a relative module-order rule.
 */
public interface ModuleOrderCondition {

  /**
   * Human-readable description of the matching anchor, without an ordering direction.
   */
  @NotNull String description();

  /**
   * Describes the anchor for a concrete step in an inferred batch pipeline. By default, the first
   * matching module name is used when an anchor is present.
   */
  default @NotNull String description(@NotNull final ModuleOrderEvaluationContext context) {
    return Stream.concat(context.stepsBefore().stream(), context.stepsAfter().stream())
        .filter(this::matches).map(step -> step.getModule().getName()).findFirst()
        .orElseGet(this::description);
  }

  /**
   * Tests whether a batch step is an anchor for this condition.
   */
  boolean matches(@NotNull MZmineProcessingStep<? extends MZmineProcessingModule> step);
}
