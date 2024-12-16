package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowLibraryGenerationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * uses annotations to build spectral libraries
 */
public class WorkflowLibraryGeneration extends WorkflowWizardParameterFactory {

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
  public Map<WizardPart, List<WizardPartFilter>> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE, List.of(WizardPartFilter.allow(
        List.of(IonInterfaceWizardParameterFactory.DIRECT_INFUSION,
            IonInterfaceWizardParameterFactory.FLOW_INJECT,
            IonInterfaceWizardParameterFactory.GC_CI, IonInterfaceWizardParameterFactory.HPLC,
            IonInterfaceWizardParameterFactory.UHPLC, IonInterfaceWizardParameterFactory.HILIC))));
  }
}
