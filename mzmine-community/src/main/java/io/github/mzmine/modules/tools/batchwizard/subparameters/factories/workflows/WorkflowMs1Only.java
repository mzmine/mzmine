package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilderImagingDda;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.mzio.users.user.MZmineUser;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Nothing special just avoids all MS2 specific steps
 */
public class WorkflowMs1Only extends WorkflowWizardParameterFactory {

  @Override
  public @NotNull String getUniqueID() {
    return "MS1_ONLY";
  }

  @Override
  public WizardStepParameters create() {
    return new WorkflowWizardParameters(this);
  }

  @Override
  public String toString() {
    return "MS1 only";
  }

  @Override
  public Map<WizardPart, List<WizardPartFilter>> getStepFilters() {
    return Map.of();
  }

  @Override
  public @NotNull WizardBatchBuilder getBatchBuilder(final @NotNull WizardSequence steps) {
    // throw in case we hit unsupported workflow
    // those combinations should be filtered out previously though
    throw new UnsupportedOperationException(
        "Currently not implemented workflow " + this);
  }

  @Override
  public boolean isAvailableWithLicense(@Nullable MZmineUser user) {
    return true;
  }
}
