package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDiaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Options for GNPS, molecular networking, SIRIUS,
 */
public class WorkflowDIA implements WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "DIA";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowDiaWizardParameters(0.8, 5, true, null, true, true, false);
  }

  @Override
  public String toString() {
    return "DIA";
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorkflowWizardParameterFactory fac && fac.getUniqueID()
        .equals(this.getUniqueID());
  }
}
