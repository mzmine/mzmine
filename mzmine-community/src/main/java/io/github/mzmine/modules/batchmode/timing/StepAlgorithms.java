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

package io.github.mzmine.modules.batchmode.timing;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the algorithms selected in a batch step for the step measurements: every
 * {@link ModuleOptionsEnumComboParameter} of the parameter set and, recursively, of the parameters
 * of the selected options. Options nested in other embedded parameters, e.g., optional advanced
 * parameters, are not described.
 */
public final class StepAlgorithms {

  private StepAlgorithms() {
  }

  /**
   * @return the selected option of a single choice, e.g., "Fast (auto)", "name: option" joined by
   * "; " for several, null without choices
   */
  @Nullable
  public static String describe(@Nullable ParameterSet parameters) {
    if (parameters == null) {
      return null;
    }
    final List<String[]> selected = new ArrayList<>();
    collect(parameters, selected);
    if (selected.isEmpty()) {
      return null;
    }
    if (selected.size() == 1) {
      return selected.getFirst()[1];
    }
    final List<String> parts = selected.stream().map(s -> s[0] + ": " + s[1]).toList();
    return String.join("; ", parts);
  }

  private static void collect(@NotNull ParameterSet parameters,
      @NotNull List<String[]> selected) {
    for (final Parameter<?> parameter : parameters.getParameters()) {
      if (!(parameter instanceof ModuleOptionsEnumComboParameter<?> options)
          || options.getValue() == null) {
        continue;
      }
      selected.add(new String[]{options.getName(), options.getValue().toString()});
      final ParameterSet embedded = options.getEmbeddedParameters();
      if (embedded != null) {
        collect(embedded, selected);
      }
    }
  }
}
