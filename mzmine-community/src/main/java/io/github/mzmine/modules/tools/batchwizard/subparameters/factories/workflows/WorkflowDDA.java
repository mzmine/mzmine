package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import java.util.List;
import java.util.Map;
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

  @Override
  public Map<WizardPart, List<WizardPartFilter>> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE, List.of(WizardPartFilter.allow(
        List.of(IonInterfaceWizardParameterFactory.DIRECT_INFUSION,
            IonInterfaceWizardParameterFactory.FLOW_INJECT,
            IonInterfaceWizardParameterFactory.GC_CI, IonInterfaceWizardParameterFactory.HPLC,
            IonInterfaceWizardParameterFactory.UHPLC, IonInterfaceWizardParameterFactory.HILIC))));
  }
}
