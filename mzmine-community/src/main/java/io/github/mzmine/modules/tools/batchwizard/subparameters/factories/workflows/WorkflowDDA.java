package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Options for GNPS, molecular networking, SIRIUS,
 */
public class WorkflowDDA extends WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "DDA";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowDdaWizardParameters(true, true, null, true, true, false);
  }

  @Override
  public String toString() {
    return "DDA";
  }

}
