package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowImagingWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * imaging analysis
 */
public class WorkflowImaging extends WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "IMAGING";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowImagingWizardParameters(true);
  }

  @Override
  public String toString() {
    return "Imaging";
  }
}
