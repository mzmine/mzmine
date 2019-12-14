/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.AUTO_REMOVE;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.PEAK_RESOLVER;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.RetentionTimeMSMS;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.SUFFIX;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.mzRangeMSMS;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.maths.CenterFunction;

public class DeconvolutionTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(DeconvolutionTask.class.getName());

    // Feature lists.
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
    private boolean setMSMSRange, setMSMSRT;
    private double msmsRange, RTRangeMSMS;

    // function to find center mz of all feature data points
    private final CenterFunction mzCenterFunction;

    /**
     * Create the task.
     * 
     * @param list
     *            feature list to operate on.
     * @param parameterSet
     *            task parameters.
     */
    public DeconvolutionTask(final MZmineProject project, final PeakList list,
            final ParameterSet parameterSet, CenterFunction mzCenterFunction) {

        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        processedRows = 0;
        totalRows = 0;
        this.mzCenterFunction = mzCenterFunction;
    }

    @Override
    public String getTaskDescription() {

        return "Peak recognition on " + originalPeakList;
    }

    @Override
    public double getFinishedPercentage() {

        return totalRows == 0 ? 0.0
                : (double) processedRows / (double) totalRows;
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
                setErrorMessage(
                        "Peak deconvolution can only be performed on feature lists with a single raw data file");

            } else {

                try {

                    // Peak resolver.
                    final MZmineProcessingStep<PeakResolver> resolver = parameters
                            .getParameter(PEAK_RESOLVER).getValue();

                    if (resolver.getModule().getRequiresR()) {
                        // Check R availability, by trying to open the
                        // connection.
                        String[] reqPackages = resolver.getModule()
                                .getRequiredRPackages();
                        String[] reqPackagesVersions = resolver.getModule()
                                .getRequiredRPackagesVersions();
                        String callerFeatureName = resolver.getModule()
                                .getName();

                        REngineType rEngineType = resolver.getModule()
                                .getREngineType(resolver.getParameterSet());
                        this.rSession = new RSessionWrapper(rEngineType,
                                callerFeatureName, reqPackages,
                                reqPackagesVersions);
                        this.rSession.open();
                    } else {
                        this.rSession = null;
                    }

                    // Deconvolve peaks.
                    newPeakList = resolvePeaks(originalPeakList, this.rSession);

                    if (!isCanceled()) {

                        // Add new peaklist to the project.
                        project.addPeakList(newPeakList);

                        // Add quality parameters to peaks
                        QualityParameters
                                .calculateQualityParameters(newPeakList);

                        // Remove the original peaklist if requested.
                        if (parameters.getParameter(AUTO_REMOVE).getValue()) {
                            project.removePeakList(originalPeakList);
                        }

                        setStatus(TaskStatus.FINISHED);
                        LOG.info("Finished peak recognition on "
                                + originalPeakList);
                    }
                    // Turn off R instance.
                    if (this.rSession != null)
                        this.rSession.close(false);

                } catch (RSessionWrapperException e) {
                    errorMsg = "'R computing error' during CentWave detection. \n"
                            + e.getMessage();
                } catch (Exception e) {
                    errorMsg = "'Unknown error' during CentWave detection. \n"
                            + e.getMessage();
                } catch (Throwable t) {

                    setStatus(TaskStatus.ERROR);
                    setErrorMessage(t.getMessage());
                    LOG.log(Level.SEVERE, "Peak deconvolution error", t);
                }

                // Turn off R instance, once task ended UNgracefully.
                try {
                    if (this.rSession != null && !isCanceled())
                        rSession.close(isCanceled());
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
            }
        }
    }

    /**
     * Deconvolve a chromatogram into separate peaks.
     * 
     * @param peakList
     *            holds the chromatogram to deconvolve.
     * @param mzCenterFunction2
     * @return a new feature list holding the resolved peaks.
     * @throws RSessionWrapperException
     */
    private PeakList resolvePeaks(final PeakList peakList,
            RSessionWrapper rSession) throws RSessionWrapperException {

        // Get data file information.
        final RawDataFile dataFile = peakList.getRawDataFile(0);

        // Peak resolver.
        final MZmineProcessingStep<PeakResolver> resolver = parameters
                .getParameter(PEAK_RESOLVER).getValue();
        // set msms pairing range
        this.setMSMSRange = parameters.getParameter(mzRangeMSMS).getValue();
        if (setMSMSRange)
            this.msmsRange = parameters.getParameter(mzRangeMSMS)
                    .getEmbeddedParameter().getValue();
        else
            this.msmsRange = 0;

        this.setMSMSRT = parameters.getParameter(RetentionTimeMSMS).getValue();
        if (setMSMSRT)
            this.RTRangeMSMS = parameters.getParameter(RetentionTimeMSMS)
                    .getEmbeddedParameter().getValue();
        else
            this.RTRangeMSMS = 0;

        // Create new feature list.
        final PeakList resolvedPeaks = new SimplePeakList(
                peakList + " " + parameters.getParameter(SUFFIX).getValue(),
                dataFile);

        // Load previous applied methods.
        for (final PeakListAppliedMethod method : peakList
                .getAppliedMethods()) {

            resolvedPeaks.addDescriptionOfAppliedTask(method);
        }

        // Add task description to feature list.
        resolvedPeaks
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        "Peak deconvolution by " + resolver,
                        resolver.getParameterSet()));

        // Initialise counters.
        processedRows = 0;
        totalRows = peakList.getNumberOfRows();
        int peakId = 1;

        // Process each chromatogram.
        final PeakListRow[] peakListRows = peakList.getRows();
        final int chromatogramCount = peakListRows.length;
        for (int index = 0; !isCanceled()
                && index < chromatogramCount; index++) {

            final PeakListRow currentRow = peakListRows[index];
            final Feature chromatogram = currentRow.getPeak(dataFile);

            // Resolve peaks.
            final PeakResolver resolverModule = resolver.getModule();
            final ParameterSet resolverParams = resolver.getParameterSet();
            final ResolvedPeak[] peaks = resolverModule.resolvePeaks(
                    chromatogram, resolverParams, rSession, mzCenterFunction,
                    msmsRange, RTRangeMSMS);

            // Add peaks to the new feature list.
            for (final ResolvedPeak peak : peaks) {

                peak.setParentChromatogramRowID(currentRow.getID());

                final PeakListRow newRow = new SimplePeakListRow(peakId++);
                newRow.addPeak(dataFile, peak);
                newRow.setPeakInformation(peak.getPeakInformation());
                resolvedPeaks.addRow(newRow);
            }

            processedRows++;
        }

        return resolvedPeaks;
    }

    @Override
    public void cancel() {

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
