package io.github.mzmine.modules.tools.output_analyze_logs;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class AnalyzeLogFileModule extends AbstractRunnableModule {

  public AnalyzeLogFileModule() {
    super("Analyze mzmine log file", AnalyzeLogFileParameters.class, MZmineModuleCategory.TOOLS,
        "Analyze mzmine log file, extracting warnings, errors, exceptions");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    tasks.add(new AnalyzeLogFileTask(moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
