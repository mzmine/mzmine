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
 * A {@link ModuleOrderRecommendation} that combines several alternatives with OR semantics: the
 * recommendation is satisfied as soon as any alternative is satisfied. It only warns when no
 * alternative passes and at least one is violated; when every alternative is not applicable it is
 * ignored. Each alternative keeps its own rationale and rule.
 *
 * @param alternatives the interchangeable recommendations, at least two
 */
public record AnyOfModuleOrderRecommendation(
    @NotNull List<@NotNull ModuleOrderRecommendation> alternatives) implements
    ModuleOrderRecommendation {

  public AnyOfModuleOrderRecommendation {
    alternatives = List.copyOf(Objects.requireNonNull(alternatives));
    if (alternatives.size() < 2) {
      throw new IllegalArgumentException("anyOf requires at least two recommendations");
    }
  }
}
