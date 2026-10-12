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

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Result of evaluating one (possibly combined) recommendation. {@code violations} is non-empty only
 * when {@code status} is {@link ModuleOrderRuleStatus#VIOLATION} and lists every violated
 * alternative in declaration order.
 */
record ModuleOrderRecommendationEvaluation(@NotNull ModuleOrderRecommendation recommendation,
                                           @NotNull ModuleOrderRuleStatus status,
                                           @NotNull List<@NotNull ModuleOrderRuleViolation> violations) {

  ModuleOrderRecommendationEvaluation {
    Objects.requireNonNull(recommendation);
    Objects.requireNonNull(status);
    violations = List.copyOf(Objects.requireNonNull(violations));
  }
}
