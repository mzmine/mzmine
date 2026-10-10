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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single parameter override for a specific module parameter. This class is
 * serializable so it can be saved/loaded with wizard presets.
 */
public record ParameterOverride(@NotNull String moduleClassName, @NotNull String moduleUniqueId,
                                @NotNull Parameter<?> parameterWithValue,
                                @NotNull ApplicationScope scope) {

  private static final Logger logger = Logger.getLogger(ParameterOverride.class.getName());

  /**
   * The supplied parameter may be a shared prototype; only its clone receives the override value.
   */
  public <T> ParameterOverride(@NotNull String moduleClassName, @NotNull String moduleUniqueId,
      @NotNull Parameter<T> parameter, @Nullable T value, @NotNull ApplicationScope scope) {
    this(moduleClassName, moduleUniqueId, cloneWithValue(parameter, value), scope);
  }

  /**
   * Snapshots an edited or XML-loaded parameter, including its embedded parameter settings.
   */
  public static <T> @NotNull ParameterOverride fromParameter(@NotNull String moduleClassName,
      @NotNull String moduleUniqueId, @NotNull Parameter<T> parameter,
      @NotNull ApplicationScope scope) {
    return new ParameterOverride(moduleClassName, moduleUniqueId, parameter, parameter.getValue(),
        scope);
  }

  private static <T> @NotNull Parameter<T> cloneWithValue(@NotNull Parameter<T> parameter,
      @Nullable T value) {
    final Parameter<T> clone = parameter.cloneParameter();
    clone.setValue(value);
    return clone;
  }

  /**
   * Applies this override to all matching steps in the given batch queue.
   *
   * @param queue the batch queue to apply the override to
   */
  public void apply(BatchQueue queue) {
    List<Integer> matchingStepIndices = new ArrayList<>();
    for (int i = 0; i < queue.size(); i++) {
      if (queue.get(i).getModule().getClass().getName().equals(moduleClassName)) {
        matchingStepIndices.add(i);
      }
    }

    if (matchingStepIndices.isEmpty()) {
      logger.info("Override parameters set for module %s but not present in batch.".formatted(
          moduleClassName));
      return;
    }

    List<Integer> targetIndices = switch (scope) {
      case ALL -> matchingStepIndices;
      case FIRST -> List.of(matchingStepIndices.getFirst());
      case LAST -> List.of(matchingStepIndices.getLast());
    };

    String targetParameterName = parameterWithValue.getName();
    for (int index : targetIndices) {
      var step = queue.get(index);
      final ParameterSet stepParameters = step.getParameterSet();

      final Parameter<?> targetParameter = Arrays.stream(stepParameters.getParameters())
          .filter(param -> param.getName().equals(targetParameterName)).findFirst().orElse(null);

      if (targetParameter == null) {
        logger.warning(
            "Parameter %s not found in module %s".formatted(targetParameterName, moduleClassName));
        continue;
      }

      try {
        ParameterUtils.copyParameterValue(parameterWithValue, targetParameter);
        logger.fine(
            "Applied parameter override (%s) for %s.%s = %s".formatted(scope, moduleClassName,
                targetParameterName, parameterWithValue.getValue()));
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Failed to apply parameter override for %s.%s".formatted(moduleClassName,
                targetParameterName), ex);
      }
    }
  }

  /**
   * Gets a display string for the value (truncated if too long)
   */
  public String getDisplayValue() {
    String valueAsString = String.valueOf(parameterWithValue.getValue());
    if (parameterWithValue instanceof EmbeddedParameter<?, ?, ?> opt) {
      valueAsString += ", " + opt.getEmbeddedParameter().getValue();
    }

    if (valueAsString.length() > 50) {
      return valueAsString.substring(0, 47) + "...";
    }
    return valueAsString;
  }

  @Override
  public @NotNull String toString() {
    return moduleUniqueId + "." + parameterWithValue.getName() + " = " + getDisplayValue();
  }
}
