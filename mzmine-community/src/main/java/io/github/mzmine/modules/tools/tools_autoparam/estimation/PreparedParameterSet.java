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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Complete immutable baseline; selected optimizer dimensions are views of these same parameters.
 */
public record PreparedParameterSet(@NotNull List<PreparedParameter<?>> parameters) {

  public PreparedParameterSet {
    parameters = List.copyOf(parameters);
    if (parameters.stream().map(PreparedParameter::definition).distinct().count()
        != parameters.size()) {
      throw new IllegalArgumentException("Duplicate prepared parameter identities");
    }
  }

  /**
   * Prepares every applicable definition from completed statistics without running processing.
   */
  public static @NotNull PreparedParameterSet prepare(@NotNull ParameterEstimationContext context) {
    final List<PreparedParameter<?>> parameters = OptimizationParameterRegistry.forSequence(
            context.sequence()).stream()
        .<PreparedParameter<?>>map(definition -> definition.prepare(context)).toList();
    return new PreparedParameterSet(parameters);
  }

  /**
   * Applies measured estimates and heuristics, preserving unrelated current wizard edits.
   */
  public void applyEstimates(@NotNull WizardSequence sequence) {
    applyEstimates(sequence, Set.of());
  }

  /**
   * Applies measured estimates and heuristics except for the excluded parameters, preserving
   * unrelated current wizard edits.
   *
   * @param excluded parameters that are set by another source, e.g., the optimizer
   */
  public void applyEstimates(@NotNull WizardSequence sequence,
      @NotNull Set<ParameterDefinition<?>> excluded) {
    for (final PreparedParameter<?> parameter : parameters) {
      // decision: preset defaults are not applied, so values the user changed in the wizard stay
      // as they are. Unlike applyBaseline, which evaluates with the preset defaults.
      if (parameter.origin() != ValueOrigin.PRESET_DEFAULT && !excluded.contains(
          parameter.definition())) {
        parameter.applyInitialValue(sequence);
      }
    }
  }

  /**
   * Candidate values replace selected parameters; all remaining baseline values stay fixed.
   * <p>
   * decision: the optimization always evaluates with the wizard preset defaults, including the
   * {@link ValueOrigin#PRESET_DEFAULT} values, so results do not depend on prior user edits. When
   * the result is applied to the wizard, {@link #applyEstimates(WizardSequence, Set)} keeps the
   * user-changed values of those parameters.
   */
  public void applyBaseline(@NotNull WizardSequence sequence,
      @NotNull Set<ParameterDefinition<?>> selected) {
    for (final PreparedParameter<?> parameter : parameters) {
      if (!selected.contains(parameter.definition())) {
        parameter.applyInitialValue(sequence);
      }
    }
  }

  public @NotNull String describe() {
    return parameters.stream().map(PreparedParameter::describe).collect(Collectors.joining("\n"));
  }
}
