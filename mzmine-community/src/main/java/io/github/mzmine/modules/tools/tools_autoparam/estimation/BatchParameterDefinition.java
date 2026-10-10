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

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BatchParameterDefinition<T>(@NotNull String name, @NotNull String moduleClassName,
                                          @NotNull String parameterName,
                                          @NotNull ApplicationScope scope,
                                          @NotNull OptimizationRole role,
                                          @NotNull Set<WizardParameterFactory> presets,
                                          @NotNull Function<T, ParameterOverride> overrideFactory,
                                          @NotNull Function<ParameterEstimationContext, ParameterEstimate<T>> estimator) implements
    ParameterDefinition<T> {

  public BatchParameterDefinition {
    presets = Set.copyOf(presets);
  }

  public BatchParameterDefinition(@NotNull String name,
      @NotNull Class<? extends MZmineProcessingModule> module,
      @NotNull UserParameter<T, ?> parameter, @NotNull ApplicationScope scope,
      @NotNull OptimizationRole role, @NotNull Set<WizardParameterFactory> presets,
      @NotNull Function<ParameterEstimationContext, ParameterEstimate<T>> estimator) {
    this(name, module.getName(), parameter.getName(), scope, role, presets,
        value -> new ParameterOverride(module.getName(), module.getSimpleName(), parameter, value,
            scope), estimator);
  }

  @Override
  public @NotNull String id() {
    return "batch/" + moduleClassName + "/" + scope.name() + "/" + parameterName;
  }

  @Override
  public void apply(@NotNull WizardSequence sequence, @NotNull T value) {
    final ParameterOverride replacement = overrideFactory.apply(value);
    sequence.get(WizardPart.CUSTOMIZATION).ifPresent(customization -> {
      final List<ParameterOverride> existing = customization.getValue(
          CustomizationWizardParameters.overrides);
      final List<ParameterOverride> merged = new ArrayList<>(
          existing != null ? existing : List.of());
      merged.removeIf(current -> current.moduleClassName().equals(replacement.moduleClassName())
          && current.parameterWithValue().getName()
          .equals(replacement.parameterWithValue().getName())
          && current.scope() == replacement.scope());
      merged.add(replacement);
      customization.setParameter(CustomizationWizardParameters.enabled, true);
      customization.setParameter(CustomizationWizardParameters.overrides, List.copyOf(merged));
    });
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
