package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BioTransformerModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "BioTransformer";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return BioTransformerParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Transforms features with a SMILES annotation to their metabolites by BioTransformer "
        + "in-silico computation.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new BioTransformerTask(project, parameters, moduleCallDate));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.IDENTIFICATION;
  }
}
