/*
 * Copyright 2006-2007 The MZmine Development Team
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

import java.util.Iterator;
import java.util.TreeSet;

import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleAlignmentResult;
import net.sf.mzmine.data.impl.SimpleAlignmentResultRow;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class JoinAlignerTask implements Task {

    private OpenedRawDataFile[] dataFiles;

    private TaskStatus status;
    private String errorMessage;

    private float processedPercentage;

    private SimpleAlignmentResult alignmentResult;

    private double MZTolerance;
    private double MZvsRTBalance;
    private boolean RTToleranceUseAbs;
    private double RTToleranceValueAbs;
    private double RTToleranceValuePercent;

    /**
     * @param rawDataFile
     * @param parameters
     */
    JoinAlignerTask(OpenedRawDataFile[] dataFiles, ParameterSet parameters) {

        status = TaskStatus.WAITING;
        this.dataFiles = dataFiles;

        // Get parameter values for easier use
        MZTolerance = (Double) parameters.getParameterValue(JoinAligner.MZTolerance);
        MZvsRTBalance = (Double) parameters.getParameterValue(JoinAligner.MZvsRTBalance);

        RTToleranceUseAbs = (parameters.getParameterValue(JoinAligner.RTToleranceType) == JoinAligner.RTToleranceTypeAbsolute);
        RTToleranceValueAbs = (Double) parameters.getParameterValue(JoinAligner.RTToleranceValueAbs);
        RTToleranceValuePercent = (Double) parameters.getParameterValue(JoinAligner.RTToleranceValuePercent);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Join aligner, " + dataFiles.length + " peak lists";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return processedPercentage;
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
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        return alignmentResult;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        /*
         * Initialize master isotope list and isotope pattern utility vector
         */
        alignmentResult = new SimpleAlignmentResult("Result from Join Aligner");

        // Add openedrawdatafiles to alignment result
        for (OpenedRawDataFile dataFile : dataFiles)
            alignmentResult.addOpenedRawDataFile(dataFile);

        /*
         * Loop through all data files
         */
        for (OpenedRawDataFile dataFile : dataFiles) {

            if (status == TaskStatus.CANCELED)
                return;

            /*
             * Pickup peak list for this file and generate list of wrapped peaks
             */
            PeakList peakList = dataFile.getPeakList();
            PeakWrapper wrappedPeakList[] = new PeakWrapper[peakList.getNumberOfPeaks()];
            for (int i = 0; i < peakList.getNumberOfPeaks(); i++) {
                Peak peakToWrap = peakList.getPeak(i);
                wrappedPeakList[i] = new PeakWrapper(peakToWrap);
            }

            /*
             * Calculate scores between all pairs of isotope pattern and master
             * isotope list row
             */

            // Reset score tree
            TreeSet<PeakVsRowScore> scoreTree = new TreeSet<PeakVsRowScore>(
                    new ScoreSorter());

            for (PeakWrapper wrappedPeak : wrappedPeakList) {
                for (AlignmentResultRow row : alignmentResult.getRows()) {

                    if (status == TaskStatus.CANCELED)
                        return;

                    PeakVsRowScore score = new PeakVsRowScore(
                            (SimpleAlignmentResultRow) row, wrappedPeak,
                            MZTolerance, RTToleranceUseAbs,
                            RTToleranceValueAbs, RTToleranceValuePercent,
                            MZvsRTBalance);

                    if (score.isGoodEnough())
                        scoreTree.add(score);
                }
            }
            /*
             * Browse scores in order of descending goodness-of-fit
             */
            Iterator<PeakVsRowScore> scoreIter = scoreTree.iterator();
            while (scoreIter.hasNext()) {
                PeakVsRowScore score = scoreIter.next();

                processedPercentage += 1.0f / (float) dataFiles.length
                        / (float) scoreTree.size();

                if (status == TaskStatus.CANCELED)
                    return;

                SimpleAlignmentResultRow row = score.getRow();
                PeakWrapper wrappedPeak = score.getPeakWrapper();

                // Check if master list row is already assigned with an isotope
                // pattern (from this rawDataID)
                if (row.getPeak(dataFile) != null)
                    continue;

                // Check if peak is already assigned to some alignment result row
                if (wrappedPeak.isAlreadyJoined())
                    continue;

                // Assign peak pattern to alignment result row
                row.addPeak(dataFile, wrappedPeak.getPeak(), wrappedPeak.getPeak());
                wrappedPeak.setAlreadyJoined(true);

            }

            /*
             * Add remaining peaks as new rows to alignment result
             */
            for (PeakWrapper wrappedPeak : wrappedPeakList) {

                if (status == TaskStatus.CANCELED)
                    return;

                if (wrappedPeak.isAlreadyJoined())
                    continue;

                SimpleAlignmentResultRow row = new SimpleAlignmentResultRow();
                row.addPeak(dataFile, wrappedPeak.getPeak(), wrappedPeak.getPeak());
                alignmentResult.addRow(row);
            }

        }

        status = TaskStatus.FINISHED;

    }

}
