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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchDomain;
import org.jetbrains.annotations.NotNull;

/**
 * Dataset-specific values stay attached to the definition, independent of vector ordering.
 */
public record PreparedParameter<T>(@NotNull ParameterDefinition<T> definition,
                                   @NotNull ParameterEstimate<T> estimate) {

  public PreparedParameter {
    estimate = new ParameterEstimate<>(estimate.searchDomain().constrain(estimate.initialValue()),
        estimate.origin(), estimate.searchDomain());
  }

  /**
   * @return the estimated value, constrained to the search domain
   */
  public @NotNull T initialValue() {
    return estimate.initialValue();
  }

  public @NotNull ValueOrigin origin() {
    return estimate.origin();
  }

  public @NotNull SearchDomain<T> searchDomain() {
    return estimate.searchDomain();
  }

  public void applyInitialValue(@NotNull WizardSequence sequence) {
    definition.apply(sequence, initialValue());
  }

  public @NotNull String describe() {
    return "%s: %s (%s)".formatted(definition.name(), searchDomain().format(initialValue()),
        origin());
  }
}
