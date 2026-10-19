package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class BatchResourceEstimationModule extends AbstractRunnableModule {

  public BatchResourceEstimationModule() {
    super("Batch resource estimation", BatchResourceEstimationParameters.class,
        MZmineModuleCategory.TOOLS, """
            Estimates the memory (RAM), temp file space and time that a batch needs for all its \
            files. Runs the batch on subsets of the files and extrapolates the measurements.""");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    if (!confirmClearProject(project)) {
      return ExitCode.CANCEL;
    }
    tasks.add(new BatchResourceEstimationTask(moduleCallDate, parameters,
        new GlobalProjectProvider()));
    return ExitCode.OK;
  }

  /**
   * The batch runs on the global project, which is cleared for every subset. Asks the user to
   * confirm if the project has data, call on the FX thread.
   *
   * @return true if the project is empty, the user confirmed or the GUI is not running
   */
  static boolean confirmClearProject(@NotNull final MZmineProject project) {
    final boolean projectHasData =
        !project.getCurrentRawDataFiles().isEmpty() || !project.getCurrentFeatureLists().isEmpty();
    return !projectHasData || !DesktopService.isGUI() || DialogLoggerUtil.showDialogYesNo(
        "Clear current project?", """
            The resource estimation processes the batch on subsets of the files and clears the \
            current project before each subset. Unsaved data will be lost. Continue?""");
  }
}
