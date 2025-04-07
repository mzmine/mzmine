/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.parameters.Parameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public non-sealed class WorkflowWizardParameters extends WizardStepParameters {

  public WorkflowWizardParameters(final WorkflowWizardParameterFactory preset,
      final Parameter<?>... parameters) {
    super(WizardPart.WORKFLOW, preset, parameters);
  }

  /**
   * @param steps a sequence of steps
   * @return true if the sequence of steps allows for this workflow, false otherwise
   */
  public boolean isApplicableToSteps(final WizardSequence steps) {
    final Map<WizardPart, WizardPartFilter> workflowStepFilters = getFactory().getStepFilters();
    // check if all filters match
    // only WizardPart with filters are added as filters. Other parts that allow all workflows are not in map
    return workflowStepFilters.entrySet().stream().allMatch(entry -> steps.get(entry.getKey())
        .map(stepParam -> entry.getValue().accept(stepParam.getFactory())).orElse(false));
  }

  @Override
  public @NotNull WorkflowWizardParameterFactory getFactory() {
    return (WorkflowWizardParameterFactory) super.getFactory();
  }
}
