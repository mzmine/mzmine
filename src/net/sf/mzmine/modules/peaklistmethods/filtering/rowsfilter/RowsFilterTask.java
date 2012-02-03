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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.data.*;
import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

import java.util.logging.Level;
import java.util.logging.Logger;

import static net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter.RowsFilterParameters.*;

/**
 * Filters out peak list rows.
 */
public class RowsFilterTask extends AbstractTask {

        // Logger.
        private static final Logger LOG = Logger.getLogger(RowsFilterTask.class.getName());
        // Peak lists.
        private final PeakList origPeakList;
        private PeakList filteredPeakList;
        // Processed rows counter
        private int processedRows;
        private int totalRows;
        // Parameters.
        private final ParameterSet parameters;

        /**
         * Create the task.
         *
         * @param list         peak list to process.
         * @param parameterSet task parameters.
         */
        public RowsFilterTask(final PeakList list, final ParameterSet parameterSet) {

                // Initialize.
                parameters = parameterSet;
                origPeakList = list;
                filteredPeakList = null;
                processedRows = 0;
                totalRows = 0;
        }

        @Override
        public Object[] getCreatedObjects() {

                return new Object[]{filteredPeakList};
        }

        @Override
        public double getFinishedPercentage() {

                return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
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
                                        final MZmineProject currentProject = MZmineCore.getCurrentProject();
                                        currentProject.addPeakList(filteredPeakList);

                                        // Remove the original peaklist if requested
                                        if (parameters.getParameter(AUTO_REMOVE).getValue()) {

                                                currentProject.removePeakList(origPeakList);
                                        }

                                        setStatus(TaskStatus.FINISHED);
                                        LOG.info("Finished peak list rows filter");
                                }
                        } catch (Throwable t) {

                                errorMessage = t.getMessage();
                                setStatus(TaskStatus.ERROR);
                                LOG.log(Level.SEVERE, "Peak list row filter error", t);
                        }
                }
        }

        /**
         * Filter the peak list rows.
         *
         * @param peakList peak list to filter.
         * @return a new peak list with rows of the original peak list that pass the filtering.
         */
        private PeakList filterPeakListRows(final PeakList peakList) {

                // Create new peak list.
                final PeakList newPeakList = new SimplePeakList(
                        peakList.getName() + ' ' + parameters.getParameter(SUFFIX).getValue(), peakList.getRawDataFiles());

                // Copy previous applied methods.
                for (final PeakListAppliedMethod method : peakList.getAppliedMethods()) {

                        newPeakList.addDescriptionOfAppliedTask(method);
                }

                // Add task description to peakList.
                newPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(getTaskDescription(), parameters));

                // Get parameters.
                final boolean identified = parameters.getParameter(HAS_IDENTITIES).getValue();
                final String groupingParameter = (String) parameters.getParameter(GROUPSPARAMETER).getValue();
                final int minPresent = parameters.getParameter(MIN_PEAK_COUNT).getValue();
                final int minIsotopePatternSize = parameters.getParameter(MIN_ISOTOPE_PATTERN_COUNT).getValue();
                final Range mzRange = parameters.getParameter(MZ_RANGE).getValue();
                final Range rtRange = parameters.getParameter(RT_RANGE).getValue();
                final Range durationRange = parameters.getParameter(PEAK_DURATION).getValue();

                // Filter rows.
                final PeakListRow[] rows = peakList.getRows();
                totalRows = rows.length;
                for (processedRows = 0;
                        !isCanceled() && processedRows < totalRows;
                        processedRows++) {

                        final PeakListRow row = rows[processedRows];
                        boolean rowIsGood = true;

                        // Check number of peaks.
                        final int peakCount = getPeakCount(row, groupingParameter);
                        if (peakCount < minPresent) {

                                rowIsGood = false;
                        }

                        // Check identities.
                        if (identified && row.getPreferredPeakIdentity() == null) {

                                rowIsGood = false;
                        }

                        // Check average m/z.
                        if (!mzRange.contains(row.getAverageMZ())) {

                                rowIsGood = false;
                        }

                        // Check average RT.
                        if (!rtRange.contains(row.getAverageRT())) {

                                rowIsGood = false;
                        }

                        // Calculate average duration and isotope pattern count.
                        int maxIsotopePatternSizeOnRow = 1;
                        double avgDuration = 0.0;
                        final ChromatographicPeak[] peaks = row.getPeaks();
                        for (final ChromatographicPeak p : peaks) {

                                final IsotopePattern pattern = p.getIsotopePattern();
                                if (pattern != null && maxIsotopePatternSizeOnRow < pattern.getNumberOfIsotopes()) {

                                        maxIsotopePatternSizeOnRow = pattern.getNumberOfIsotopes();
                                }

                                avgDuration += p.getRawDataPointsRTRange().getSize();
                        }

                        // Check isotope pattern count.
                        if (maxIsotopePatternSizeOnRow < minIsotopePatternSize) {

                                rowIsGood = false;
                        }

                        // Check average duration.
                        avgDuration /= (double) peakCount;
                        if (!durationRange.contains(avgDuration)) {

                                rowIsGood = false;
                        }

                        // Good row?
                        if (rowIsGood) {

                                newPeakList.addRow(copyPeakRow(row));
                        }
                }

                return newPeakList;
        }

        /**
         * Create a copy of a peak list row.
         *
         * @param row the row to copy.
         * @return the newly created copy.
         */
        private static PeakListRow copyPeakRow(final PeakListRow row) {

                // Copy the peak list row.
                final PeakListRow newRow = new SimplePeakListRow(row.getID());
                PeakUtils.copyPeakListRowProperties(row, newRow);

                // Copy the peaks.
                for (final ChromatographicPeak peak : row.getPeaks()) {

                        final ChromatographicPeak newPeak = new SimpleChromatographicPeak(peak);
                        PeakUtils.copyPeakProperties(peak, newPeak);
                        newRow.addPeak(peak.getDataFile(), newPeak);
                }

                return newRow;
        }

        private int getPeakCount(PeakListRow row, String groupingParameter) {
                if (groupingParameter.contains("Filtering by ")) {
                        HashMap<String, Integer> groups = new HashMap<String, Integer>();
                        for (RawDataFile file : MZmineCore.getCurrentProject().getDataFiles()) {
                                UserParameter params[] = MZmineCore.getCurrentProject().getParameters();
                                for (UserParameter p : params) {
                                        groupingParameter = groupingParameter.replace("Filtering by ", "");
                                        if (groupingParameter.equals(p.getName())) {
                                                String parameterValue = String.valueOf(MZmineCore.getCurrentProject().getParameterValue(p, file));
                                                if (row.hasPeak(file)) {
                                                        if (groups.containsKey(parameterValue)) {
                                                                groups.put(parameterValue, groups.get(parameterValue) + 1);
                                                        } else {
                                                                groups.put(parameterValue, 1);
                                                        }
                                                } else {
                                                        groups.put(parameterValue, 0);
                                                }
                                        }
                                }
                        }

                        Set ref = groups.keySet();
                        Iterator it = ref.iterator();
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
