package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.users.user.CurrentUserService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

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

  /** All registered capabilities, independent of the current user's licence. */
  public static synchronized @NotNull WorkflowWizardParameterFactory[] allRegistered() {
    return values.toArray(WorkflowWizardParameterFactory[]::new);
  }

  public static synchronized void addWorkflow(WorkflowWizardParameterFactory workflow) {
    values.add(workflow);
  }
}
