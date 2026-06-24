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
