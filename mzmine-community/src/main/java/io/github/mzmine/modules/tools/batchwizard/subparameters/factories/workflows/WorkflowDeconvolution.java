package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderGcEiDeconvolution;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowGcElectronImpactWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.users.service.UserActiveService;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  public Map<WizardPart, WizardPartFilter> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE,
        WizardPartFilter.allow(List.of(IonInterfaceWizardParameterFactory.GC_EI)));
  }

  @Override
  public @NotNull WizardBatchBuilder getBatchBuilder(final @NotNull WizardSequence steps) {
    var ionInterface = (IonInterfaceWizardParameterFactory) steps.get(WizardPart.ION_INTERFACE)
        .get().getFactory();

    return switch (ionInterface.group()) {
      case CHROMATOGRAPHY_HARD -> new WizardBatchBuilderGcEiDeconvolution(steps);
      case CHROMATOGRAPHY_SOFT, DIRECT_AND_FLOW, SPATIAL_IMAGING ->
          throw new UnsupportedWorkflowException(steps);
    };
  }

  @Override
  public @NotNull Set<@NotNull UserActiveService> getUnlockingServices() {
    return EnumSet.allOf(UserActiveService.class);
  }
}
