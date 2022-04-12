package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_import;

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

public class SiriusResultsImportModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "Sirius results import";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return SiriusResultsImportParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Imports compound annotations from Sirius projects";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new SiriusResultsImportTask(parameters, null, moduleCallDate));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
