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
import net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter.RowsFilterParameters;
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
public class PeakComparisonRowFilterTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(PeakComparisonRowFilterTask.class
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
    public PeakComparisonRowFilterTask(final MZmineProject project, final PeakList list,
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
                            PeakComparisonRowFilterParameters.AUTO_REMOVE).getValue()) {
                        project.removePeakList(origPeakList);
                    }

                    setStatus(TaskStatus.FINISHED);
                    LOG.info("Finished peak comparison rows filter");
                }
            } catch (Throwable t) {

                setErrorMessage(t.getMessage());
                setStatus(TaskStatus.ERROR);
                LOG.log(Level.SEVERE, "Peak comparison row filter error", t);
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
                + parameters.getParameter(PeakComparisonRowFilterParameters.SUFFIX)
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
        final boolean foldChangeBool = parameters.getParameter(
                PeakComparisonRowFilterParameters.FOLD_CHANGE).getValue();
        final boolean columnIndex1 = parameters.getParameter(
                PeakComparisonRowFilterParameters.COLUMN_INDEX_1).getValue();
        final boolean columnIndex2 = parameters.getParameter(
                PeakComparisonRowFilterParameters.COLUMN_INDEX_2).getValue();
        final Range<Double> foldChangeRange = parameters.getParameter(
                PeakComparisonRowFilterParameters.FOLD_CHANGE)
                .getEmbeddedParameter().getValue();
        final int columnIndex1Value = parameters
                .getParameter(PeakComparisonRowFilterParameters.COLUMN_INDEX_1)
                .getEmbeddedParameter().getValue();
        final int columnIndex2Value = parameters
                .getParameter(PeakComparisonRowFilterParameters.COLUMN_INDEX_2)
                .getEmbeddedParameter().getValue();

        
        
        // Filter rows.
        final PeakListRow[] rows = peakList.getRows();
        RawDataFile rawDataFile1;
        RawDataFile rawDataFile2;
        int lengthRawDataFiles = 0;
        int lengthPeaks = 0;
        Feature peak1;
        Feature peak2;
        totalRows = rows.length;
        double foldChange = 0;
        for (processedRows = 0; !isCanceled() && processedRows < totalRows; processedRows++) {
            
            

            final PeakListRow row = rows[processedRows];
                
            final RawDataFile[] rawDataFiles = peakList.getRawDataFiles();
            lengthRawDataFiles = rawDataFiles.length;
            //rawDataFile1 = rawDataFiles[columnIndex1Value];
            //rawDataFile2 = rawDataFiles[columnIndex2Value];
            
            final Feature[] peaks = row.getPeaks();
            lengthPeaks = peaks.length;
            peak1 = peaks[columnIndex1Value];
            peak2 = peaks[columnIndex2Value];
            
            if ( peak1 != null && peak2 != null)
            {
            
                foldChange = Math.log(peak1.getArea()/peak2.getArea()) / Math.log(2);
            

            
                if ( foldChangeRange.contains(foldChange) )
                    newPeakList.addRow(copyPeakRow(row)); //Row matches criteria

            }
            
           


            
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
