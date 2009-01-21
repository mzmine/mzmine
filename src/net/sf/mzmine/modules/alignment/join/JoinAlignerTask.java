/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.alignment.join;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

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
    private double mzTolerance, mzWeight;
    private boolean rtToleranceUseAbs;
    private double rtToleranceValueAbs, rtToleranceValuePercent;
    private double rtWeight;
    private boolean sameIDRequired, compareIsotopePattern;
    private double sameIDWeight, isotopePatternScoreThreshold,
            isotopeScoreWeight;
    private JoinAlignerParameters parameters;

    // ID counter for the new peaklist
    private int newRowID = 1;

    /**
     * @param rawDataFile
     * @param parameters
     */
    JoinAlignerTask(PeakList[] peakLists, JoinAlignerParameters parameters) {

        this.peakLists = peakLists;
        this.parameters = parameters;

        // Get parameter values for easier use
        peakListName = (String) parameters.getParameterValue(JoinAlignerParameters.peakListName);

        mzTolerance = (Double) parameters.getParameterValue(JoinAlignerParameters.MZTolerance);
        mzWeight = (Double) parameters.getParameterValue(JoinAlignerParameters.MZWeight);

        rtToleranceUseAbs = (parameters.getParameterValue(JoinAlignerParameters.RTToleranceType) == JoinAlignerParameters.RTToleranceTypeAbsolute);
        rtToleranceValueAbs = (Double) parameters.getParameterValue(JoinAlignerParameters.RTToleranceValueAbs);
        rtToleranceValuePercent = (Double) parameters.getParameterValue(JoinAlignerParameters.RTToleranceValuePercent);
        rtWeight = (Double) parameters.getParameterValue(JoinAlignerParameters.RTWeight);

        sameIDRequired = (Boolean) parameters.getParameterValue(JoinAlignerParameters.SameIDRequired);
        sameIDWeight = (Double) parameters.getParameterValue(JoinAlignerParameters.SameIDWeight);

        compareIsotopePattern = (Boolean) parameters.getParameterValue(JoinAlignerParameters.compareIsotopePattern);
        isotopePatternScoreThreshold = (Double) parameters.getParameterValue(JoinAlignerParameters.isotopePatternScoreThreshold);
        isotopeScoreWeight = (Double) parameters.getParameterValue(JoinAlignerParameters.isotopeScoreWeight);

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
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0f;
        return (double) processedRows / (double) totalRows;
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

        if ((mzWeight == 0) && (rtWeight == 0)) {
            if (((isotopeScoreWeight == 0) && (compareIsotopePattern))
                    || (!compareIsotopePattern)) {
                status = TaskStatus.ERROR;
                errorMessage = "Cannot run alignment, all the weight parameters are zero";
                return;
            }
        }

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

            // Create a sorted set of scores matching
            TreeSet<RowVsRowScore> scoreSet = new TreeSet<RowVsRowScore>();

            PeakListRow allRows[] = peakList.getRows();

            // Calculate scores for all possible alignments of this row
            for (PeakListRow row : allRows) {

                if (status == TaskStatus.CANCELED)
                    return;

                // Calculate limits for a row with which the row can be aligned
                double mzMin = row.getAverageMZ() - mzTolerance;
                double mzMax = row.getAverageMZ() + mzTolerance;
                double rtMin, rtMax;
                double rtToleranceValue = 0.0f;
                if (rtToleranceUseAbs) {
                    rtToleranceValue = rtToleranceValueAbs;
                    rtMin = row.getAverageRT() - rtToleranceValue;
                    rtMax = row.getAverageRT() + rtToleranceValue;
                } else {
                    rtToleranceValue = row.getAverageRT()
                            * rtToleranceValuePercent;
                    rtMin = row.getAverageRT() - rtToleranceValue;
                    rtMax = row.getAverageRT() + rtToleranceValue;
                }

                // Get all rows of the aligned peaklist within parameter limits
                PeakListRow candidateRows[] = alignedPeakList.getRowsInsideScanAndMZRange(
                        new Range(rtMin, rtMax), new Range(mzMin, mzMax));

                // Calculate scores and store them
                for (PeakListRow candidate : candidateRows) {

                    if (sameIDRequired) {
                        if (!PeakUtils.compareIdentities(row, candidate))
                            continue;
                    }

                    RowVsRowScore score;
                    try {

                        score = new RowVsRowScore(row, candidate, mzTolerance,
                                mzWeight, rtToleranceValue, rtWeight,
                                sameIDWeight, compareIsotopePattern,
                                isotopePatternScoreThreshold,
                                isotopeScoreWeight);

                        scoreSet.add(score);
                        errorMessage = score.getErrorMessage();

                    } catch (Exception e) {
                        e.printStackTrace();
                        status = TaskStatus.ERROR;
                        return;
                    }
                }

                processedRows++;

            }

            // Create a table of mappings for best scores
            Hashtable<PeakListRow, PeakListRow> alignmentMapping = new Hashtable<PeakListRow, PeakListRow>();

            // Iterate scores by descending order
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
                    targetRow.addPeak(file, row.getPeak(file));
                }

                // Add all non-existing identities from the original row to the
                // aligned row
                for (PeakIdentity identity : row.getCompoundIdentities()) {
                    if (!PeakUtils.containsIdentity(targetRow, identity))
                        targetRow.addCompoundIdentity(identity, false);
                }
                // TODO Handling of conflicting preferred identities
                targetRow.setPreferredCompoundIdentity(row.getPreferredCompoundIdentity());

                processedRows++;

            }

        } // Next peak list

        // Add new aligned peak list to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(alignedPeakList);
        
        // Add task description to peakList
        alignedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Join aligner", parameters));


        status = TaskStatus.FINISHED;

    }

}
