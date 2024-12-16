package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowTargetPlateWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Target plate analysis
 */
public class WorkflowTargetPlate implements WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "TARGET_PLATE";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowTargetPlateWizardParameters(false, null);
  }

  @Override
  public String toString() {
    return "Target plate";
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorkflowWizardParameterFactory fac && fac.getUniqueID()
        .equals(this.getUniqueID());
  }
}
