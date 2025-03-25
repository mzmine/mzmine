/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import static io.github.mzmine.main.ConfigService.getPreference;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.change_outfiles.ChangeOutputFilesUtils;
import io.github.mzmine.modules.batchmode.timing.StepTimeMeasurement;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.taskcontrol.threadpools.ThreadPoolTask;
import io.github.mzmine.taskcontrol.utils.TaskUtils;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CsvWriter;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import static java.util.Objects.requireNonNullElse;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert.AlertType;
import org.jetbrains.annotations.NotNull;

/**
 * Batch mode task
 */
public class BatchTask extends AbstractTask {

  private final BatchQueue queue;
  // advanced parameters
  private final int stepsPerDataset;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final int totalSteps;
  private final MZmineProject project;
  private final boolean useAdvanced;
  private final int datasets;
  private final List<StepTimeMeasurement> stepTimes = new ArrayList<>();
  private final boolean runGCafterBatchStep;
  private int processedSteps;
  private List<File> subDirectories;
  private List<RawDataFile> createdDataFiles;
  private List<RawDataFile> previousCreatedDataFiles;
  private List<FeatureList> createdFeatureLists;
  private List<FeatureList> previousCreatedFeatureLists;
  private boolean skipOnError = false;
  private Boolean searchSubdirs;
  private Boolean createResultsDir;
  private File parentDir;
  private int currentDataset;

  BatchTask(MZmineProject project, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    this(project, parameters, moduleCallDate,
        null); // we don't create any new data here, date is irrelevant, too.
  }

  public BatchTask(final MZmineProject project, final ParameterSet parameters,
      final Instant moduleCallDate, final List<File> subDirectories) {
    super(null, moduleCallDate);
    this.runGCafterBatchStep = requireNonNullElse(
        getPreference(MZminePreferences.runGCafterBatchStep), false);

    setName("Batch task");
    this.project = project;
    this.queue = parameters.getParameter(BatchModeParameters.batchQueue).getValue();
    // advanced parameters
    useAdvanced = false;
//    useAdvanced = parameters.getParameter(BatchModeParameters.advanced).getValue();
    if (useAdvanced) {
      // if sub directories is set - the input and output files are changed to each sub directory
      // each sub dir is processed sequentially as a different dataset
      AdvancedBatchModeParameters advanced = parameters.getParameter(BatchModeParameters.advanced)
          .getEmbeddedParameters();
      skipOnError = advanced.getValue(AdvancedBatchModeParameters.skipOnError);
      searchSubdirs = advanced.getValue(AdvancedBatchModeParameters.includeSubdirectories);
      createResultsDir = advanced.getValue(AdvancedBatchModeParameters.createResultsDirectory);
      parentDir = advanced.getValue(AdvancedBatchModeParameters.processingParentDir);
      this.subDirectories = subDirectories;
      datasets = subDirectories == null || subDirectories.isEmpty() ? 1 : subDirectories.size();
    } else {
      datasets = 1;
    }
    stepsPerDataset = queue.size();
    totalSteps = stepsPerDataset * datasets;
    createdDataFiles = new ArrayList<>();
    createdFeatureLists = new ArrayList<>();
    previousCreatedDataFiles = new ArrayList<>();
    previousCreatedFeatureLists = new ArrayList<>();
  }

  /**
   * Runs all tasks in a single {@link ThreadPoolTask} on the {@link TaskController#getExecutor()}
   * default executor
   *
   * @param method     for logging
   * @param tasksToRun list will be cleared after scheduling to avoid memory leak in long running
   *                   tasks. all tasks will be organized by ThreadPool
   * @return the {@link TaskStatus} of the carrier task reflecting the worst case of the sub tasks
   * meaning that if any had an error > cancel > finished
   */
  private TaskStatus runInTaskPool(final MZmineProcessingModule method,
      final List<Task> tasksToRun) {
    TaskController taskController = MZmineCore.getTaskController();
    var description = "%s on %d items".formatted(method.getName(), tasksToRun.size());
    var threadPoolTask = ThreadPoolTask.createDefaultTaskManagerPool(description, tasksToRun);
    // clear tasks to not leak the long running tasks by keeping them alive
    tasksToRun.clear();
    // Submit the tasks to the task controller for processing
    // this runs the ThreadPoolTask on this thread (blocking) and calls all sub tasks in the default executor
    WrappedTask finishedTask = taskController.runTaskOnThisThreadBlocking(threadPoolTask);
    if (finishedTask == null) {
      return TaskStatus.ERROR;
    }

    String errorMessage = finishedTask.getErrorMessage();
    if (finishedTask.getStatus() == TaskStatus.ERROR) {
      // not needed as this is done by caller - just important to set this task to error
//      if (DesktopService.isGUI()) {
//        FxThread.runLater(
//            () -> DialogLoggerUtil.showErrorDialog("Batch had errors and stopped", errorMessage));
//      }
      error(errorMessage);
      return TaskStatus.ERROR;
    }

    return finishedTask.getActualTask().getStatus();
  }

  @Override
  public void run() {
    Instant batchStart = Instant.now();
    setStatus(TaskStatus.PROCESSING);
    logger.info("Starting a batch of " + totalSteps + " steps");

    try {
      // run batch that may fail with error message
      // BatchTask is often run directly without WrappedTask, so handling of error message is important
      runBatchQueue();
    } catch (Throwable e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      // in case of an exception
      // regular errors are already handled in the runBatchQueue methods
      if (getErrorMessage() != null) {
        if (DesktopService.isGUI()) {
          FxThread.runLater(
              () -> DialogLoggerUtil.showErrorDialog("EXCEPTION Batch had errors and stopped",
                  getErrorMessage()));
        }
      }
      return;
    }

    if (getStatus() == TaskStatus.ERROR && getErrorMessage() != null) {
      logger.log(Level.WARNING, getErrorMessage());
    }

    if (isCanceled()) {
      return;
    }

    logger.info("Finished a batch of " + totalSteps + " steps");
    setStatus(TaskStatus.FINISHED);
    Duration duration = Duration.between(batchStart, Instant.now());
    if (runGCafterBatchStep) {
      System.gc();
    }
    stepTimes.add(new StepTimeMeasurement(0, "WHOLE BATCH", duration, runGCafterBatchStep));
    printBatchTimes();
  }

  private void runBatchQueue() {
    int errorDataset = 0;
    currentDataset = -1;
    String datasetName = "";
    // Process individual batch steps
    for (int i = 0; i < totalSteps; i++) {
      // at the end of one dataset, clear the project and start over again
      if (useAdvanced && currentStep() == 0) {
        // clear the old project
        ProjectService.getProjectManager().clearProject();
        currentDataset++;

        // print step times
        if (!stepTimes.isEmpty()) {
          printBatchTimes();
          stepTimes.clear();
        }

        // change files
        File datasetDir = subDirectories.get(currentDataset);
        datasetName = datasetDir.getName();
        File[] allFiles = FileAndPathUtil.findFilesInDirFlat(datasetDir,
            ExtensionFilters.ALL_MS_DATA_FILTER, searchSubdirs);

        logger.info(
            String.format("Processing batch dataset %s (%d/%d)", datasetName, currentDataset + 1,
                datasets));

        if (allFiles.length != 0) {
          // set files to import
          if (!queue.setImportFiles(allFiles, null, null)) {
            if (skipOnError) {
              processedSteps += stepsPerDataset;
              continue;
            } else {
              setStatus(TaskStatus.ERROR);
              setErrorMessage(
                  "Could not set data files in advanced batch mode. Will cancel all jobs. "
                  + datasetName);
              return;
            }
          }
          // set files to output
          setOutputFiles(parentDir, createResultsDir, datasetName);

        } else {
          errorDataset++;
          logger.info("No data files found in directory: " + datasetName);
          // no data files
          if (skipOnError) {
            processedSteps += stepsPerDataset;
            continue;
          } else {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("No data files found in directory: " + datasetName);
            return;
          }
        }
      }

      // run step
      final int stepNumber = i % stepsPerDataset;
      Instant start = Instant.now();

      // the heavy lifting
      processQueueStep(stepNumber);
      processedSteps++;

      Duration duration = Duration.between(start, Instant.now());
      if (runGCafterBatchStep) {
        System.gc();
      }
      stepTimes.add(
          new StepTimeMeasurement(stepNumber + 1, queue.get(stepNumber).getModule().getName(),
              duration, runGCafterBatchStep));

      // If we are canceled or ran into error, stop here
      if (getStatus() == TaskStatus.ERROR) {
        errorDataset++;
        DialogLoggerUtil.showDialog(AlertType.ERROR, "Batch processing error",
            "An error occurred while processing batch step %d.\n%s".formatted(stepNumber + 1,
                getErrorMessage()), false);
        if (skipOnError && datasets - currentDataset > 0) {
          // skip to next dataset
          logger.info("Error in dataset: " + datasetName + " total error datasets:" + errorDataset);
          processedSteps = (processedSteps / stepsPerDataset + 1) * stepsPerDataset;
          continue;
        }
      }
      if (isCanceled()) {
        return;
      }
    }
  }

  private void printBatchTimes() {
    String csv = CsvWriter.writeToString(stepTimes, StepTimeMeasurement.class, '\t', true);
    logger.info("""
        Timing: Whole batch took %.3f seconds to finish
        %s""".formatted(stepTimes.getLast().secondsToFinish(), csv));

//    CsvWriter.writeToFile();
//    logger.info(csv);
//    String times = stepTimes.stream().map(Objects::toString).collect(Collectors.joining("\n"));
//    logger.info(STR."""
//    Timing: Whole batch took \{duration} to finish
//    \{times}""");
  }

  private void setOutputFiles(final File parentDir, final boolean createResultsDir,
      final String datasetName) {
    File exportPath = createDatasetExportPath(parentDir, createResultsDir, datasetName);
    var countChanged = ChangeOutputFilesUtils.applyTo(queue, exportPath);

    if (countChanged == 0) {
      logger.info(
          "No output steps were changes... Make sure to include steps that include filename parameters for export");
      throw new IllegalStateException(
          "No output steps were changes... Make sure to include steps that include filename parameters for export");
    } else {
      logger.info("Changed output for n steps=" + countChanged);
    }
  }

  private File createDatasetExportPath(final File parentDir, final boolean createResultsDir,
      final String datasetName) {
    if (createResultsDir) {
      return Paths.get(parentDir.getPath(), "results", datasetName, datasetName).toFile();
    } else {
      return Paths.get(parentDir.getPath(), datasetName, datasetName).toFile();
    }
  }

  public List<StepTimeMeasurement> getStepTimes() {
    return stepTimes;
  }

  public int currentStep() {
    return processedSteps % stepsPerDataset;
  }

  private void processQueueStep(int stepNumber) {
    logger.info("Starting step # " + (stepNumber + 1));

    // Run next step of the batch
    MZmineProcessingStep<?> currentStep = queue.get(stepNumber);
    MZmineProcessingModule method = (MZmineProcessingModule) currentStep.getModule();
    ParameterSet batchStepParameters = currentStep.getParameterSet();

    final List<FeatureList> beforeFeatureLists = project.getCurrentFeatureLists();
    final List<RawDataFile> beforeDataFiles = project.getCurrentRawDataFiles();

    // If the last step did not produce any data files or feature lists, use
    // the ones from the previous step
    if (createdDataFiles.isEmpty()) {
      createdDataFiles = previousCreatedDataFiles;
    }
    if (createdFeatureLists.isEmpty()) {
      createdFeatureLists = previousCreatedFeatureLists;
    }

    // Update the RawDataFilesParameter parameters to reflect the current
    // state of the batch
    for (Parameter<?> p : batchStepParameters.getParameters()) {
      if (p instanceof RawDataFilesParameter rdp) {
        RawDataFile[] createdFiles = createdDataFiles.toArray(new RawDataFile[0]);
        final RawDataFilesSelection selectedFiles = rdp.getValue();
        if (selectedFiles == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Invalid parameter settings for module " + method.getName() + ": "
                          + "Missing parameter value for " + p.getName());
          return;
        }
        selectedFiles.setBatchLastFiles(createdFiles);
      }
    }

    if (!setBatchlastFeatureListsToParamSet(method, batchStepParameters)) {
      return;
    }

    // Check if the parameter settings are valid
    ArrayList<String> messages = new ArrayList<String>();
    boolean paramsCheck = batchStepParameters.checkParameterValues(messages);
    if (!paramsCheck) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(
          "Invalid parameter settings for module " + method.getName() + ": " + Arrays.toString(
              messages.toArray()));
      return;
    }

    List<Task> currentStepTasks = new ArrayList<>();
    Instant moduleCallDate = Instant.now();
    logger.finest(() -> "Module " + method.getName() + " called at " + moduleCallDate.toString());
    ExitCode exitCode = method.runModule(project, batchStepParameters, currentStepTasks,
        moduleCallDate);

    if (exitCode != ExitCode.OK) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not start batch step " + method.getName());
      return;
    }

    // If current step didn't produce any tasks, continue with next step
    if (currentStepTasks.isEmpty()) {
      // this might be the case for AllSpectralDataImportModule if all files are already loaded
      // special option to skip already imported files in the AllSpectralDataImportParameters
      // add skipped files
      setLastFilesIfAllDataImportStep(batchStepParameters);
      if (getStatus() == TaskStatus.ERROR) {
        return;
      }
      return;
    }

    // submit as ThreadPoolTask
    final TaskStatus status;
    // create ThreadPool
    if (currentStepTasks.size() > 1) {
      status = runInTaskPool(method, currentStepTasks);
    } else {
      // Submit the tasks to the task controller for processing
      status = runTasksIndividually(currentStepTasks);
    }

    if (status != TaskStatus.FINISHED) {
      return;
    }

    createdDataFiles = new ArrayList<>(project.getCurrentRawDataFiles());
    createdFeatureLists = new ArrayList<>(project.getCurrentFeatureLists());
    createdDataFiles.removeAll(beforeDataFiles);
    createdFeatureLists.removeAll(beforeFeatureLists);

    // special option to skip already imported files in the AllSpectralDataImportParameters
    // add skipped files
    setLastFilesIfAllDataImportStep(batchStepParameters);
    if (getStatus() == TaskStatus.ERROR) {
      return;
    }

    // Clear the saved data files and feature lists. Save them to the
    // "previous" lists, in case the next step does not produce any new data
    if (!createdDataFiles.isEmpty()) {
      previousCreatedDataFiles = createdDataFiles;
    }
    if (!createdFeatureLists.isEmpty()) {
      previousCreatedFeatureLists = createdFeatureLists;
    }
  }

  /**
   * Runs all tasks in the {@link TaskController}
   *
   * @param tasksToRun this list will be cleared after scheduling the tasks to avoid memory leaks
   *                   during long running tasks
   * @return TaskStatus of all tasks, the first cancel or error will be returned without checking
   * the rest of the threads
   */
  private TaskStatus runTasksIndividually(List<Task> tasksToRun) {
    final WrappedTask[] wrappedTasks = TaskService.getController()
        .addTasks(tasksToRun.toArray(new Task[0]));
    tasksToRun.clear(); // do not keep the instance alive during long-running tasks

    TaskStatus result = TaskUtils.waitForTasksToFinish(this, wrappedTasks);

    // any error message - even if null this means that there was an error
    Optional<String> errorMessage = Arrays.stream(wrappedTasks)
        .filter(t -> t.getStatus() == TaskStatus.ERROR).map(WrappedTask::getErrorMessage)
        .findFirst();
    if (errorMessage.isPresent()) {
      // not needed as this is done by caller - just important to set this task to error
//      if (DesktopService.isGUI()) {
//        FxThread.runLater(() -> DialogLoggerUtil.showErrorDialog("Batch had errors and stopped",
//            errorMessage.get()));
//      }
      error(errorMessage.get());
      return TaskStatus.ERROR;
    }
    return result;
  }

  private void setLastFilesIfAllDataImportStep(final ParameterSet batchStepParameters) {
    if (AllSpectralDataImportParameters.isParameterSetClass(batchStepParameters)) {
      var loadedRawDataFiles = AllSpectralDataImportParameters.getLoadedRawDataFiles(
          ProjectService.getProject(), batchStepParameters);

      // because of concurrency - the project may not have all the new raw data files - but all newly created files are in createdDataFiles
      Set<RawDataFile> files = new HashSet<>(loadedRawDataFiles);
      files.addAll(createdDataFiles);
      loadedRawDataFiles = new ArrayList<>(files);

      // loaded should always be >= created as we are at most skipping files
      if (loadedRawDataFiles.size() >= createdDataFiles.size()) {
        createdDataFiles = loadedRawDataFiles;
        previousCreatedDataFiles = createdDataFiles;
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Wanted to set the last batch files from raw data import but failed because less data files were imported than expected.\n"
            + "expected %d  but loaded %d".formatted(loadedRawDataFiles.size(),
                createdDataFiles.size()));
      }
    }
  }

  /**
   * Recursively sets the last feature lists to the parameters since there might be embedded
   * parameters.
   *
   * @return false on error
   */
  private boolean setBatchlastFeatureListsToParamSet(MZmineProcessingModule method,
      ParameterSet batchStepParameters) {
    // Update the FeatureListsParameter parameters to reflect the current
    // state of the batch
    for (Parameter<?> p : batchStepParameters.getParameters()) {
      if (p instanceof FeatureListsParameter featureListsParameter) {
        FeatureList[] createdFlists = createdFeatureLists.toArray(new FeatureList[0]);
        final FeatureListsSelection selectedFeatureLists = featureListsParameter.getValue();
        if (selectedFeatureLists == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Invalid parameter settings for module " + method.getName() + ": "
                          + "Missing parameter value for " + p.getName());
          return false;
        }
        selectedFeatureLists.setBatchLastFeatureLists(createdFlists);
      } else if (p instanceof EmbeddedParameterSet embedded) {
        if (!setBatchlastFeatureListsToParamSet(method, embedded.getEmbeddedParameters())) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public TaskPriority getTaskPriority() {
    // to not block mzmine when run with single thread
    return TaskPriority.HIGH;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0) {
      return 0;
    }
    return (double) processedSteps / totalSteps;
  }

  @Override
  public String getTaskDescription() {
    if (datasets > 1) {
      if (stepsPerDataset == 0) {
        return "Batch mode";
      } else {
        return String.format("Batch step %d/%d of dataset %d/%d",
            Math.min(processedSteps % stepsPerDataset + 1, totalSteps), stepsPerDataset,
            currentDataset + 1, datasets);
      }
    } else {
      return String.format("Batch step %d/%d", Math.min(processedSteps + 1, totalSteps),
          totalSteps);
    }
  }

  public BatchQueue getQueueCopy() {
    return queue.clone();
  }
}
