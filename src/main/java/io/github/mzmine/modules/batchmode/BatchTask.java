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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.GoogleAnalyticsTracker;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
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
  private int processedSteps;
  private final boolean useAdvanced;
  private final int datasets;
  private List<File> subDirectories;
  private List<RawDataFile> createdDataFiles;
  private List<RawDataFile> previousCreatedDataFiles;
  private List<FeatureList> createdFeatureLists;
  private List<FeatureList> previousCreatedFeatureLists;
  private Boolean skipOnError;
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
    this.project = project;
    this.queue = parameters.getParameter(BatchModeParameters.batchQueue).getValue();
    // advanced parameters
    useAdvanced = parameters.getParameter(BatchModeParameters.advanced).getValue();
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

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Starting a batch of " + totalSteps + " steps");

    int errorDataset = 0;
    currentDataset = -1;
    String datasetName = "";
    // Process individual batch steps
    for (int i = 0; i < totalSteps; i++) {
      // at the end of one dataset, clear the project and start over again
      if (useAdvanced && processedSteps % stepsPerDataset == 0) {
        // clear the old project
        MZmineCore.getProjectManager().clearProject();
        currentDataset++;

        // change files
        File datasetDir = subDirectories.get(currentDataset);
        datasetName = datasetDir.getName();
        File[] allFiles = FileAndPathUtil.findFilesInDirFlat(datasetDir,
            AllSpectralDataImportParameters.ALL_MS_DATA_FILTER, searchSubdirs);

        logger.info(
            String.format("Processing batch dataset %s (%d/%d)", datasetName, currentDataset + 1,
                datasets));

        if (allFiles.length != 0) {
          // set files to import
          if (!queue.setImportFiles(allFiles, null)) {
            if (skipOnError) {
              processedSteps += stepsPerDataset;
              continue;
            } else {
              setStatus(TaskStatus.ERROR);
              setErrorMessage("Could not set data files in advanced batch mode. Will cancel all jobs. " + datasetName);
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
      processQueueStep(i % stepsPerDataset);
      processedSteps++;

      // If we are canceled or ran into error, stop here
      if (isCanceled()) {
        return;
      } else if (getStatus() == TaskStatus.ERROR) {
        errorDataset++;
        if (skipOnError && datasets - currentDataset > 0) {
          // skip to next dataset
          logger.info("Error in dataset: " + datasetName + " total error datasets:" + errorDataset);
          processedSteps = (processedSteps / stepsPerDataset + 1) * stepsPerDataset;
          continue;
        } else {
          return;
        }
      }
    }

    logger.info("Finished a batch of " + totalSteps + " steps");
    setStatus(TaskStatus.FINISHED);
  }

  private void setOutputFiles(final File parentDir, final boolean createResultsDir,
      final String datasetName) {
    int changedOutputSteps = 0;
    for (MZmineProcessingStep<?> currentStep : queue) {
      // only change for export modules
      if (currentStep.getModule() instanceof MZmineRunnableModule mod && (
          mod.getModuleCategory() == MZmineModuleCategory.FEATURELISTEXPORT
          || mod.getModuleCategory() == MZmineModuleCategory.RAWDATAEXPORT)) {
        ParameterSet params = currentStep.getParameterSet();
        for (final Parameter<?> p : params.getParameters()) {
          if (p instanceof FileNameParameter fnp) {
            File old = fnp.getValue();
            String oldName = FileAndPathUtil.eraseFormat(old).getName();
            fnp.setValue(
                createDatasetExportPath(parentDir, createResultsDir, datasetName, oldName));
            changedOutputSteps++;
          }
        }
      }
    }
    if (changedOutputSteps == 0) {
      logger.info(
          "No output steps were changes... Make sure to include steps that include filename parameters for export");
      throw new IllegalStateException(
          "No output steps were changes... Make sure to include steps that include filename parameters for export");
    } else {
      logger.info("Changed output for n steps=" + changedOutputSteps);
    }
  }

  private File createDatasetExportPath(final File parentDir, final boolean createResultsDir,
      final String datasetName, final String oldName) {
    if (createResultsDir) {
      return Paths.get(parentDir.getPath(), "results", datasetName, oldName + datasetName).toFile();
    } else {
      return Paths.get(parentDir.getPath(), datasetName, oldName + datasetName).toFile();
    }
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
    } else {
      // track step by module
      GoogleAnalyticsTracker.trackModule(method);
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

    boolean allTasksFinished = false;

    // Submit the tasks to the task controller for processing
    WrappedTask[] currentStepWrappedTasks = MZmineCore.getTaskController()
        .addTasks(currentStepTasks.toArray(new Task[0]));
    currentStepTasks = null;

    while (!allTasksFinished) {

      // If we canceled the batch, cancel all running tasks
      if (isCanceled()) {
        for (WrappedTask stepTask : currentStepWrappedTasks) {
          stepTask.getActualTask().cancel();
        }
        return;
      }

      // First set to true, then check all tasks
      allTasksFinished = true;

      for (WrappedTask stepTask : currentStepWrappedTasks) {

        TaskStatus stepStatus = stepTask.getActualTask().getStatus();

        // If any of them is not finished, keep checking
        if (stepStatus != TaskStatus.FINISHED) {
          allTasksFinished = false;
        }

        // If there was an error, we have to stop the whole batch
        if (stepStatus == TaskStatus.ERROR) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(
              stepTask.getActualTask().getTaskDescription() + ": " + stepTask.getActualTask()
                  .getErrorMessage());
          return;
        }

        // If user canceled any of the tasks, we have to cancel the
        // whole batch
        if (stepStatus == TaskStatus.CANCELED) {
          setStatus(TaskStatus.CANCELED);
          for (WrappedTask t : currentStepWrappedTasks) {
            t.getActualTask().cancel();
          }
          return;
        }

      }

      // Wait 1s before checking the tasks again
      if (!allTasksFinished) {
        synchronized (this) {
          try {
            this.wait(1000);
          } catch (InterruptedException e) {
            // ignore
          }
        }
      }
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

  private void setLastFilesIfAllDataImportStep(final ParameterSet batchStepParameters) {
    if (AllSpectralDataImportParameters.isParameterSetClass(batchStepParameters)) {
      var loadedRawDataFiles = AllSpectralDataImportParameters.getLoadedRawDataFiles(
          MZmineCore.getProject(), batchStepParameters);

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

}
