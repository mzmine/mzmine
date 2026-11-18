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

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.parameters.UserParameter;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record WizardParameterDefinition<T>(@NotNull String name, @NotNull WizardPart part,
                                           @NotNull UserParameter<T, ?> parameter,
                                           @NotNull OptimizationRole role,
                                           @NotNull Set<WizardParameterFactory> presets,
                                           @NotNull Function<ParameterEstimationContext, ParameterEstimate<T>> estimator) implements
    ParameterDefinition<T> {

  public WizardParameterDefinition(String name, WizardPart part, UserParameter<T, ?> parameter,
      OptimizationRole role, Set<WizardParameterFactory> presets,
      Function<ParameterEstimationContext, ParameterEstimate<T>> estimator) {
    this.name = name;
    this.part = part;
    this.parameter = parameter.cloneParameter();
    this.role = role;
    this.presets = Set.copyOf(presets);
    this.estimator = estimator;
  }

  @Override
  public @NotNull String id() {
    return "wizard/" + part.name() + "/" + parameter.getName();
  }

  @Override
  public void apply(@NotNull WizardSequence sequence, @NotNull T value) {
    sequence.get(part).ifPresent(step -> step.setParameter(parameter, value));
  }

  @Override
  public @NotNull String toString() {
    return name;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof ParameterDefinition<?> definition && id().equals(definition.id());
  }

  @Override
  public int hashCode() {
    return id().hashCode();
  }
}
