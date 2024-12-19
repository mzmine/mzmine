package io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows;

import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class UnsupportedWorkflowException extends UnsupportedOperationException {

  public UnsupportedWorkflowException(@Nullable WizardSequence steps) {
    final String message = "Workflow %s is currently not supported".formatted(steps == null ? "unknown" : steps.stream().map(
        wsp -> wsp.getFactory().toString()).collect(Collectors.joining("-")));
    super(message);
  }
}
