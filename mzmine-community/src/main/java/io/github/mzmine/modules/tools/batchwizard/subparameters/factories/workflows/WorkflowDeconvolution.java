package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowGcElectronImpactWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import java.util.List;
import java.util.Map;
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

  @Override
  public Map<WizardPart, List<WizardPartFilter>> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE,
        List.of(WizardPartFilter.allow(List.of(IonInterfaceWizardParameterFactory.GC_EI))));
  }
}
