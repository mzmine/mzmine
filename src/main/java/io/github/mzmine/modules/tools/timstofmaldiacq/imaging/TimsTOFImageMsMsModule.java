package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

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

public class TimsTOFImageMsMsModule implements MZmineRunnableModule {

  @Override
  public @NotNull String getName() {
    return "Maldi imaging MSMS acquisition module";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return TimsTOFImageMsMsParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "null";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new TimsTOFImageMsMsTask(null, moduleCallDate, parameters, project));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return null;
  }
}
