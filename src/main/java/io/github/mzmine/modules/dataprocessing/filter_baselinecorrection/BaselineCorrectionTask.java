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

package io.github.mzmine.modules.dataprocessing.filter_baselinecorrection;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task that performs baseline correction.
 * 
 * Deeply modified to delegate baseline correction to various correctors (whose implement specific
 * methods by themselves). Those correctors all share a common behavior by inheriting from the base
 * class "BaselineCorrector", and apply there specific way of building the baselines via the various
 * algorithms implemented in the sub-package
 * "io.github.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors" .
 */
public class BaselineCorrectionTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(BaselineCorrectionTask.class.getName());

  // Original data file and newly created baseline corrected file.
  private final MZmineProject project;
  private final RawDataFile origDataFile;
  private RawDataFile correctedDataFile;

  // Remove original data file.
  private final boolean removeOriginal;

  // Baseline corrector processing step.
  private final MZmineProcessingStep<BaselineCorrector> baselineCorrectorProcStep;

  // Common parameters.
  private final ParameterSet commonParameters;

  private RSessionWrapper rSession;
  private String errorMsg;

  private REngineType rEngineType;

  /**
   * Creates the task.
   *  @param dataFile raw data file on which to perform correction.
   * @param parameters correction parameters.
   * @param storage
   */
  public BaselineCorrectionTask(MZmineProject project, final RawDataFile dataFile,
      final ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    // Initialize.
    this.project = project;
    this.origDataFile = dataFile;
    this.correctedDataFile = null;
    this.removeOriginal =
        parameters.getParameter(BaselineCorrectionParameters.REMOVE_ORIGINAL).getValue();
    this.baselineCorrectorProcStep =
        parameters.getParameter(BaselineCorrectionParameters.BASELINE_CORRECTORS).getValue();

    this.rEngineType =
        parameters.getParameter(BaselineCorrectionParameters.RENGINE_TYPE).getValue();

    this.commonParameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Correcting baseline for " + origDataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return baselineCorrectorProcStep.getModule().getFinishedPercentage(origDataFile);
  }

  @Override
  public void run() {

    errorMsg = null;

    // Update the status of this task.
    setStatus(TaskStatus.PROCESSING);

    try {

      // Check R availability, by trying to open the connection.
      String[] reqPackages = this.baselineCorrectorProcStep.getModule().getRequiredRPackages();
      String callerFeatureName = this.baselineCorrectorProcStep.getModule().getName();
      this.rSession = new RSessionWrapper(rEngineType, callerFeatureName, reqPackages, null);

      this.rSession.open();

      this.baselineCorrectorProcStep.getModule().initProgress(origDataFile);

      final RawDataFile correctedDataFile =
          this.baselineCorrectorProcStep.getModule().correctDatafile(this.rSession, origDataFile,
              baselineCorrectorProcStep.getParameterSet(), this.commonParameters, getMemoryMapStorage());

      // If this task was canceled, stop processing.
      if (!isCanceled() && correctedDataFile != null) {

        this.correctedDataFile = correctedDataFile;

        for (FeatureListAppliedMethod appliedMethod : origDataFile.getAppliedMethods()) {
          this.correctedDataFile.getAppliedMethods().add(appliedMethod);
        }
        this.correctedDataFile.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
            BaselineCorrectionModule.class, commonParameters, getModuleCallDate()));
        // Add the newly created file to the project
        this.project.addFile(this.correctedDataFile);

        // Remove the original data file if requested.
        if (removeOriginal) {
          project.removeFile(origDataFile);
        }

        // Set task status to FINISHED
        setStatus(TaskStatus.FINISHED);

        logger.info("Baseline corrected " + origDataFile.getName());
      }
      // Turn off R instance, once task ended gracefully.
      if (!isCanceled())
        this.rSession.close(false);

    } catch (IOException | RSessionWrapperException e) {
      if (!isCanceled()) {
        errorMsg = "'R computing error' during baseline correction. \n" + e.getMessage();
      }
    } catch (Exception e) {
      if (!isCanceled()) {
        errorMsg = "'Unknown error' during baseline correction. \n" + e.getMessage();
      }
    }

    this.baselineCorrectorProcStep.getModule().setAbortProcessing(origDataFile, true);

    // Turn off R instance, once task ended UNgracefully.
    try {
      if (!isCanceled())
        this.rSession.close(isCanceled());
    } catch (RSessionWrapperException e) {
      if (!isCanceled()) {
        // Do not override potential previous error message.
        if (errorMsg == null) {
          errorMsg = e.getMessage();
        }
      } else {
        // User canceled: Silent.
      }
    }

    // Report error.
    if (errorMsg != null) {
      setErrorMessage(errorMsg);
      setStatus(TaskStatus.ERROR);
    }

    // ...
    this.baselineCorrectorProcStep.getModule().clearProgress(origDataFile);
  }

  @Override
  public void cancel() {

    // Ask running module to stop.
    baselineCorrectorProcStep.getModule().setAbortProcessing(origDataFile, true);

    super.cancel();
    // Turn off R instance, if already existing.
    try {
      if (this.rSession != null)
        this.rSession.close(true);
    } catch (RSessionWrapperException e) {
      // Silent, always...
    }
  }

}
