package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderLcDIA;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDiaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.users.service.UserActiveService;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    return new WorkflowDiaWizardParameters(0.8, 5, true, true, null, true, true, false);
  }

  @Override
  public String toString() {
    return "DIA";
  }

  @Override
  public Map<WizardPart, WizardPartFilter> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE, WizardPartFilter.allow(
        List.of(IonInterfaceWizardParameterFactory.HPLC, IonInterfaceWizardParameterFactory.UHPLC,
            IonInterfaceWizardParameterFactory.HILIC)));
  }

  @Override
  public @NotNull WizardBatchBuilder getBatchBuilder(final @NotNull WizardSequence steps) {
    var ionInterface = (IonInterfaceWizardParameterFactory) steps.get(WizardPart.ION_INTERFACE)
        .get().getFactory();

    return switch (ionInterface.group()) {
      case CHROMATOGRAPHY_SOFT -> new WizardBatchBuilderLcDIA(steps);
      case CHROMATOGRAPHY_HARD, SPATIAL_IMAGING, DIRECT_AND_FLOW ->
          throw new UnsupportedWorkflowException(steps);
    };
  }

  @Override
  public @NotNull Set<@NotNull UserActiveService> getUnlockingServices() {
    return EnumSet.allOf(UserActiveService.class);
  }
}
