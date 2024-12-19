package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderFlowInjectDDA;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderImagingDda;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderLcDDA;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.users.user.MZmineUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @Override
  public @NotNull WizardBatchBuilder getBatchBuilder(final @NotNull WizardSequence steps) {
    // throw in case we hit unsupported workflow
    // those combinations should be filtered out previously though
    var unsupportedException = new UnsupportedOperationException(
        "Currently not implemented workflow " + this);
    var ionInterface = (IonInterfaceWizardParameterFactory) steps.get(WizardPart.ION_INTERFACE)
        .get().getFactory();

    return switch (ionInterface.group()) {
      case CHROMATOGRAPHY_SOFT -> new WizardBatchBuilderLcDDA(steps);
      case DIRECT_AND_FLOW -> new WizardBatchBuilderFlowInjectDDA(steps);
      case SPATIAL_IMAGING -> new WizardBatchBuilderImagingDda(steps);
      case CHROMATOGRAPHY_HARD -> throw unsupportedException;
    };
  }

  @Override
  public boolean isAvailableWithLicense(@Nullable MZmineUser user) {
    return true;
  }
}
