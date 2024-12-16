package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Nothing special just avoids all MS2 specific steps
 */
public class WorkflowMs1Only extends WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "MS1_ONLY";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowWizardParameters(this);
  }

  @Override
  public String toString() {
    return "MS1 only";
  }

}
