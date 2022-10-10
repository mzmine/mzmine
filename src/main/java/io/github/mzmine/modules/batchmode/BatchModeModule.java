/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

/**
 * Batch mode module
 */
public class BatchModeModule implements MZmineProcessingModule {

  private static final Logger logger = Logger.getLogger(BatchModeModule.class.getName());
  private static final String MODULE_NAME = "Batch mode";
  private static final String MODULE_DESCRIPTION = "This module allows execution of multiple processing tasks in a batch.";

  /**
   * Run from batch file (usually in headless mode)
   *
   * @param batchFile local file
   * @return exit code that reflects if the batch mode was started
   */
  public static ExitCode runBatch(@NotNull MZmineProject project, File batchFile,
      @NotNull Instant moduleCallDate) {

    if (MZmineCore.getTaskController().isTaskInstanceRunningOrQueued(BatchTask.class)) {
      MZmineCore.getDesktop().displayErrorMessage(
          "Cannot run a second batch while the current batch is not finished.");
      return ExitCode.ERROR;
    }

    logger.info("Running batch from file " + batchFile);

    try {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document parsedBatchXML = docBuilder.parse(batchFile);
      BatchQueue newQueue = BatchQueue.loadFromXml(parsedBatchXML.getDocumentElement());
      ParameterSet parameters = new BatchModeParameters();
      parameters.getParameter(BatchModeParameters.batchQueue).setValue(newQueue);
      Task batchTask = new BatchTask(project, parameters, moduleCallDate);
      batchTask.run();
      if (batchTask.getStatus() == TaskStatus.FINISHED) {
        return ExitCode.OK;
      } else {
        return ExitCode.ERROR;
      }
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Error while running batch. " + e.getMessage(), e);
      e.printStackTrace();
      return ExitCode.ERROR;
    }
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    if (MZmineCore.getTaskController().isTaskInstanceRunningOrQueued(BatchTask.class)) {
      MZmineCore.getDesktop().displayErrorMessage(
          "Cannot run a second batch while the current batch is not finished.");
      return ExitCode.ERROR;
    }

    final BatchTask newTask;
    // check if advanced
    if (parameters.getValue(BatchModeParameters.advanced)) {
      AdvancedBatchModeParameters params = parameters.getParameter(BatchModeParameters.advanced)
          .getEmbeddedParameters();
      File parentDir = params.getValue(AdvancedBatchModeParameters.processingParentDir);
      if (parentDir != null && parentDir.exists()) {
        List<File> subDirs = new ArrayList<>();
        for (File sub : parentDir.listFiles()) {
          if (sub.isDirectory()) {
            subDirs.add(sub);
          }
        }
        if (subDirs.isEmpty()) {
          DialogLoggerUtil.showErrorDialog("No parent directory",
              "Parent directory not set or does not exist");
          return ExitCode.ERROR;
        }
        String dirs = subDirs.size() + " directories";

        String message = String.join(": ", "Advanced mode selected. Will run on ", dirs);
        if (DialogLoggerUtil.showDialogYesNo("Run advanced mode?", message)) {
          newTask = new BatchTask(project, parameters, moduleCallDate, subDirs);
        } else {
          return ExitCode.CANCEL;
        }
      } else {
        DialogLoggerUtil.showErrorDialog("No parent directory",
            "Parent directory not set or does not exist");
        return ExitCode.ERROR;
      }
    } else {
      newTask = new BatchTask(project, parameters, moduleCallDate);
    }
    /*
     * We do not add the task to the tasks collection, but instead directly submit to the task
     * controller, because we need to set the priority to HIGH. If the priority is not HIGH and the
     * maximum number of concurrent tasks is set to 1 in the MZmine preferences, then this BatchTask
     * would block all other tasks. See getTaskPriority in BatchTask
     */
    tasks.add(newTask);

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.PROJECT;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return BatchModeParameters.class;
  }

}
