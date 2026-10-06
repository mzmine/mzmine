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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All parameter values that were changed in a wizard sequence by a single source, e.g., parameter
 * estimation or optimization.
 *
 * @param source  what changed the values, e.g., "parameter estimation"
 * @param changes the changed parameters
 */
public record WizardParameterChanges(@NotNull WizardParameterChanges.Source source,
                                     @NotNull List<WizardParameterChange> changes) {

  private static final WizardParameterChanges EMPTY = new WizardParameterChanges(Source.NONE,
      List.of());

  /**
   * Compares all user parameters of the steps in after with the steps of the same
   * {@link WizardPart} in before.
   *
   * @param before sequence before the change, should be a copy
   * @param after  sequence after the change
   * @param source what changed the values, e.g., "parameter estimation"
   * @return all parameters that were added or have a different value
   */
  public static @NotNull WizardParameterChanges diff(@NotNull WizardSequence before,
      @NotNull WizardSequence after, @NotNull WizardParameterChanges.Source source) {
    final List<WizardParameterChange> changes = new ArrayList<>();
    for (final WizardStepParameters afterStep : after) {
      final WizardPart part = afterStep.getPart();
      final WizardStepParameters beforeStep = before.get(part).orElse(null);

      for (final Parameter<?> afterParam : afterStep.getParameters()) {
        // only user parameters have components that can be highlighted
        if (!(afterParam instanceof UserParameter<?, ?> userParam)) {
          continue;
        }
        // decision: compare by parameter name even if the preset (factory) of the part changed
        final Parameter<?> beforeParam =
            beforeStep == null ? null : beforeStep.tryGetParameter(afterParam).orElse(null);
        if (beforeParam != null && afterParam.valueEquals(beforeParam)) {
          continue;
        }
        changes.add(new WizardParameterChange(part, userParam, displayValue(beforeParam),
            displayValue(afterParam)));
      }
    }
    return new WizardParameterChanges(source, changes);
  }

  public WizardParameterChanges {
    changes = List.copyOf(changes);
  }

  public static @NotNull WizardParameterChanges empty() {
    return EMPTY;
  }

  public enum Source {
    NONE, ESTIMATION, OPTIMIZATION;

    @Override
    public String toString() {
      return switch (this) {
        case NONE -> "None";
        case ESTIMATION -> "Estimation";
        case OPTIMIZATION -> "Optimization";
      };
    }

    @Nullable
    public FxIcons icon() {
      return switch (this) {
        case NONE -> null;
        case ESTIMATION -> FxIcons.LIGHTBULB;
        case OPTIMIZATION -> FxIcons.GRAPH_UP;
      };
    }
  }

  /**
   * Optional parameters only carry the selection state as value, show the embedded value instead.
   */
  private static @Nullable Object displayValue(@Nullable Parameter<?> parameter) {
    return switch (parameter) {
      case null -> null;
      case OptionalParameter<?> optional ->
          Boolean.TRUE.equals(optional.getValue()) ? optional.getEmbeddedParameter().getValue()
              : "disabled";
      default -> parameter.getValue();
    };
  }

  public boolean isEmpty() {
    return changes.isEmpty();
  }

  public @NotNull List<WizardParameterChange> forPart(@NotNull WizardPart part) {
    return changes.stream().filter(change -> change.part() == part).toList();
  }

  /**
   * @return a copy without the changes of part, e.g., after the user selected another preset
   */
  public @NotNull WizardParameterChanges withoutPart(@NotNull WizardPart part) {
    return new WizardParameterChanges(source,
        changes.stream().filter(change -> change.part() != part).toList());
  }
}
