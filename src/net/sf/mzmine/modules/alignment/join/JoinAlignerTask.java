/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.alignment.join;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class JoinAlignerTask implements Task {

    private PeakList[] peakLists;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // Processed rows counter
    private int processedRows, totalRows;

    private String peakListName;
    private float MZTolerance, MZvsRTBalance;
    private boolean RTToleranceUseAbs;
    private float RTToleranceValueAbs, RTToleranceValuePercent;

    // ID counter for the new peaklist
    private int newRowID = 1;

    /**
     * @param rawDataFile
     * @param parameters
     */
    JoinAlignerTask(PeakList[] peakLists, JoinAlignerParameters parameters) {

        this.peakLists = peakLists;

        // Get parameter values for easier use
        peakListName = (String) parameters.getParameterValue(JoinAlignerParameters.peakListName);
        MZTolerance = (Float) parameters.getParameterValue(JoinAlignerParameters.MZTolerance);
        MZvsRTBalance = (Float) parameters.getParameterValue(JoinAlignerParameters.MZvsRTBalance);
        RTToleranceUseAbs = (parameters.getParameterValue(JoinAlignerParameters.RTToleranceType) == JoinAlignerParameters.RTToleranceTypeAbsolute);
        RTToleranceValueAbs = (Float) parameters.getParameterValue(JoinAlignerParameters.RTToleranceValueAbs);
        RTToleranceValuePercent = (Float) parameters.getParameterValue(JoinAlignerParameters.RTToleranceValuePercent);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Join aligner, " + peakListName + " (" + peakLists.length
                + " peak lists)";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalRows == 0)
            return 0f;
        return (float) processedRows / (float) totalRows;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        // Remember how many rows we need to process. Each row will be processed
        // twice, first for score calculation, second for actual alignment.
        for (int i = 0; i < peakLists.length; i++) {
            totalRows += peakLists[i].getNumberOfRows() * 2;
        }

        // Collect all data files
        Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
        for (PeakList peakList : peakLists) {

            for (RawDataFile dataFile : peakList.getRawDataFiles()) {

                // Each data file can only have one column in aligned peak list
                if (allDataFiles.contains(dataFile)) {
                    status = TaskStatus.ERROR;
                    errorMessage = "Cannot run alignment, because file "
                            + dataFile + " is present in multiple peak lists";
                    return;
                }
                allDataFiles.add(dataFile);
            }
        }

        // Create a new aligned peak list
        SimplePeakList alignedPeakList = new SimplePeakList(peakListName,
                allDataFiles.toArray(new RawDataFile[0]));

        // Iterate source peak lists
        for (PeakList peakList : peakLists) {

            // Each data file can only have one column in aligned peak list
            for (RawDataFile dataFile : peakList.getRawDataFiles()) {
                if (alignedPeakList.hasRawDataFile(dataFile)) {
                    status = TaskStatus.ERROR;
                    errorMessage = "Cannot run alignment, because file "
                            + dataFile + " is present in multiple peak lists";
                    return;
                }
            }

            // Create a sorted set of scores matching
            TreeSet<RowVsRowScore> scoreSet = new TreeSet<RowVsRowScore>();

            PeakListRow allRows[] = peakList.getRows();

            // Calculate scores for all possible alignments of this row
            for (PeakListRow row : allRows) {

                if (status == TaskStatus.CANCELED)
                    return;

                // Calculate limits for a row with which the row can be aligned
                float mzMin = row.getAverageMZ() - MZTolerance;
                float mzMax = row.getAverageMZ() + MZTolerance;
                float rtMin, rtMax;
                if (RTToleranceUseAbs) {
                    rtMin = row.getAverageRT() - RTToleranceValueAbs;
                    rtMax = row.getAverageRT() + RTToleranceValueAbs;
                } else {
                    rtMin = row.getAverageRT()
                            - (row.getAverageRT() * RTToleranceValuePercent);
                    rtMax = row.getAverageRT()
                            + (row.getAverageRT() * RTToleranceValuePercent);
                }

                // Get all rows of the aligned peaklist within parameter limits
                PeakListRow candidateRows[] = alignedPeakList.getRowsInsideScanAndMZRange(
                        rtMin, rtMax, mzMin, mzMax);

                // Calculate scores and store them
                for (PeakListRow candidate : candidateRows) {
                    RowVsRowScore score = new RowVsRowScore(row, candidate,
                            MZvsRTBalance);
                    scoreSet.add(score);
                }

                processedRows++;

            }

            // Create a table of mappings for best scores
            Hashtable<PeakListRow, PeakListRow> alignmentMapping = new Hashtable<PeakListRow, PeakListRow>();

            // Iterate scores by ascending order
            Iterator<RowVsRowScore> scoreIterator = scoreSet.iterator();
            while (scoreIterator.hasNext()) {
                RowVsRowScore score = scoreIterator.next();

                // Check if the row is already mapped
                if (alignmentMapping.containsKey(score.getPeakListRow()))
                    continue;

                // Check if the aligned row is already filled
                if (alignmentMapping.containsValue(score.getAlignedRow()))
                    continue;

                alignmentMapping.put(score.getPeakListRow(),
                        score.getAlignedRow());

            }

            // Align all rows using mapping
            for (PeakListRow row : allRows) {

                PeakListRow targetRow = alignmentMapping.get(row);

                // If we have no mapping for this row, add a new one
                if (targetRow == null) {
                    targetRow = new SimplePeakListRow(newRowID);
                    newRowID++;
                    alignedPeakList.addRow(targetRow);
                }

                // Add all peaks from the original row to the aligned row
                for (RawDataFile file : row.getRawDataFiles()) {
                    targetRow.addPeak(file, row.getOriginalPeakListEntry(file),
                            row.getPeak(file));
                }

                processedRows++;

            }

        } // Next peak list

        // Add new aligned peak list to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(alignedPeakList);

        status = TaskStatus.FINISHED;

    }

}
