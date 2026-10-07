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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameter;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchDomain;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OrdinalIntegerVariable;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;

/**
 * The only parameter binding that knows about an optimizer vector index.
 */
public record IndexedParameter<T>(@NotNull PreparedParameter<T> parameter, int index) {

  public IndexedParameter {
    if (index < 0) {
      throw new IllegalArgumentException("A solution index must be nonnegative");
    }
  }

  /**
   * Binds selected definitions in their requested order without changing the prepared baseline.
   */
  public static @NotNull List<IndexedParameter<?>> bind(@NotNull PreparedParameterSet prepared,
      @NotNull List<ParameterDefinition<?>> selected) {
    final List<IndexedParameter<?>> indexed = new ArrayList<>();
    if (selected.stream().distinct().count() != selected.size()) {
      throw new IllegalArgumentException("Duplicate optimizer selections");
    }
    for (final ParameterDefinition<?> definition : selected) {
      final PreparedParameter<?> parameter = prepared.parameters().stream()
          .filter(p -> p.definition().equals(definition)).findFirst().orElseThrow(
              () -> new IllegalArgumentException(
                  "Selected parameter is unavailable for this wizard: " + definition.id()));
      indexed.add(new IndexedParameter<>(parameter, indexed.size()));
    }
    return List.copyOf(indexed);
  }

  public void initialize(@NotNull Solution solution) {
    final RealVariable variable = createVariable(parameter.searchDomain(),
        parameter.definition().name());
    variable.setValue(Math.clamp(parameter.searchDomain().encode(parameter.initialValue()),
        variable.getLowerBound(), variable.getUpperBound()));
    solution.setVariable(index, variable);
  }

  /**
   * The only place that converts a search domain into a MOEA variable. Ordinal domains become
   * {@link OrdinalIntegerVariable}s, which the search algorithms round and sample per integer.
   */
  private static @NotNull RealVariable createVariable(@NotNull SearchDomain<?> domain,
      @NotNull String name) {
    return domain.ordinal() ? new OrdinalIntegerVariable(name, (int) domain.lower(),
        (int) domain.upper()) : new RealVariable(name, domain.lower(), domain.upper());
  }

  public @NotNull T value(@NotNull Solution solution) {
    return parameter.searchDomain().decode(OrdinalIntegerVariable.effectiveValue(solution, index));
  }

  public void applyToWizard(@NotNull Solution solution, @NotNull WizardSequence sequence) {
    parameter.definition().apply(sequence, value(solution));
  }

}
