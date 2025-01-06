package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderTargetPlate;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowTargetPlateWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.users.user.MZmineUser;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Target plate analysis
 */
public class WorkflowTargetPlate extends WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "TARGET_PLATE";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowTargetPlateWizardParameters(false, null);
  }

  @Override
  public String toString() {
    return "Target plate";
  }

  @Override
  public Map<WizardPart, WizardPartFilter> getStepFilters() {
    return Map.of(WizardPart.ION_INTERFACE,
        WizardPartFilter.allow(IonInterfaceWizardParameterFactory.MALDI,
            IonInterfaceWizardParameterFactory.LDI, IonInterfaceWizardParameterFactory.DESI,
            IonInterfaceWizardParameterFactory.SIMS));
  }

  @Override
  public @NotNull WizardBatchBuilder getBatchBuilder(final @NotNull WizardSequence steps) {
    var ionInterface = (IonInterfaceWizardParameterFactory) steps.get(WizardPart.ION_INTERFACE)
        .get().getFactory();

    return switch (ionInterface.group()) {
      case SPATIAL_IMAGING -> new WizardBatchBuilderTargetPlate(steps);
      case CHROMATOGRAPHY_HARD, CHROMATOGRAPHY_SOFT, DIRECT_AND_FLOW ->
          throw new UnsupportedWorkflowException(steps);
    };
  }

  @Override
  public boolean isAvailableWithLicense(@Nullable MZmineUser user) {
    return true;
  }
}
