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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimplePeakInformation;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.PeakUtils;
import io.github.mzmine.util.RangeUtils;

/**
 * Filters out feature list rows.
 */
public class RowsFilterTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(RowsFilterTask.class.getName());
    // Feature lists.
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
     *            feature list to process.
     * @param parameterSet
     *            task parameters.
     */
    public RowsFilterTask(final MZmineProject project, final PeakList list,
            final ParameterSet parameterSet) {

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

        return "Filtering feature list rows";
    }

    @Override
    public void run() {

        if (!isCanceled()) {

            try {
                setStatus(TaskStatus.PROCESSING);
                LOG.info("Filtering feature list rows");

                // Filter the feature list.
                filteredPeakList = filterPeakListRows(origPeakList);

                if (!isCanceled()) {

                    // Add new peaklist to the project
                    project.addPeakList(filteredPeakList);

                    // Remove the original peaklist if requested
                    if (parameters
                            .getParameter(RowsFilterParameters.AUTO_REMOVE)
                            .getValue()) {

                        project.removePeakList(origPeakList);
                    }
                    setStatus(TaskStatus.FINISHED);
                    LOG.info("Finished feature list rows filter");
                }
            } catch (Throwable t) {

                setErrorMessage(t.getMessage());
                setStatus(TaskStatus.ERROR);
                LOG.log(Level.SEVERE, "Feature list row filter error", t);
            }
        }
    }

    /**
     * Filter the feature list rows.
     *
     * @param peakList
     *            feature list to filter.
     * @return a new feature list with rows of the original feature list that
     *         pass the filtering.
     */
    private PeakList filterPeakListRows(final PeakList peakList) {

        // Create new feature list.

        final PeakList newPeakList = new SimplePeakList(
                peakList.getName() + ' ' + parameters
                        .getParameter(RowsFilterParameters.SUFFIX).getValue(),
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
        final boolean onlyIdentified = parameters
                .getParameter(RowsFilterParameters.HAS_IDENTITIES).getValue();
        final boolean filterByIdentityText = parameters
                .getParameter(RowsFilterParameters.IDENTITY_TEXT).getValue();
        final boolean filterByCommentText = parameters
                .getParameter(RowsFilterParameters.COMMENT_TEXT).getValue();
        final String groupingParameter = (String) parameters
                .getParameter(RowsFilterParameters.GROUPSPARAMETER).getValue();
        final boolean filterByMinPeakCount = parameters
                .getParameter(RowsFilterParameters.MIN_PEAK_COUNT).getValue();
        final boolean filterByMinIsotopePatternSize = parameters
                .getParameter(RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT)
                .getValue();
        final boolean filterByMzRange = parameters
                .getParameter(RowsFilterParameters.MZ_RANGE).getValue();
        final boolean filterByRtRange = parameters
                .getParameter(RowsFilterParameters.RT_RANGE).getValue();
        final boolean filterByDuration = parameters
                .getParameter(RowsFilterParameters.PEAK_DURATION).getValue();
        final boolean filterByFWHM = parameters
                .getParameter(RowsFilterParameters.FWHM).getValue();
        final boolean filterByCharge = parameters
                .getParameter(RowsFilterParameters.CHARGE).getValue();
        final boolean filterByKMD = parameters
                .getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
                .getValue();
        final boolean filterByMS2 = parameters
                .getParameter(RowsFilterParameters.MS2_Filter).getValue();
        final String removeRowString = parameters
                .getParameter(RowsFilterParameters.REMOVE_ROW).getValue();
        Double minCount = parameters
                .getParameter(RowsFilterParameters.MIN_PEAK_COUNT)
                .getEmbeddedParameter().getValue();
        final boolean renumber = parameters
                .getParameter(RowsFilterParameters.Reset_ID).getValue();

        int rowsCount = 0;
        boolean removeRow = false;

        if (removeRowString.equals(RowsFilterParameters.removeRowChoices[0]))
            removeRow = false;
        else
            removeRow = true;

        // Keep rows that don't match any criteria. Keep by default.
        boolean filterRowCriteriaFailed = false;

        // Handle < 1 values for minPeakCount
        if ((minCount == null) || (minCount < 1))
            minCount = 1.0;
        // Round value down to nearest hole number
        int intMinCount = minCount.intValue();

        // Filter rows.
        final PeakListRow[] rows = peakList.getRows();
        totalRows = rows.length;
        for (processedRows = 0; !isCanceled()
                && processedRows < totalRows; processedRows++) {

            filterRowCriteriaFailed = false;

            final PeakListRow row = rows[processedRows];

            final int peakCount = getPeakCount(row, groupingParameter);

            // Check number of peaks.
            if (filterByMinPeakCount) {
                if (peakCount < intMinCount)
                    filterRowCriteriaFailed = true;
            }

            // Check identities.
            if (onlyIdentified) {

                if (row.getPreferredPeakIdentity() == null)
                    filterRowCriteriaFailed = true;
            }

            // Check average m/z.
            if (filterByMzRange) {
                final Range<Double> mzRange = parameters
                        .getParameter(RowsFilterParameters.MZ_RANGE)
                        .getEmbeddedParameter().getValue();
                if (!mzRange.contains(row.getAverageMZ()))
                    filterRowCriteriaFailed = true;

            }

            // Check average RT.
            if (filterByRtRange) {

                final Range<Double> rtRange = parameters
                        .getParameter(RowsFilterParameters.RT_RANGE)
                        .getEmbeddedParameter().getValue();

                if (!rtRange.contains(row.getAverageRT()))
                    filterRowCriteriaFailed = true;

            }

            // Search peak identity text.
            if (filterByIdentityText) {

                if (row.getPreferredPeakIdentity() == null)
                    filterRowCriteriaFailed = true;
                if (row.getPreferredPeakIdentity() != null) {
                    final String searchText = parameters
                            .getParameter(RowsFilterParameters.IDENTITY_TEXT)
                            .getEmbeddedParameter().getValue().toLowerCase()
                            .trim();
                    int numFailedIdentities = 0;
                    PeakIdentity[] identities = row.getPeakIdentities();
                    for (int index = 0; !isCanceled()
                            && index < identities.length; index++) {
                        String rowText = identities[index].getName()
                                .toLowerCase().trim();
                        if (!rowText.contains(searchText))
                            numFailedIdentities += 1;
                    }
                    if (numFailedIdentities == identities.length)
                        filterRowCriteriaFailed = true;

                }
            }

            // Search peak comment text.
            if (filterByCommentText) {

                if (row.getComment() == null)
                    filterRowCriteriaFailed = true;
                if (row.getComment() != null) {
                    final String searchText = parameters
                            .getParameter(RowsFilterParameters.COMMENT_TEXT)
                            .getEmbeddedParameter().getValue().toLowerCase()
                            .trim();
                    final String rowText = row.getComment().toLowerCase()
                            .trim();
                    if (!rowText.contains(searchText))
                        filterRowCriteriaFailed = true;

                }
            }

            // Calculate average duration and isotope pattern count.
            int maxIsotopePatternSizeOnRow = 1;
            double avgDuration = 0.0;
            final Feature[] peaks = row.getPeaks();
            for (final Feature p : peaks) {

                final IsotopePattern pattern = p.getIsotopePattern();
                if (pattern != null && maxIsotopePatternSizeOnRow < pattern
                        .getNumberOfDataPoints()) {

                    maxIsotopePatternSizeOnRow = pattern
                            .getNumberOfDataPoints();
                }

                avgDuration += RangeUtils
                        .rangeLength(p.getRawDataPointsRTRange());
            }

            // Check isotope pattern count.
            if (filterByMinIsotopePatternSize) {

                final int minIsotopePatternSize = parameters
                        .getParameter(
                                RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT)
                        .getEmbeddedParameter().getValue();
                if (maxIsotopePatternSizeOnRow < minIsotopePatternSize)
                    filterRowCriteriaFailed = true;
            }

            // Check average duration.
            avgDuration /= peakCount;
            if (filterByDuration) {

                final Range<Double> durationRange = parameters
                        .getParameter(RowsFilterParameters.PEAK_DURATION)
                        .getEmbeddedParameter().getValue();
                if (!durationRange.contains(avgDuration))
                    filterRowCriteriaFailed = true;

            }

            // Filter by FWHM range
            if (filterByFWHM) {

                final Range<Double> FWHMRange = parameters
                        .getParameter(RowsFilterParameters.FWHM)
                        .getEmbeddedParameter().getValue();
                // If any of the peaks fail the FWHM criteria,
                Double FWHM_value = row.getBestPeak().getFWHM();

                if (FWHM_value != null && !FWHMRange.contains(FWHM_value))
                    filterRowCriteriaFailed = true;
            }

            // Filter by charge range
            if (filterByCharge) {

                final Range<Integer> chargeRange = parameters
                        .getParameter(RowsFilterParameters.CHARGE)
                        .getEmbeddedParameter().getValue();
                int charge = row.getBestPeak().getCharge();
                if (charge == 0 || !chargeRange.contains(charge))
                    filterRowCriteriaFailed = true;
            }

            // Filter by KMD or RKM range
            if (filterByKMD) {

                // get embedded parameters
                final Range<Double> rangeKMD = parameters
                        .getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
                        .getEmbeddedParameters()
                        .getParameter(
                                KendrickMassDefectFilterParameters.kendrickMassDefectRange)
                        .getValue();
                final String kendrickMassBase = parameters
                        .getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
                        .getEmbeddedParameters()
                        .getParameter(
                                KendrickMassDefectFilterParameters.kendrickMassBase)
                        .getValue();
                final double shift = parameters
                        .getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
                        .getEmbeddedParameters()
                        .getParameter(KendrickMassDefectFilterParameters.shift)
                        .getValue();
                final int charge = parameters
                        .getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
                        .getEmbeddedParameters()
                        .getParameter(KendrickMassDefectFilterParameters.charge)
                        .getValue();
                final int divisor = parameters
                        .getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
                        .getEmbeddedParameters()
                        .getParameter(
                                KendrickMassDefectFilterParameters.divisor)
                        .getValue();
                final boolean useRemainderOfKendrickMass = parameters
                        .getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
                        .getEmbeddedParameters()
                        .getParameter(
                                KendrickMassDefectFilterParameters.useRemainderOfKendrickMass)
                        .getValue();

                // get m/z
                Double valueMZ = row.getBestPeak().getMZ();

                // calc exact mass of Kendrick mass base
                double exactMassFormula = FormulaUtils
                        .calculateExactMass(kendrickMassBase);

                // calc exact mass of Kendrick mass factor
                double kendrickMassFactor = Math
                        .round(exactMassFormula / divisor)
                        / (exactMassFormula / divisor);

                double defectOrRemainder = 0.0;

                if (!useRemainderOfKendrickMass) {

                    // calc Kendrick mass defect
                    defectOrRemainder = Math
                            .ceil(charge * (valueMZ * kendrickMassFactor))
                            - charge * (valueMZ * kendrickMassFactor);
                } else {

                    // calc Kendrick mass remainder
                    defectOrRemainder = (charge
                            * (divisor - Math.round(FormulaUtils
                                    .calculateExactMass(kendrickMassBase)))
                            * valueMZ)
                            / FormulaUtils.calculateExactMass(kendrickMassBase)//
                            - Math.floor((charge
                                    * (divisor - Math.round(
                                            FormulaUtils.calculateExactMass(
                                                    kendrickMassBase)))
                                    * valueMZ)
                                    / FormulaUtils.calculateExactMass(
                                            kendrickMassBase));
                }

                // shift Kendrick mass defect or remainder of Kendrick mass
                double kendrickMassDefectShifted = defectOrRemainder + shift
                        - Math.floor(defectOrRemainder + shift);

                // check if shifted Kendrick mass defect or remainder of
                // Kendrick mass is in range
                if (!rangeKMD.contains(kendrickMassDefectShifted))
                    filterRowCriteriaFailed = true;
            }

            // Check ms2 filter .
            if (filterByMS2) {
                // iterates the peaks
                int failCounts = 0;
                for (int i = 0; i < peakCount; i++) {
                    if (row.getPeaks()[i]
                            .getMostIntenseFragmentScanNumber() < 1) {
                        failCounts++;
                        // filterRowCriteriaFailed = true;
                        // break;
                    }
                }
                if (failCounts == peakCount) {
                    filterRowCriteriaFailed = true;
                }
            }

            if (!filterRowCriteriaFailed && !removeRow) {
                // Only add the row if none of the criteria have failed.
                rowsCount++;
                PeakListRow resetRow = copyPeakRow(row);
                if (renumber) {
                    resetRow.setID(rowsCount);
                }
                newPeakList.addRow(resetRow);
            }

            if (filterRowCriteriaFailed && removeRow) {
                // Only remove rows that match *all* of the criteria, so add
                // rows that fail any of the criteria.
                rowsCount++;
                PeakListRow resetRow = copyPeakRow(row);
                if (renumber) {
                    resetRow.setID(rowsCount);
                }
                newPeakList.addRow(resetRow);
            }

        }

        return newPeakList;
    }

    /**
     * Create a copy of a feature list row.
     *
     * @param row
     *            the row to copy.
     * @return the newly created copy.
     */
    private static PeakListRow copyPeakRow(final PeakListRow row) {

        // Copy the feature list row.
        final PeakListRow newRow = new SimplePeakListRow(row.getID());
        PeakUtils.copyPeakListRowProperties(row, newRow);

        // Copy the peaks.
        for (final Feature peak : row.getPeaks()) {

            final Feature newPeak = new SimpleFeature(peak);
            PeakUtils.copyPeakProperties(peak, newPeak);
            newRow.addPeak(peak.getDataFile(), newPeak);
        }

        // Add PeakInformation
        if (row.getPeakInformation() != null) {
            SimplePeakInformation information = new SimplePeakInformation(
                    new HashMap<>(row.getPeakInformation().getAllProperties()));
            newRow.setPeakInformation(information);
        }

        return newRow;
    }

    private int getPeakCount(PeakListRow row, String groupingParameter) {
        if (groupingParameter.contains("Filtering by ")) {
            HashMap<String, Integer> groups = new HashMap<String, Integer>();
            for (RawDataFile file : project.getDataFiles()) {
                UserParameter<?, ?> params[] = project.getParameters();
                for (UserParameter<?, ?> p : params) {
                    groupingParameter = groupingParameter
                            .replace("Filtering by ", "");
                    if (groupingParameter.equals(p.getName())) {
                        String parameterValue = String
                                .valueOf(project.getParameterValue(p, file));
                        if (row.hasPeak(file)) {
                            if (groups.containsKey(parameterValue)) {
                                groups.put(parameterValue,
                                        groups.get(parameterValue) + 1);
                            } else {
                                groups.put(parameterValue, 1);
                            }
                        } else {
                            groups.put(parameterValue, 0);
                        }
                    }
                }
            }

            Set<String> ref = groups.keySet();
            Iterator<String> it = ref.iterator();
            int min = Integer.MAX_VALUE;
            while (it.hasNext()) {
                String name = it.next();
                int val = groups.get(name);
                if (val < min) {
                    min = val;
                }
            }
            return min;

        } else {
            return row.getNumberOfPeaks();
        }
    }
}
