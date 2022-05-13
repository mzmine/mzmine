package io.github.mzmine.modules.visualization.massvoltammogram;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MassvoltommogramModule implements MZmineRunnableModule {

  @Override
  public @NotNull String getName() {
    return "Massvoltammogram chart";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return MassvoltammogramParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

      tasks.add(new MassvoltammogramTask(parameters, moduleCallDate));

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }
}
