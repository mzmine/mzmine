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

package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
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
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.RangeUtils;

import com.google.common.collect.Range;

/**
 * Filters out peak list rows.
 */
public class RowsFilterTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(RowsFilterTask.class
            .getName());
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

        return totalRows == 0 ? 0.0 : (double) processedRows
                / (double) totalRows;
    }

    @Override
    public String getTaskDescription() {

        return "Filtering peak list rows";
    }

    @Override
    public void run() {

        if (!isCanceled()) {

            try {
                setStatus(TaskStatus.PROCESSING);
                LOG.info("Filtering peak list rows");

                // Filter the peak list.
                filteredPeakList = filterPeakListRows(origPeakList);

                if (!isCanceled()) {

                    // Add new peaklist to the project
                    project.addPeakList(filteredPeakList);

                    // Remove the original peaklist if requested
                    if (parameters.getParameter(
                            RowsFilterParameters.AUTO_REMOVE).getValue()) {
                        project.removePeakList(origPeakList);
                    }

                    setStatus(TaskStatus.FINISHED);
                    LOG.info("Finished peak list rows filter");
                }
            } catch (Throwable t) {

                setErrorMessage(t.getMessage());
                setStatus(TaskStatus.ERROR);
                LOG.log(Level.SEVERE, "Peak list row filter error", t);
            }
        }
    }

    /**
     * Filter the peak list rows.
     *
     * @param peakList
     *            peak list to filter.
     * @return a new peak list with rows of the original peak list that pass the
     *         filtering.
     */
    private PeakList filterPeakListRows(final PeakList peakList) {

        // Create new peak list.
        final PeakList newPeakList = new SimplePeakList(peakList.getName()
                + ' '
                + parameters.getParameter(RowsFilterParameters.SUFFIX)
                        .getValue(), peakList.getRawDataFiles());

        // Copy previous applied methods.
        for (final PeakListAppliedMethod method : peakList.getAppliedMethods()) {

            newPeakList.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peakList.
        newPeakList
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        getTaskDescription(), parameters));

        // Get parameters.
        final boolean onlyIdentified = parameters.getParameter(
                RowsFilterParameters.HAS_IDENTITIES).getValue();
        final boolean filterByIdentityText = parameters.getParameter(
                RowsFilterParameters.IDENTITY_TEXT).getValue();
        final boolean filterByCommentText = parameters.getParameter(
                RowsFilterParameters.COMMENT_TEXT).getValue();
        final String groupingParameter = (String) parameters.getParameter(
                RowsFilterParameters.GROUPSPARAMETER).getValue();
        final boolean filterByMinPeakCount = parameters.getParameter(
                RowsFilterParameters.MIN_PEAK_COUNT).getValue();
        final boolean filterByMinIsotopePatternSize = parameters.getParameter(
                RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT).getValue();
        final boolean filterByMzRange = parameters.getParameter(
                RowsFilterParameters.MZ_RANGE).getValue();
        final boolean filterByRtRange = parameters.getParameter(
                RowsFilterParameters.RT_RANGE).getValue();
        final boolean filterByDuration = parameters.getParameter(
                RowsFilterParameters.PEAK_DURATION).getValue();
        final String removeRowString = (String) parameters.getParameter(
                RowsFilterParameters.REMOVE_ROW).getValue();
        
        boolean removeRow = false;
        if ( removeRowString.equalsIgnoreCase("Keep rows that match all criteria") )
            removeRow = false;
        if ( removeRowString.equalsIgnoreCase("Remove rows that match all criteria") )
            removeRow = true;
        
        boolean filterRowCriteriaFailed = false; //Keep rows that don't match any criteria.  Keep by default.  
        
        // Filter rows.
        final PeakListRow[] rows = peakList.getRows();
        totalRows = rows.length;
        for (processedRows = 0; !isCanceled() && processedRows < totalRows; processedRows++) {
            
            filterRowCriteriaFailed = false;


            final PeakListRow row = rows[processedRows];

            final int peakCount = getPeakCount(row, groupingParameter);

            // Check number of peaks.
            if (filterByMinPeakCount) {

                final int minPeakCount = parameters
                        .getParameter(RowsFilterParameters.MIN_PEAK_COUNT)
                        .getEmbeddedParameter().getValue();
                if (peakCount < minPeakCount)
                    filterRowCriteriaFailed = true;
            }

            // Check identities.
            if (onlyIdentified)
            {
                
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
                if (row.getPreferredPeakIdentity() != null)
                {
                final String searchText = parameters
                        .getParameter(RowsFilterParameters.IDENTITY_TEXT)
                        .getEmbeddedParameter().getValue().toLowerCase().trim();
                final String rowText = row.getPreferredPeakIdentity().getName()
                        .toLowerCase().trim();
                if (!rowText.contains(searchText))
                    filterRowCriteriaFailed = true;
                
                }
            }

            // Search peak comment text.
            if (filterByCommentText) {

                if (row.getComment() == null)
                    filterRowCriteriaFailed = true;
                if (row.getComment() != null)
                {
                final String searchText = parameters
                        .getParameter(RowsFilterParameters.COMMENT_TEXT)
                        .getEmbeddedParameter().getValue().toLowerCase().trim();
                final String rowText = row.getComment().toLowerCase().trim();
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
                if (pattern != null
                        && maxIsotopePatternSizeOnRow < pattern
                                .getNumberOfDataPoints()) {

                    maxIsotopePatternSizeOnRow = pattern
                            .getNumberOfDataPoints();
                }

                avgDuration += RangeUtils.rangeLength(p
                        .getRawDataPointsRTRange());
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
            avgDuration /= (double) peakCount;
            if (filterByDuration) {

                final Range<Double> durationRange = parameters
                        .getParameter(RowsFilterParameters.PEAK_DURATION)
                        .getEmbeddedParameter().getValue();
                if (!durationRange.contains(avgDuration))
                    filterRowCriteriaFailed = true;
                
            }

            
            if (!filterRowCriteriaFailed && !removeRow)
                //Only add the row if none of the criteria have failed.
                newPeakList.addRow(copyPeakRow(row));
            if (filterRowCriteriaFailed && removeRow)
                //Only remove rows that match *all* of the criteria, so add rows that fail any of the criteria.
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

    private int getPeakCount(PeakListRow row, String groupingParameter) {
        if (groupingParameter.contains("Filtering by ")) {
            HashMap<String, Integer> groups = new HashMap<String, Integer>();
            for (RawDataFile file : project.getDataFiles()) {
                UserParameter<?, ?> params[] = project.getParameters();
                for (UserParameter<?, ?> p : params) {
                    groupingParameter = groupingParameter.replace(
                            "Filtering by ", "");
                    if (groupingParameter.equals(p.getName())) {
                        String parameterValue = String.valueOf(project
                                .getParameterValue(p, file));
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
                String name = (String) it.next();
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
