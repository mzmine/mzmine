package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowLibraryGenerationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * uses annotations to build spectral libraries
 */
public class WorkflowLibraryGeneration implements WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "LIBRARY_GENERATION";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowLibraryGenerationWizardParameters(null, true, true, false);
  }

  @Override
  public String toString() {
    return "Library generation";
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorkflowWizardParameterFactory fac && fac.getUniqueID()
        .equals(this.getUniqueID());
  }
}
