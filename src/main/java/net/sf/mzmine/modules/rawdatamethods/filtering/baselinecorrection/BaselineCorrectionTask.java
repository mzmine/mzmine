/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.REngineType;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

/**
 * Task that performs baseline correction.
 * 
 * Deeply modified to delegate baseline correction to various correctors (whose
 * implement specific methods by themselves). Those correctors all share a
 * common behavior by inheriting from the base class "BaselineCorrector", and
 * apply there specific way of building the baselines via the various algorithms
 * implemented in the sub-package
 * "net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors"
 * .
 */
public class BaselineCorrectionTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(BaselineCorrectionTask.class.getName());

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
     * 
     * @param dataFile
     *            raw data file on which to perform correction.
     * @param parameters
     *            correction parameters.
     */
    public BaselineCorrectionTask(MZmineProject project,
            final RawDataFile dataFile, final ParameterSet parameters) {

        // Initialize.
        this.project = project;
        this.origDataFile = dataFile;
        this.correctedDataFile = null;
        this.removeOriginal = parameters.getParameter(
                BaselineCorrectionParameters.REMOVE_ORIGINAL).getValue();
        this.baselineCorrectorProcStep = parameters.getParameter(
                BaselineCorrectionParameters.BASELINE_CORRECTORS).getValue();

        this.rEngineType = parameters.getParameter(
                BaselineCorrectionParameters.RENGINE_TYPE).getValue();
        
        this.commonParameters = parameters;
    }

    @Override
    public String getTaskDescription() {
        return "Correcting baseline for " + origDataFile;
    }

    @Override
    public double getFinishedPercentage() {
        return baselineCorrectorProcStep.getModule().getFinishedPercentage(
                origDataFile);
    }

    @Override
    public void run() {

        errorMsg = null;

        // Update the status of this task.
        setStatus(TaskStatus.PROCESSING);

        try {

            // Check R availability, by trying to open the connection.
            String[] reqPackages = this.baselineCorrectorProcStep.getModule()
                    .getRequiredRPackages();
            String callerFeatureName = this.baselineCorrectorProcStep
                    .getModule().getName();
            this.rSession = new RSessionWrapper(rEngineType,
                    callerFeatureName, reqPackages,
                    null);

            this.rSession.open();

            this.baselineCorrectorProcStep.getModule().initProgress(
                    origDataFile);

            final RawDataFile correctedDataFile = this.baselineCorrectorProcStep
                    .getModule().correctDatafile(this.rSession, origDataFile,
                            baselineCorrectorProcStep.getParameterSet(),
                            this.commonParameters);

            // If this task was canceled, stop processing.
            if (!isCanceled() && correctedDataFile != null) {

                this.correctedDataFile = correctedDataFile;

                // Add the newly created file to the project
                this.project.addFile(this.correctedDataFile);

                // Remove the original data file if requested.
                if (removeOriginal) {
                    project.removeFile(origDataFile);
                }

                // Set task status to FINISHED
                setStatus(TaskStatus.FINISHED);

                LOG.info("Baseline corrected " + origDataFile.getName());
            }
            // Turn off R instance, once task ended gracefully.
            if (!isCanceled())
                this.rSession.close(false);

        } catch (IOException | RSessionWrapperException e) {
            if (!isCanceled()) {
                errorMsg = "'R computing error' during baseline correction. \n"
                        + e.getMessage();
            }
        } catch (Exception e) {
            if (!isCanceled()) {
                errorMsg = "'Unknown error' during baseline correction. \n"
                        + e.getMessage();
            }
        }

        this.baselineCorrectorProcStep.getModule().setAbortProcessing(
                origDataFile, true);

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
        baselineCorrectorProcStep.getModule().setAbortProcessing(origDataFile,
                true);

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
