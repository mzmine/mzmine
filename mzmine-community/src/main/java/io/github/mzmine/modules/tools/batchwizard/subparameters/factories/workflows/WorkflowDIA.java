package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDiaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Options for GNPS, molecular networking, SIRIUS,
 */
public class WorkflowDIA extends WorkflowWizardParameterFactory {

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
  public Map<WizardPart, List<WizardPartFilter>> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE, List.of(WizardPartFilter.allow(
        List.of(IonInterfaceWizardParameterFactory.HPLC, IonInterfaceWizardParameterFactory.UHPLC,
            IonInterfaceWizardParameterFactory.HILIC))));
  }
}
