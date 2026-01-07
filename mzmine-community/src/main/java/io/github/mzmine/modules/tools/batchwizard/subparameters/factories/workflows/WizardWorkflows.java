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

package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.users.user.CurrentUserService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all registered workflows. Not all must be available in the workspace/license
 */
public class WizardWorkflows {

  private static final Set<WorkflowWizardParameterFactory> values = new LinkedHashSet<>(
      // default workflows
      List.of(new WorkflowDDA(), new WorkflowDIA(), new WorkflowDeconvolution(),
          new WorkflowLibraryGeneration(), new WorkflowImaging(), new WorkflowTargetPlate()));

  public static synchronized WorkflowWizardParameterFactory[] values() {
    return values.stream()
        .filter(workflow -> workflow.checkUserForServices(CurrentUserService.getUser()).isOk())
        .toArray(WorkflowWizardParameterFactory[]::new);
  }

  public static synchronized void addWorkflow(WorkflowWizardParameterFactory workflow) {
    values.add(workflow);
  }
}
