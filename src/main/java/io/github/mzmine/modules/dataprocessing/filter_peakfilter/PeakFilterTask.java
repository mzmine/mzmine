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

package io.github.mzmine.modules.dataprocessing.filter_peakfilter;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import com.google.common.primitives.Booleans;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.PeakUtils;

/**
 * Filters out peaks from feature list.
 */
public class PeakFilterTask extends AbstractTask {

    // Logger
    private static final Logger LOG = Logger
            .getLogger(PeakFilterTask.class.getName());

    // Feature lists
    private final MZmineProject project;
    private final PeakList origPeakList;
    private PeakList filteredPeakList;

    // Processed rows counter
    private int processedRows, totalRows;

    // Parameters
    private final ParameterSet parameters;

    /**
     * Create the task.
     *
     * @param list
     *            feature list to process.
     * @param parameterSet
     *            task parameters.
     */
    public PeakFilterTask(final MZmineProject project, final PeakList list,
            final ParameterSet parameterSet) {

        // Initialize
        this.project = project;
        parameters = parameterSet;
        origPeakList = list;
        filteredPeakList = null;
        processedRows = 0;
        totalRows = 0;
    }

    @Override
    public double getFinishedPercentage() {
        return totalRows == 0 ? 0.0
                : (double) processedRows / (double) totalRows;
    }

    @Override
    public String getTaskDescription() {
        return "Filtering feature list";
    }

    @Override
    public void run() {

        if (isCanceled()) {
            return;
        }

        try {
            setStatus(TaskStatus.PROCESSING);
            LOG.info("Filtering feature list");

            // Filter the feature list
            filteredPeakList = filterPeakList(origPeakList);

            if (!isCanceled()) {

                // Add new peaklist to the project
                project.addPeakList(filteredPeakList);

                // Remove the original peaklist if requested
                if (parameters.getParameter(PeakFilterParameters.AUTO_REMOVE)
                        .getValue()) {
                    project.removePeakList(origPeakList);
                }
                setStatus(TaskStatus.FINISHED);
                LOG.info("Finished feature list filter");
            }
        } catch (Throwable t) {

            setErrorMessage(t.getMessage());
            setStatus(TaskStatus.ERROR);
            LOG.log(Level.SEVERE, "Feature list filter error", t);
        }

    }

    /**
     * Filter the feature list.
     *
     * @param peakList
     *            feature list to filter.
     * @return a new feature list with entries of the original feature list that
     *         pass the filtering.
     */
    private PeakList filterPeakList(final PeakList peakList) {

        // Make a copy of the peakList
        final PeakList newPeakList = new SimplePeakList(
                peakList.getName() + ' ' + parameters
                        .getParameter(RowsFilterParameters.SUFFIX).getValue(),
                peakList.getRawDataFiles());

        // Get parameters - which filters are active
        final boolean filterByDuration = parameters
                .getParameter(PeakFilterParameters.PEAK_DURATION).getValue();
        final boolean filterByArea = parameters
                .getParameter(PeakFilterParameters.PEAK_AREA).getValue();
        final boolean filterByHeight = parameters
                .getParameter(PeakFilterParameters.PEAK_HEIGHT).getValue();
        final boolean filterByDatapoints = parameters
                .getParameter(PeakFilterParameters.PEAK_DATAPOINTS).getValue();
        final boolean filterByFWHM = parameters
                .getParameter(PeakFilterParameters.PEAK_FWHM).getValue();
        final boolean filterByTailingFactor = parameters
                .getParameter(PeakFilterParameters.PEAK_TAILINGFACTOR)
                .getValue();
        final boolean filterByAsymmetryFactor = parameters
                .getParameter(PeakFilterParameters.PEAK_ASYMMETRYFACTOR)
                .getValue();
        final boolean filterByMS2 = parameters
                .getParameter(PeakFilterParameters.MS2_Filter).getValue();

        // Loop through all rows in feature list
        final PeakListRow[] rows = peakList.getRows();
        totalRows = rows.length;
        for (processedRows = 0; !isCanceled()
                && processedRows < totalRows; processedRows++) {
            final PeakListRow row = rows[processedRows];
            final RawDataFile[] rawdatafiles = row.getRawDataFiles();
            int totalRawDataFiles = rawdatafiles.length;
            boolean[] keepPeak = new boolean[totalRawDataFiles];

            for (int i = 0; i < totalRawDataFiles; i++) {
                // Peak values
                keepPeak[i] = true;
                final Feature peak = row.getPeak(rawdatafiles[i]);
                final double peakDuration = peak.getRawDataPointsRTRange()
                        .upperEndpoint()
                        - peak.getRawDataPointsRTRange().lowerEndpoint();
                final double peakArea = peak.getArea();
                final double peakHeight = peak.getHeight();
                final int peakDatapoints = peak.getScanNumbers().length;
                final int msmsScanNumber = peak
                        .getMostIntenseFragmentScanNumber();

                Double peakFWHM = peak.getFWHM();
                Double peakTailingFactor = peak.getTailingFactor();
                Double peakAsymmetryFactor = peak.getAsymmetryFactor();
                if (peakFWHM == null) {
                    peakFWHM = -1.0;
                }
                if (peakTailingFactor == null) {
                    peakTailingFactor = -1.0;
                }
                if (peakAsymmetryFactor == null) {
                    peakAsymmetryFactor = -1.0;
                }

                // Check Duration
                if (filterByDuration) {
                    final Range<Double> durationRange = parameters
                            .getParameter(PeakFilterParameters.PEAK_DURATION)
                            .getEmbeddedParameter().getValue();
                    if (!durationRange.contains(peakDuration)) {
                        // Mark peak to be removed
                        keepPeak[i] = false;
                    }
                }

                // Check Area
                if (filterByArea) {
                    final Range<Double> areaRange = parameters
                            .getParameter(PeakFilterParameters.PEAK_AREA)
                            .getEmbeddedParameter().getValue();
                    if (!areaRange.contains(peakArea)) {
                        // Mark peak to be removed
                        keepPeak[i] = false;
                    }
                }

                // Check Height
                if (filterByHeight) {
                    final Range<Double> heightRange = parameters
                            .getParameter(PeakFilterParameters.PEAK_HEIGHT)
                            .getEmbeddedParameter().getValue();
                    if (!heightRange.contains(peakHeight)) {
                        // Mark peak to be removed
                        keepPeak[i] = false;
                    }
                }

                // Check # Data Points
                if (filterByDatapoints) {
                    final Range<Integer> datapointsRange = parameters
                            .getParameter(PeakFilterParameters.PEAK_DATAPOINTS)
                            .getEmbeddedParameter().getValue();
                    if (!datapointsRange.contains(peakDatapoints)) {
                        // Mark peak to be removed
                        keepPeak[i] = false;
                    }
                }

                // Check FWHM
                if (filterByFWHM) {
                    final Range<Double> fwhmRange = parameters
                            .getParameter(PeakFilterParameters.PEAK_FWHM)
                            .getEmbeddedParameter().getValue();
                    if (!fwhmRange.contains(peakFWHM)) {
                        // Mark peak to be removed
                        keepPeak[i] = false;
                    }
                }

                // Check Tailing Factor
                if (filterByTailingFactor) {
                    final Range<Double> tailingRange = parameters
                            .getParameter(
                                    PeakFilterParameters.PEAK_TAILINGFACTOR)
                            .getEmbeddedParameter().getValue();
                    if (!tailingRange.contains(peakTailingFactor)) {
                        // Mark peak to be removed
                        keepPeak[i] = false;
                    }
                }

                // Check height
                if (filterByAsymmetryFactor) {
                    final Range<Double> asymmetryRange = parameters
                            .getParameter(
                                    PeakFilterParameters.PEAK_ASYMMETRYFACTOR)
                            .getEmbeddedParameter().getValue();
                    if (!asymmetryRange.contains(peakAsymmetryFactor)) {
                        // Mark peak to be removed
                        keepPeak[i] = false;
                    }
                }

                // Check MS/MS filter
                if (filterByMS2) {
                    if (msmsScanNumber < 1)
                        keepPeak[i] = false;
                }
            }
            // empty row?
            boolean isEmpty = Booleans.asList(keepPeak).stream()
                    .allMatch(keep -> !keep);
            if (!isEmpty)
                newPeakList.addRow(copyPeakRow(row, keepPeak));

        }

        return newPeakList;
    }

    /**
     * Create a copy of a feature list row.
     */
    private static PeakListRow copyPeakRow(final PeakListRow row,
            final boolean[] keepPeak) {

        // Copy the feature list row.
        final PeakListRow newRow = new SimplePeakListRow(row.getID());
        PeakUtils.copyPeakListRowProperties(row, newRow);

        // Copy the peaks.
        int i = 0;
        for (final Feature peak : row.getPeaks()) {

            // Only keep peak if it fulfills the filter criteria
            if (keepPeak[i]) {
                final Feature newPeak = new SimpleFeature(peak);
                PeakUtils.copyPeakProperties(peak, newPeak);
                newRow.addPeak(peak.getDataFile(), newPeak);
            }
            i++;
        }

        return newRow;
    }
}
