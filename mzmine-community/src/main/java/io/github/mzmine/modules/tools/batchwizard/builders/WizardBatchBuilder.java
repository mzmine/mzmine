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

package io.github.mzmine.modules.tools.batchwizard.builders;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a batch queue from a list of {@link WizardStepParameters} making up a workflow defined in
 * {@link WizardPart}
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class WizardBatchBuilder {

  private static final Logger logger = Logger.getLogger(WizardBatchBuilder.class.getName());

  protected final WizardSequence steps;

  protected WizardBatchBuilder(WizardSequence steps) {
    this.steps = steps;
  }

  /**
   * Create different workflows in {@link BatchQueue}. Workflows are defined in
   *
   * @return a batch queue
   */
  public final BatchQueue createQueue() {
    // this method is final so overriding classes do not need to worry about applying the parameter override.
    BatchQueue queue = __createQueueInternal();
    applyParameterOverrides(queue);
    return queue;
  }

  /**
   * Internal createQueue method to override by implementing classes. PostProcessing is applied in
   * the final {@link #createQueue()} method.
   *
   * @return a batch queue
   */
  protected abstract BatchQueue __createQueueInternal();

  /**
   * Get parameter if available or else return null. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return the value of the parameter or null if !params.isPresent
   */
  public static <T> T getValue(@NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final Parameter<T> parameter) {
    if (params.isPresent()) {
      try {
        return params.get().getValue(parameter);
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return null;
  }

  /**
   * Get parameter if available or else return default value. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return the value of the parameter or default value if !params.isPresent
   */
  @NotNull
  public static <T> T getOrElse(@NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final Parameter<T> parameter, @NotNull T defaultValue) {
    return requireNonNullElse(getValue(params, parameter), defaultValue);
  }

  /**
   * Get parameter if available or else null. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return value and selection state of an OptionalParameter
   */
  public static <V, T extends UserParameter<V, ?>> OptionalValue<V> getOptional(
      @NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final OptionalParameter<T> parameter) {
    if (params.isPresent()) {
      try {
        OptionalParameter<T> param = params.get().getParameter(parameter);
        return new OptionalValue<>(param.getValue(), param.getEmbeddedParameter().getValue());
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return new OptionalValue<>(false, null);
  }

  /**
   * Get parameter if available or else null. params usually comes from
   * {@link WizardSequence#get(WizardPart)}
   *
   * @param params    an optional parameter class for a part
   * @param parameter parameter as defined in params class. Usually a static parameter
   * @return embedded parameterset and selection state of an OptionalParameter
   */
  public static <T extends ParameterSet> OptionalValue<T> getOptionalParameters(
      @NotNull final Optional<? extends WizardStepParameters> params,
      @NotNull final EmbeddedParameterSet<T, ?> parameter) {
    if (params.isPresent()) {
      try {
        EmbeddedParameterSet<T, ?> param = params.get().getParameter(parameter);
        boolean state = true;
        if (param instanceof AdvancedParametersParameter<?> check) {
          state = check.getValue();
        }
        if (param instanceof OptionalModuleParameter<?> check) {
          state = check.getValue();
        }

        return new OptionalValue<>(state, param.getEmbeddedParameters());
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return null;
  }

  protected void applyParameterOverrides(BatchQueue queue) {
    // Check if customization is enabled
    boolean customizationEnabled = steps.get(WizardPart.CUSTOMIZATION)
        .map(p -> p.getValue(CustomizationWizardParameters.enabled)).orElse(false);

    if (!customizationEnabled) {
      logger.finest("Parameter customization is disabled, skipping overrides");
      return;
    }

    final List<ParameterOverride> parameterOverrides = steps.get(WizardPart.CUSTOMIZATION)
        .map(p -> p.getParameter(CustomizationWizardParameters.overrides).getValue())
        .orElse(List.of());

    for (ParameterOverride parameterOverride : parameterOverrides) {
      String targetModuleClassName = parameterOverride.moduleClassName();
      Parameter<?> sourceParameter = parameterOverride.parameterWithValue();
      String targetParameterName = sourceParameter.getName();
      ApplicationScope scope = parameterOverride.scope();

      // Find all steps in the queue that match the module class name
      List<Integer> matchingStepIndices = new ArrayList<>();
      for (int i = 0; i < queue.size(); i++) {
        final String stepModuleClassName = queue.get(i).getModule().getClass().getName();
        if (stepModuleClassName.equals(targetModuleClassName)) {
          matchingStepIndices.add(i);
        }
      }

      if (matchingStepIndices.isEmpty()) {
        logger.info("Override parameters set for module %s but not present in batch.".formatted(
            targetModuleClassName));
        continue;
      }

      // Determine which steps to apply the override to based on scope
      List<Integer> targetIndices = switch (scope) {
        case ALL -> matchingStepIndices;
        case FIRST -> List.of(matchingStepIndices.getFirst());
        case LAST -> List.of(matchingStepIndices.getLast());
      };

      // Apply the override to the selected steps
      for (int index : targetIndices) {
        var step = queue.get(index);
        final ParameterSet stepParameters = step.getParameterSet();

        // Find the parameter in this step's parameter set by name
        final Parameter<?> targetParameter = Arrays.stream(stepParameters.getParameters())
            .filter(param -> param.getName().equals(targetParameterName)).findFirst().orElse(null);

        if (targetParameter == null) {
          logger.warning("Parameter %s not found in module %s".formatted(targetParameterName,
              targetModuleClassName));
          continue;
        }

        try {
          ParameterUtils.copyParameterValue(sourceParameter, targetParameter);
          logger.fine("Applied parameter override (%s) for %s.%s = %s".formatted(scope,
              targetModuleClassName, targetParameterName, sourceParameter.getValue()));
        } catch (Exception ex) {
          logger.log(Level.WARNING,
              "Failed to apply parameter override for %s.%s".formatted(targetModuleClassName,
                  targetParameterName), ex);
        }
      }
    }
  }
}
