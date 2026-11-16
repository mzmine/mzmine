/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.mzio.mzmine.startup.EstimateResourcesSettings;
import java.io.File;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
   * Runs an estimation synchronously for CLI startup. Uses current thread, RAM and temporary
   * storage settings and skips the batch's export steps.
   *
   * @param batchFile             batch whose first step imports all input files
   * @param overrideDataFiles     optional replacement input files
   * @param overrideMetadataFile  optional replacement metadata file
   * @param overrideLibraries     optional replacement spectral libraries
   * @param overrideProjectImport optional replacement project import file
   * @param overrideCsvDatabase   optional replacement compound database
   * @param settings              parsed CLI estimation settings
   * @param moduleCallDate        call date used for task tracking
   * @return completed task, including its error or cancellation status
   */
  public static @NotNull Task runBatchFile(@NotNull final File batchFile,
      @Nullable final File[] overrideDataFiles, @Nullable final File overrideMetadataFile,
      @Nullable final File[] overrideLibraries, @Nullable final File overrideProjectImport,
      @Nullable final File overrideCsvDatabase, @NotNull final EstimateResourcesSettings settings,
      @NotNull final Instant moduleCallDate) {
    final BatchResourceEstimationParameters parameters = BatchResourceEstimationParameters.fromSettings(
        batchFile, settings);
    final Task task = new BatchResourceEstimationTask(moduleCallDate, parameters,
        new GlobalProjectProvider(), queue -> {
      if ((overrideDataFiles != null || overrideMetadataFile != null || overrideLibraries != null)
          && !queue.setImportFiles(overrideDataFiles, overrideMetadataFile, overrideLibraries)) {
        throw new IllegalArgumentException("Cannot apply the CLI input overrides to the batch");
      }
      if (overrideProjectImport != null && !queue.overrideStepParameter(ProjectLoadModule.class,
          ProjectLoaderParameters.projectFile, overrideProjectImport)) {
        throw new IllegalArgumentException("Cannot apply the CLI project import override");
      }
      if (overrideCsvDatabase != null && !queue.overrideStepParameter(
          LocalCSVDatabaseSearchModule.class, LocalCSVDatabaseSearchParameters.dataBaseFile,
          overrideCsvDatabase)) {
        throw new IllegalArgumentException("Cannot apply the CLI compound database override");
      }
    });
    task.run();
    return task;
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
