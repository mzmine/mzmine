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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionParameters.AUTO_REMOVE;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionParameters.PEAK_RESOLVER;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionParameters.SUFFIX;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

public class DeconvolutionTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(DeconvolutionTask.class
	    .getName());

    // Peak lists.
    private final MZmineProject project;
    private final PeakList originalPeakList;
    private PeakList newPeakList;

    // Counters.
    private int processedRows;
    private int totalRows;

    // User parameters
    private final ParameterSet parameters;

	private RSessionWrapper rSession;
	private String errorMsg;
	private boolean userCanceled;


    /**
     * Create the task.
     *
     * @param list
     *            peak list to operate on.
     * @param parameterSet
     *            task parameters.
     */
    public DeconvolutionTask(final MZmineProject project, final PeakList list,
	    final ParameterSet parameterSet) {

	// Initialize.
	this.project = project;
	parameters = parameterSet;
	originalPeakList = list;
	newPeakList = null;
	processedRows = 0;
	totalRows = 0;

	this.userCanceled = false;
    }

    @Override
    public String getTaskDescription() {

	return "Peak recognition on " + originalPeakList;
    }

    @Override
    public double getFinishedPercentage() {

	return totalRows == 0 ? 0.0 : (double) processedRows
		/ (double) totalRows;
    }

    @Override
    public void run() {

	errorMsg = null;

	if (!isCanceled()) {

	    setStatus(TaskStatus.PROCESSING);
	    LOG.info("Started peak deconvolution on " + originalPeakList);

	    // Check raw data files.
	    if (originalPeakList.getNumberOfRawDataFiles() > 1) {

		setStatus(TaskStatus.ERROR);
		setErrorMessage("Peak deconvolution can only be performed on peak lists with a single raw data file");

	    } else {

		try {

			// Peak resolver.
			final MZmineProcessingStep<PeakResolver> resolver = parameters
					.getParameter(PEAK_RESOLVER).getValue();

			if (resolver.getModule().getRequiresR()) {
				// Check R availability, by trying to open the connection.
				String[] reqPackages = resolver.getModule().getRequiredRPackages();
				String[] reqPackagesVersions = resolver.getModule().getRequiredRPackagesVersions();
				String callerFeatureName = resolver.getModule().getName();
				this.rSession = new RSessionWrapper(callerFeatureName, /*this.rEngineType,*/ reqPackages, reqPackagesVersions);
				this.rSession.open();
			} else {
				this.rSession = null;
			}

		    // Deconvolve peaks.
			newPeakList = resolvePeaks(originalPeakList, this.rSession);

		    if (!isCanceled()) {

			// Add new peaklist to the project.

			project.addPeakList(newPeakList);

			// Remove the original peaklist if requested.
			if (parameters.getParameter(AUTO_REMOVE).getValue()) {
			    project.removePeakList(originalPeakList);
			}

			setStatus(TaskStatus.FINISHED);
			LOG.info("Finished peak recognition on "
				+ originalPeakList);
		    }
			// Turn off R instance.
			if (this.rSession != null) this.rSession.close(false);

		} catch (RSessionWrapperException e) {
			//if (!this.userCanceled) {
			errorMsg = "'R computing error' during CentWave detection. \n" + e.getMessage();
			//}
		} catch (Exception e) {
			//if (!this.userCanceled) {
			errorMsg = "'Unknown error' during CentWave detection. \n" + e.getMessage();
			//}
		} catch (Throwable t) {

		    setStatus(TaskStatus.ERROR);
		    setErrorMessage(t.getMessage());
		    LOG.log(Level.SEVERE, "Peak deconvolution error", t);
		}

		// Turn off R instance, once task ended UNgracefully.
		try {
			if (this.rSession != null && !this.userCanceled) rSession.close(this.userCanceled);
		}
		catch (RSessionWrapperException e) {
			if (!this.userCanceled) {
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
	    }
	}
    }

    /**
     * Deconvolve a chromatogram into separate peaks.
     *
     * @param peakList
     *            holds the chromatogram to deconvolve.
     * @return a new peak list holding the resolved peaks.
     * @throws RSessionWrapperException 
     */
	private PeakList resolvePeaks(final PeakList peakList, RSessionWrapper rSession) throws RSessionWrapperException {

	// Get data file information.
	final RawDataFile dataFile = peakList.getRawDataFile(0);
	final int[] scanNumbers = dataFile.getScanNumbers(1);
	final int scanCount = scanNumbers.length;
	final double[] retentionTimes = new double[scanCount];
	for (int i = 0; i < scanCount; i++) {

	    retentionTimes[i] = dataFile.getScan(scanNumbers[i])
		    .getRetentionTime();
	}

	// Peak resolver.

	final MZmineProcessingStep<PeakResolver> resolver = parameters
		.getParameter(PEAK_RESOLVER).getValue();

	// Create new peak list.
	final PeakList resolvedPeaks = new SimplePeakList(peakList + " "
		+ parameters.getParameter(SUFFIX).getValue(), dataFile);

	// Load previous applied methods.
	for (final PeakListAppliedMethod method : peakList.getAppliedMethods()) {

	    resolvedPeaks.addDescriptionOfAppliedTask(method);
	}

	// Add task description to peak list.
	resolvedPeaks
		.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Peak deconvolution by " + resolver, resolver
				.getParameterSet()));

	// Initialise counters.
	processedRows = 0;
	totalRows = peakList.getNumberOfRows();
	int peakId = 1;

	// Process each chromatogram.
	final Feature[] chromatograms = peakList.getPeaks(dataFile);
	final int chromatogramCount = chromatograms.length;
	for (int index = 0; !isCanceled() && index < chromatogramCount; index++) {

	    final Feature chromatogram = chromatograms[index];

	    // Load the intensities into array.
	    final double[] intensities = new double[scanCount];
	    for (int i = 0; i < scanCount; i++) {

		final DataPoint dp = chromatogram.getDataPoint(scanNumbers[i]);
		intensities[i] = dp != null ? dp.getIntensity() : 0.0;
	    }

	    // Resolve peaks.
	    final PeakResolver resolverModule = resolver.getModule();
	    final ParameterSet resolverParams = resolver.getParameterSet();
	    final Feature[] peaks = resolverModule.resolvePeaks(chromatogram,
					scanNumbers, retentionTimes, intensities, resolverParams,
					rSession);

	    // Add peaks to the new peak list.
	    for (final Feature peak : peaks) {

		final PeakListRow newRow = new SimplePeakListRow(peakId++);
		newRow.addPeak(dataFile, peak);
		resolvedPeaks.addRow(newRow);
	    }

	    processedRows++;
	}

	return resolvedPeaks;
	}

	@Override
	public void cancel() {

		this.userCanceled = true;

		super.cancel();
		// Turn off R instance, if already existing.
		try {
			if (this.rSession != null) this.rSession.close(true);
		}
		catch (RSessionWrapperException e) {
			// Silent, always...
		}
    }
}
