package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowGcElectronImpactWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Currently only used in GC-EI
 */
public class WorkflowDeconvolution extends WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "DECONVOLUTION";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowGcElectronImpactWizardParameters(8, true, true, null, true, true, false);
  }

  @Override
  public String toString() {
    return "Spectral deconvolution";
  }

}
