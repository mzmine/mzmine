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

package net.sf.mzmine.modules.normalization.rtnormalizer;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

class RTNormalizerTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList[] originalPeakLists;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // Processed rows counter
    private int processedRows, totalRows;

    private String suffix;
    private float mzTolerance, rtTolerance, minHeight;
    private boolean removeOriginal;

    public RTNormalizerTask(PeakList[] peakLists,
            RTNormalizerParameters parameters) {

        this.originalPeakLists = peakLists;

        suffix = (String) parameters.getParameterValue(RTNormalizerParameters.suffix);
        mzTolerance = (Float) parameters.getParameterValue(RTNormalizerParameters.MZTolerance);
        rtTolerance = (Float) parameters.getParameterValue(RTNormalizerParameters.RTTolerance);
        minHeight = (Float) parameters.getParameterValue(RTNormalizerParameters.minHeight);
        removeOriginal = (Boolean) parameters.getParameterValue(RTNormalizerParameters.autoRemove);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        if (totalRows == 0)
            return 0f;
        return (float) processedRows / (float) totalRows;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Retention time normalization of " + originalPeakLists.length
                + " peak lists";
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        // First we need to find standards by iterating through first peak list
        totalRows = originalPeakLists[0].getNumberOfRows();

        // Create new peak lists
        SimplePeakList normalizedPeakLists[] = new SimplePeakList[originalPeakLists.length];
        for (int i = 0; i < originalPeakLists.length; i++) {
            normalizedPeakLists[i] = new SimplePeakList(originalPeakLists[i]
                    + " " + suffix);

            // Remember how many rows we need to normalize
            totalRows += originalPeakLists[i].getNumberOfRows();

        }

        // goodStandards Vector contains identified standard rows, represented
        // by arrays. Each array has same length as originalPeakLists array.
        // Array items represent particular standard peak in each PeakList
        Vector<PeakListRow[]> goodStandards = new Vector<PeakListRow[]>();

        // Iterate the first peaklist
        standardIteration: for (PeakListRow candidate : originalPeakLists[0].getRows()) {

            // Cancel?
            if (status == TaskStatus.CANCELED) {
                return;
            }

            processedRows++;

            // Check that all peaks of this row have proper height
            for (Peak p : candidate.getPeaks()) {
                if (p.getHeight() < minHeight)
                    continue standardIteration;
            }

            PeakListRow goodStandardCandidate[] = new PeakListRow[originalPeakLists.length];
            goodStandardCandidate[0] = candidate;

            float candidateMZ = candidate.getAverageMZ();
            float candidateRT = candidate.getAverageRT();

            // Find matching rows in remaining peaklists
            for (int i = 1; i < originalPeakLists.length; i++) {
                PeakListRow matchingRows[] = originalPeakLists[i].getRowsInsideScanAndMZRange(
                        candidateRT - rtTolerance, candidateRT + rtTolerance,
                        candidateMZ - mzTolerance, candidateMZ + mzTolerance);

                // If we have not found exactly 1 matching peak, move to next
                // standard candidate
                if (matchingRows.length != 1)
                    continue standardIteration;

                // Check that all peaks of this row have proper height
                for (Peak p : matchingRows[0].getPeaks()) {
                    if (p.getHeight() < minHeight)
                        continue standardIteration;
                }

                // Save reference to matching peak in this peak list
                goodStandardCandidate[i] = matchingRows[0];

            }

            // If we found a match of same peak in all peaklists, mark it as a
            // good standard
            goodStandards.add(goodStandardCandidate);
            logger.finest("Found a good standard for RT normalization: "
                    + candidate);

        }

        // Check if we have any standards
        if (goodStandards.size() == 0) {
            status = TaskStatus.ERROR;
            errorMessage = "No good standard peak was found";
            return;
        }

        // Calculate average retention times of all standards
        float averagedRTs[] = new float[goodStandards.size()];
        for (int i = 0; i < goodStandards.size(); i++) {
            float rtAverage = 0;
            for (PeakListRow row : goodStandards.get(i))
                rtAverage += row.getAverageRT();
            rtAverage /= (float) originalPeakLists.length;
            averagedRTs[i] = rtAverage;
        }

        // Normalize each peak list
        for (int peakListIndex = 0; peakListIndex < originalPeakLists.length; peakListIndex++) {

            // Get standard rows for this peak list only
            PeakListRow standards[] = new PeakListRow[goodStandards.size()];
            for (int i = 0; i < goodStandards.size(); i++) {
                standards[i] = goodStandards.get(i)[peakListIndex];
            }

            normalizePeakList(originalPeakLists[peakListIndex],
                    normalizedPeakLists[peakListIndex], standards, averagedRTs);

        }

        // Add new peaklists to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();

        for (int i = 0; i < originalPeakLists.length; i++) {

            currentProject.addPeakList(normalizedPeakLists[i]);

            // Remove the original peaklists if requested
            if (removeOriginal)
                currentProject.removePeakList(originalPeakLists[i]);

        }

        status = TaskStatus.FINISHED;

    }

    /**
     * Normalize retention time of all rows in given peak list and save normalized rows into new peak list. 
     * @param originalPeakList Peak list to be normalized
     * @param normalizedPeakList New peak list, where normalized rows are to be saved
     * @param standards Standard rows in same peak list
     * @param normalizedStdRTs Normalized retention times of standard rows
     */
    private void normalizePeakList(PeakList originalPeakList,
            PeakList normalizedPeakList, PeakListRow standards[],
            float normalizedStdRTs[]) {

        PeakListRow originalRows[] = originalPeakList.getRows();

        // Iterate peak list rows
        for (PeakListRow originalRow : originalRows) {

            // Cancel?
            if (status == TaskStatus.CANCELED) {
                return;
            }

            // Normalize one row
            PeakListRow normalizedRow = normalizeRow(originalRow, standards,
                    normalizedStdRTs);
            
            // Add the new row to normalized peak list
            normalizedPeakList.addRow(normalizedRow);

            processedRows++;

        }

    }

    /**
     * Normalize retention time of given row using selected standards
     * @param originalRow Peak list row to be normalized
     * @param standards Standard rows in same peak list
     * @param normalizedStdRTs Normalized retention times of standard rows
     * @return New peak list row with normalized retention time
     */
    private PeakListRow normalizeRow(PeakListRow originalRow,
            PeakListRow standards[], float normalizedStdRTs[]) {

        PeakListRow normalizedRow = new SimplePeakListRow(originalRow.getID());

        // Standard rows preceding and following this row
        int prevStdIndex = -1, nextStdIndex = -1;

        for (int stdIndex = 0; stdIndex < standards.length; stdIndex++) {

            // If this standard peak is actually originalRow
            if (standards[stdIndex] == originalRow) {
                prevStdIndex = stdIndex;
                nextStdIndex = stdIndex;
                break;
            }

            // If this standard peak is before our originalRow
            if (standards[stdIndex].getAverageRT() < originalRow.getAverageRT()) {
                if ((prevStdIndex == -1)
                        || (standards[stdIndex].getAverageRT() > standards[prevStdIndex].getAverageRT()))
                    prevStdIndex = stdIndex;
            }

            // If this standard peak is after our originalRow
            if (standards[stdIndex].getAverageRT() > originalRow.getAverageRT()) {
                if ((nextStdIndex == -1)
                        || (standards[stdIndex].getAverageRT() < standards[nextStdIndex].getAverageRT()))
                    nextStdIndex = stdIndex;
            }

        }

        // Calculate normalized retention time of this row
        float normalizedRT = -1;

        if ((prevStdIndex == -1) || (nextStdIndex == -1)) {
            normalizedRT = originalRow.getAverageRT();
        } else

        if (prevStdIndex == nextStdIndex) {
            normalizedRT = normalizedStdRTs[prevStdIndex];
        } else {
            float weight = (originalRow.getAverageRT() - standards[prevStdIndex].getAverageRT())
                    / (standards[nextStdIndex].getAverageRT() - standards[prevStdIndex].getAverageRT());
            normalizedRT = normalizedStdRTs[prevStdIndex]
                    + (weight * (normalizedStdRTs[nextStdIndex] - normalizedStdRTs[prevStdIndex]));
        }

        // Set normalized retention time to all peaks in this row
        for (RawDataFile file : originalRow.getRawDataFiles()) {
            Peak dataFilePeak = originalRow.getPeak(file);
            if (dataFilePeak != null) {
                SimplePeak newPeak = new SimplePeak(dataFilePeak);
                newPeak.setRT(normalizedRT);
                normalizedRow.addPeak(file,
                        originalRow.getOriginalPeakListEntry(file), newPeak);
            }
        }

        return normalizedRow;

    }

}
