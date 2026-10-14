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
import org.jetbrains.annotations.NotNull;

/**
 * One placement recommendation for a module. All recommendations returned by a module are evaluated
 * independently. Use {@link #of} for a single ordering rule, or {@link #anyOf} to combine several
 * recommendations with OR semantics: the combined recommendation is satisfied as soon as any of its
 * alternatives is satisfied, and each alternative keeps its own rationale.
 */
public sealed interface ModuleOrderRecommendation permits SingleModuleOrderRecommendation,
    AnyOfModuleOrderRecommendation {

  /**
   * A recommendation satisfied by a single ordering rule.
   *
   * @param rationale explanation of why the placement matters
   * @param rule      ordering rule for this placement
   */
  static @NotNull ModuleOrderRecommendation of(@NotNull final String rationale,
      @NotNull final ModuleOrderRule rule) {
    return new SingleModuleOrderRecommendation(rationale, rule);
  }

  /**
   * A recommendation satisfied when any of the given alternatives is satisfied. Each alternative
   * keeps its own rationale and rule, so different placements can be explained individually.
   */
  static @NotNull ModuleOrderRecommendation anyOf(
      @NotNull final ModuleOrderRecommendation... alternatives) {
    return new AnyOfModuleOrderRecommendation(List.of(alternatives));
  }
}
