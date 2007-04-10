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

package net.sf.mzmine.methods.alignment.join;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleAlignmentResult;
import net.sf.mzmine.data.impl.SimpleAlignmentResultRow;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.data.impl.StandardCompoundFlag;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.IsotopePatternUtils;

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
        Vector<MasterIsotopeListRow> masterIsotopeListRows = new Vector<MasterIsotopeListRow>();
        Hashtable<OpenedRawDataFile, IsotopePatternUtils> isotopePatternUtils = new Hashtable<OpenedRawDataFile, IsotopePatternUtils>();

        /*
         * Loop through all data files
         */
        for (OpenedRawDataFile dataFile : dataFiles) {

            if (status == TaskStatus.CANCELED)
                return;

            /*
             * Pickup peak list for this file and generate list of isotope
             * patterns
             */
            PeakList peakList = (PeakList) dataFile.getCurrentFile().getLastData(
                    PeakList.class);
            IsotopePatternUtils isoUtil = new IsotopePatternUtils(peakList);
            isotopePatternUtils.put(dataFile, isoUtil);
            IsotopePattern[] isotopePatternList = isoUtil.getAllIsotopePatterns();
            IsotopePatternWrapper[] wrappedIsotopePatternList = new IsotopePatternWrapper[isotopePatternList.length];
            for (int i = 0; i < isotopePatternList.length; i++)
                wrappedIsotopePatternList[i] = new IsotopePatternWrapper(
                        isotopePatternList[i]);

            /*
             * Calculate scores between all pairs of isotope pattern and master
             * isotope list row
             */

            // Reset score tree
            TreeSet<PatternVsRowScore> scoreTree = new TreeSet<PatternVsRowScore>(
                    new ScoreSorter());

            for (IsotopePatternWrapper wrappedIsotopePattern : wrappedIsotopePatternList) {
                for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {
                    
                    if (status == TaskStatus.CANCELED)
                        return;
                    
                    PatternVsRowScore score = new PatternVsRowScore(
                            masterIsotopeListRow, wrappedIsotopePattern,
                            isoUtil, MZTolerance, RTToleranceUseAbs,
                            RTToleranceValueAbs, RTToleranceValuePercent,
                            MZvsRTBalance);
                    if (score.isGoodEnough())
                        scoreTree.add(score);
                }
            }

            /*
             * Browse scores in order of descending goodness-of-fit
             */

            Iterator<PatternVsRowScore> scoreIter = scoreTree.iterator();
            while (scoreIter.hasNext()) {
                PatternVsRowScore score = scoreIter.next();
                
                if (status == TaskStatus.CANCELED)
                    return;

                MasterIsotopeListRow masterIsotopeListRow = score.getMasterIsotopeListRow();
                IsotopePatternWrapper wrappedIsotopePattern = score.getWrappedIsotopePattern();

                // Check if master list row is already assigned with an isotope
                // pattern (from this rawDataID)
                if (masterIsotopeListRow.isAlreadyJoined())
                    continue;

                // Check if isotope pattern is already assigned to some master
                // isotope list row
                if (wrappedIsotopePattern.isAlreadyJoined())
                    continue;

                // Assign isotope pattern to master peak list row
                masterIsotopeListRow.addIsotopePattern(dataFile,
                        wrappedIsotopePattern.getIsotopePattern(), isoUtil);

                // Mark pattern and isotope pattern row as joined
                masterIsotopeListRow.setJoined(true);
                wrappedIsotopePattern.setAlreadyJoined(true);

                processedPercentage += 1.0f / (float) dataFiles.length
                        / (float) scoreTree.size();

            }

            /*
             * Remove 'joined' from all master isotope list rows
             */
            for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {
                masterIsotopeListRow.setJoined(false);
            }

            /*
             * Add remaining isotope patterns as new rows to master isotope list
             */
            for (IsotopePatternWrapper wrappedIsotopePattern : wrappedIsotopePatternList) {
                
                if (status == TaskStatus.CANCELED)
                    return;
                
                if (wrappedIsotopePattern.isAlreadyJoined())
                    continue;

                MasterIsotopeListRow masterIsotopeListRow = new MasterIsotopeListRow();
                masterIsotopeListRow.addIsotopePattern(dataFile,
                        wrappedIsotopePattern.getIsotopePattern(), isoUtil);
                masterIsotopeListRows.add(masterIsotopeListRow);
            }

        }

        /*
         * Convert master isotope list to alignment result (master peak list)
         */

        // Get number of peak rows
        int numberOfRows = 0;
        for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {
            numberOfRows += masterIsotopeListRow.getNumberOfPeaksOnRow();
        }

        alignmentResult = new SimpleAlignmentResult("Result from Join Aligner");

        // Add openedrawdatafiles to alignment result
        for (OpenedRawDataFile dataFile : dataFiles)
            alignmentResult.addOpenedRawDataFile(dataFile);

        // Loop through master isotope list rows
        for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {

            SimpleIsotopePattern masterIsotopePattern = new SimpleIsotopePattern(
                    masterIsotopeListRow.getChargeState());

            // Loop through peaks on this master isotope list row
            for (int peakRow = 0; peakRow < masterIsotopeListRow.getNumberOfPeaksOnRow(); peakRow++) {
                
                if (status == TaskStatus.CANCELED)
                    return;

                // Create alignment result row
                SimpleAlignmentResultRow alignmentRow = new SimpleAlignmentResultRow();

                // Tag row with isotope pattern
                // alignmentRow.setIsotopePattern(masterIsotopePattern);
                alignmentRow.addData(IsotopePattern.class, masterIsotopePattern);

                // Loop through raw data files
                for (OpenedRawDataFile dataFile : dataFiles) {

                    IsotopePattern isotopePattern = masterIsotopeListRow.getIsotopePattern(dataFile);
                    if (isotopePattern == null)
                        continue;
                    IsotopePatternUtils isoUtil = isotopePatternUtils.get(dataFile);

                    // Add peak to alignment row
                    Peak[] isotopePeaks = isoUtil.getPeaksInPattern(isotopePattern);
                    if (peakRow < isotopePeaks.length) {
                        alignmentRow.addPeak(dataFile, isotopePeaks[peakRow]);
                        if (isotopePeaks[peakRow].hasData(StandardCompoundFlag.class)) {
                            if (!alignmentRow.hasData(StandardCompoundFlag.class)) {
                                alignmentRow.addData(
                                        StandardCompoundFlag.class,
                                        new StandardCompoundFlag());
                            }
                        }
                    }
                }

                alignmentResult.addRow(alignmentRow);

            }

        }
        status = TaskStatus.FINISHED;

    }

}
