/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import net.sf.mzmine.data.*;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import java.util.logging.Level;
import java.util.logging.Logger;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionParameters.*;

public class DeconvolutionTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(DeconvolutionTask.class.getName());

    // Peak lists.
    private final PeakList originalPeakList;
    private PeakList newPeakList;

    // Counters.
    private int processedRows;
    private int totalRows;

    // User parameters
    private final ParameterSet parameters;

    /**
     * Create the task.
     *
     * @param list         peak list to operate on.
     * @param parameterSet task parameters.
     */
    public DeconvolutionTask(final PeakList list,
                             final ParameterSet parameterSet) {

        // Initialize.
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        processedRows = 0;
        totalRows = 0;
    }

    @Override
    public String getTaskDescription() {

        return "Peak recognition on " + originalPeakList;
    }

    @Override
    public double getFinishedPercentage() {

        return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
    }

    @Override
    public Object[] getCreatedObjects() {

        return new Object[]{newPeakList};
    }

    @Override
    public void run() {

        if (!isCanceled()) {

            setStatus(TaskStatus.PROCESSING);
            LOG.info("Started peak deconvolution on " + originalPeakList);

            // Check raw data files.
            if (originalPeakList.getNumberOfRawDataFiles() > 1) {

                setStatus(TaskStatus.ERROR);
                errorMessage = "Peak deconvolution can only be performed on peak lists with a single raw data file";

            } else {

                try {

                    // Deconvolve peaks.
                    newPeakList = resolvePeaks(originalPeakList);

                    if (!isCanceled()) {

                        // Add new peaklist to the project.
                        final MZmineProject currentProject = MZmineCore.getCurrentProject();
                        currentProject.addPeakList(newPeakList);

                        // Remove the original peaklist if requested.
                        if (parameters.getParameter(AUTO_REMOVE).getValue()) {

                            currentProject.removePeakList(originalPeakList);
                        }

                        setStatus(TaskStatus.FINISHED);
                        LOG.info("Finished peak recognition on " + originalPeakList);
                    }
                }
                catch (Throwable t) {

                    setStatus(TaskStatus.ERROR);
                    errorMessage = t.getMessage();
                    LOG.log(Level.SEVERE, "Peak deconvolution error", t);
                }
            }
        }
    }

    /**
     * Deconvolve a chromatogram into separate peaks.
     *
     * @param peakList holds the chromatogram to deconvolve.
     * @return a new peak list holding the resolved peaks.
     */
    @SuppressWarnings("unchecked")
    private PeakList resolvePeaks(final PeakList peakList) {

        // Get data file information.
        final RawDataFile dataFile = peakList.getRawDataFile(0);
        final int[] scanNumbers = dataFile.getScanNumbers(1);
        final int scanCount = scanNumbers.length;
        final double[] retentionTimes = new double[scanCount];
        for (int i = 0; i < scanCount; i++) {

            retentionTimes[i] = dataFile.getScan(scanNumbers[i]).getRetentionTime();
        }

        // Peak resolver.

        final MZmineProcessingStep<PeakResolver> resolver = parameters.getParameter(PEAK_RESOLVER).getValue();

        // Create new peak list.
        final PeakList resolvedPeaks =
                new SimplePeakList(peakList + " " + parameters.getParameter(SUFFIX).getValue(), dataFile);

        // Load previous applied methods.
        for (final PeakListAppliedMethod method : peakList.getAppliedMethods()) {

            resolvedPeaks.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peak list.
        resolvedPeaks
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Peak deconvolution by " + resolver,
                                                                             resolver.getParameterSet()));

        // Initialise counters.
        processedRows = 0;
        totalRows = peakList.getNumberOfRows();
        int peakId = 1;

        // Process each chromatogram.
        final ChromatographicPeak[] chromatograms = peakList.getPeaks(dataFile);
        final int chromatogramCount = chromatograms.length;
        for (int index = 0; !isCanceled() && index < chromatogramCount; index++) {

            final ChromatographicPeak chromatogram = chromatograms[index];

            // Load the intensities into array.
            final double[] intensities = new double[scanCount];
            for (int i = 0; i < scanCount; i++) {

                final DataPoint dp = chromatogram.getDataPoint(scanNumbers[i]);
                intensities[i] = dp != null ? dp.getIntensity() : 0.0;
            }

            // Resolve peaks.
            final PeakResolver resolverModule = resolver.getModule();
            final ParameterSet resolverParams = resolver.getParameterSet();
            final ChromatographicPeak[] peaks =
                    resolverModule.resolvePeaks(chromatogram, scanNumbers, retentionTimes, intensities, resolverParams);

            // Add peaks to the new peak list.
            for (final ChromatographicPeak peak : peaks) {

                final PeakListRow newRow = new SimplePeakListRow(peakId++);
                newRow.addPeak(dataFile, peak);
                resolvedPeaks.addRow(newRow);
            }

            processedRows++;
        }

        return resolvedPeaks;
    }
}
