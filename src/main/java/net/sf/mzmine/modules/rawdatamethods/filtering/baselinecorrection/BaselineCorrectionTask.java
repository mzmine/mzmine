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

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.RSession.RengineType;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final RawDataFile origDataFile;

    // Remove original data file.
    private final boolean removeOriginal;

    // Baseline corrector processing step
    private final MZmineProcessingStep<BaselineCorrector> baselineCorrectorProcStep;

    // Common parameters.
    private final ParameterSet commonParameters;

    private final RengineType rEngineType;
    private RSession rSession;
    private boolean userCanceled;

    /**
     * Creates the task.
     *
     * @param dataFile
     *            raw data file on which to perform correction.
     * @param parameters
     *            correction parameters.
     */
    public BaselineCorrectionTask(final RawDataFile dataFile,
	    final ParameterSet parameters) {

	// Initialize.
	// this.rEngineType =
	// parameters.getParameter(BaselineCorrectionParameters.RENGINE_TYPE).getValue();
	this.rEngineType = RengineType.JRIengine;

	this.origDataFile = dataFile;
	this.removeOriginal = parameters.getParameter(
		BaselineCorrectionParameters.REMOVE_ORIGINAL).getValue();
	this.baselineCorrectorProcStep = parameters.getParameter(
		BaselineCorrectionParameters.BASELINE_CORRECTORS).getValue();

	this.commonParameters = parameters;

	this.userCanceled = false;
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

	// Update the status of this task
	setStatus(TaskStatus.PROCESSING);

	try {

	    // Check R availability, by trying to open the connection
	    try {
		String[] reqPackages = this.baselineCorrectorProcStep
			.getModule().getRequiredRPackages();
		this.rSession = new RSession(this.rEngineType, reqPackages);
		this.rSession.open();
	    } catch (Throwable t) {
		throw new IllegalStateException(t.getMessage());
	    }

	    // Check & load required R packages
	    String missingPackage = null;
	    missingPackage = this.rSession.loadRequiredPackages();
	    if (missingPackage != null) {
		String msg = "The \""
			+ this.baselineCorrectorProcStep.getModule().getName()
			+ "\" requires "
			+ "the \""
			+ missingPackage
			+ "\" R package, which couldn't be loaded - is it installed in R?";
		throw new IllegalStateException(msg);
	    }

	    this.baselineCorrectorProcStep.getModule().initProgress(
		    origDataFile);

	    final RawDataFile correctedDataFile = this.baselineCorrectorProcStep
		    .getModule().correctDatafile(this.rSession, origDataFile,
			    baselineCorrectorProcStep.getParameterSet(),
			    this.commonParameters);

	    // If this task was canceled, stop processing
	    if (!isCanceled() && correctedDataFile != null) {

		// Add the newly created file to the project
		final MZmineProject project = MZmineCore.getCurrentProject();
		project.addFile(correctedDataFile);

		// Remove the original data file if requested
		if (removeOriginal) {
		    project.removeFile(origDataFile);
		}

		// Set task status to FINISHED
		setStatus(TaskStatus.FINISHED);

		LOG.info("Baseline corrected " + origDataFile.getName());
	    }
	    // Turn off R instance
	    this.rSession.close();

	} catch (Throwable t) {

	    this.baselineCorrectorProcStep.getModule().setAbortProcessing(
		    origDataFile, true);

	    if (!this.userCanceled) {
		LOG.log(Level.SEVERE, "Unknown baseline correction error.", t);
		setErrorMessage(t.getMessage());
		setStatus(TaskStatus.ERROR);
		// Turn off R instance
		this.rSession.close();
	    } else {
		this.rSession.close();
	    }
	}

	this.baselineCorrectorProcStep.getModule().clearProgress(origDataFile);
    }

    @Override
    public void cancel() {

	this.userCanceled = true;

	// Turn off R instance
	this.rSession.close();

	// Ask running module to stop
	baselineCorrectorProcStep.getModule().setAbortProcessing(origDataFile,
		true);

	super.cancel();
    }

}
