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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * Index-free parameter identity, estimator, and typed wizard/batch binding.
 */
public sealed interface ParameterDefinition<T> extends UniqueIdSupplier permits
    WizardParameterDefinition, BatchParameterDefinition {

  /**
   * Derived from the actual target, independently of the optimization display label.
   */
  @NotNull String id();

  /**
   * The stable ID for saving a selection, see {@link #id()}.
   */
  @Override
  default @NotNull String getUniqueID() {
    return id();
  }

  @NotNull String name();

  /**
   * How the optimizer treats this definition.
   */
  @NotNull OptimizationRole role();

  /**
   * The wizard presets this definition applies to. It is estimated and applied, and can be
   * optimized, only if the sequence contains one of them.
   */
  @NotNull Set<WizardParameterFactory> presets();

  default boolean appliesTo(@NotNull WizardSequence sequence) {
    return sequence.stream().map(WizardStepParameters::getFactory).anyMatch(presets()::contains);
  }

  @NotNull Function<ParameterEstimationContext, ParameterEstimate<T>> estimator();

  void apply(@NotNull WizardSequence sequence, @NotNull T value);

  default @NotNull PreparedParameter<T> prepare(@NotNull ParameterEstimationContext context) {
    return new PreparedParameter<>(this, estimator().apply(context));
  }
}
