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

package net.sf.mzmine.modules.peaklistmethods.filtering.peakcomparisonrowfilter;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;

import com.google.common.collect.Range;

/**
 * Filters out peak list rows.
 */
public class PeakComparisonRowFilterTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(PeakComparisonRowFilterTask.class.getName());
    // Peak lists.
    private final MZmineProject project;
    private final PeakList origPeakList;
    private PeakList filteredPeakList;
    // Processed rows counter
    private int processedRows, totalRows;
    // Parameters.
    private final ParameterSet parameters;

    /**
     * Create the task.
     *
     * @param list
     *            peak list to process.
     * @param parameterSet
     *            task parameters.
     */
    public PeakComparisonRowFilterTask(final MZmineProject project,
            final PeakList list, final ParameterSet parameterSet) {

        // Initialize.
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

        return "Filtering peak list rows based on peak comparisons";
    }

    @Override
    public void run() {

        try {
            setStatus(TaskStatus.PROCESSING);
            LOG.info("Filtering peak list rows");

            // Filter the peak list.
            filteredPeakList = filterPeakListRows(origPeakList);

            if (getStatus() == TaskStatus.ERROR)
                return;

            if (isCanceled())
                return;

            // Add new peaklist to the project
            project.addPeakList(filteredPeakList);

            // Remove the original peaklist if requested
            if (parameters
                    .getParameter(PeakComparisonRowFilterParameters.AUTO_REMOVE)
                    .getValue()) {
                project.removePeakList(origPeakList);
            }

            setStatus(TaskStatus.FINISHED);
            LOG.info("Finished peak comparison rows filter");

        } catch (Throwable t) {
            t.printStackTrace();
            setErrorMessage(t.getMessage());
            setStatus(TaskStatus.ERROR);
            LOG.log(Level.SEVERE, "Peak comparison row filter error", t);
        }

    }

    /**
     * Filter the peak list rows by comparing peaks within a row.
     *
     * @param peakList
     *            peak list to filter.
     * @return a new peak list with rows of the original peak list that pass the
     *         filtering.
     */
    private PeakList filterPeakListRows(final PeakList peakList) {

        // Create new peak list.
        final PeakList newPeakList = new SimplePeakList(
                peakList.getName() + ' '
                        + parameters
                                .getParameter(
                                        PeakComparisonRowFilterParameters.SUFFIX)
                                .getValue(),
                peakList.getRawDataFiles());

        // Copy previous applied methods.
        for (final PeakListAppliedMethod method : peakList
                .getAppliedMethods()) {

            newPeakList.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peakList.
        newPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                getTaskDescription(), parameters));

        // Get parameters.
        final boolean evalutateFoldChange = parameters
                .getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE)
                .getValue();
        final boolean evalutatePPMdiff = parameters
                .getParameter(PeakComparisonRowFilterParameters.MZ_PPM_DIFF)
                .getValue();
        final boolean evalutateRTdiff = parameters
                .getParameter(PeakComparisonRowFilterParameters.RT_DIFF)
                .getValue();
        final int columnIndex1 = parameters
                .getParameter(PeakComparisonRowFilterParameters.COLUMN_INDEX_1)
                .getValue();
        final int columnIndex2 = parameters
                .getParameter(PeakComparisonRowFilterParameters.COLUMN_INDEX_2)
                .getValue();
        final Range<Double> foldChangeRange = parameters
                .getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE)
                .getEmbeddedParameter().getValue();
        final Range<Double> ppmDiffRange = parameters
                .getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE)
                .getEmbeddedParameter().getValue();
        final Range<Double> rtDiffRange = parameters
                .getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE)
                .getEmbeddedParameter().getValue();

        // Setup variables
        final PeakListRow[] rows = peakList.getRows();
        RawDataFile rawDataFile1;
        RawDataFile rawDataFile2;
        Feature peak1;
        Feature peak2;
        totalRows = rows.length;
        final RawDataFile[] rawDataFiles = peakList.getRawDataFiles();

        boolean allCriteriaMatched = true;

        // Error handling. User tried to select a column from the peaklist that
        // doesn't exist.
        if (columnIndex1 > rawDataFiles.length) {
            setErrorMessage("Column 1 set too large.");
            setStatus(TaskStatus.ERROR);
            return null;
        }
        if (columnIndex2 > rawDataFiles.length) {
            setErrorMessage("Column 2 set too large.");
            setStatus(TaskStatus.ERROR);
            return null;
        }

        // Loop over the rows & filter
        for (processedRows = 0; !isCanceled()
                && processedRows < totalRows; processedRows++) {

            if (isCanceled())
                return null;

            allCriteriaMatched = true;

            double peak1Area = 1.0; // Default value in case of null peak
            double peak2Area = 1.0;
            double peak1MZ = -1.0;
            double peak2MZ = -1.0;
            double peak1RT = -1.0;
            double peak2RT = -1.0;
            double foldChange = 0.0;
            double ppmDiff = 0.0;
            double rtDiff = 0.0;
            final PeakListRow row = rows[processedRows];
            rawDataFile1 = rawDataFiles[columnIndex1];
            rawDataFile2 = rawDataFiles[columnIndex2];

            peak1 = row.getPeak(rawDataFile1);
            peak2 = row.getPeak(rawDataFile2);

            if (peak1 != null) {
                peak1Area = peak1.getArea();
                peak1MZ = peak1.getMZ();
                peak1RT = peak1.getRT();
            }

            if (peak2 != null) {
                peak2Area = peak2.getArea();
                peak2MZ = peak2.getMZ();
                peak2RT = peak2.getRT();
            }

            // Fold change criteria checking.
            if (evalutateFoldChange) {
                foldChange = Math.log(peak1Area / peak2Area) / Math.log(2);
                if (!foldChangeRange.contains(foldChange))
                    allCriteriaMatched = false;


                // PPM difference evaluation
                if (evalutatePPMdiff) {
                    ppmDiff = (peak1MZ - peak2MZ) / peak1MZ * 1E6;
                    if (!ppmDiffRange.contains(ppmDiff))
                        allCriteriaMatched = false;
                }

                // RT difference evaluation
                if (evalutateRTdiff) {
                    rtDiff = peak1RT - peak2RT;
                    if (!rtDiffRange.contains(rtDiff))
                        allCriteriaMatched = false;
                }

            }

            // Good row?
            if (allCriteriaMatched)
                newPeakList.addRow(copyPeakRow(row));

        }

        return newPeakList;
    }

    /**
     * Create a copy of a peak list row.
     *
     * @param row
     *            the row to copy.
     * @return the newly created copy.
     */
    private static PeakListRow copyPeakRow(final PeakListRow row) {

        // Copy the peak list row.
        final PeakListRow newRow = new SimplePeakListRow(row.getID());
        PeakUtils.copyPeakListRowProperties(row, newRow);

        // Copy the peaks.
        for (final Feature peak : row.getPeaks()) {

            final Feature newPeak = new SimpleFeature(peak);
            PeakUtils.copyPeakProperties(peak, newPeak);
            newRow.addPeak(peak.getDataFile(), newPeak);
        }

        return newRow;
    }

}
