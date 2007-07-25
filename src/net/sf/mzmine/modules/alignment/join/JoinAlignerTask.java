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

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.PeakListRowSorterByMZ;

/**
 * 
 */
class JoinAlignerTask implements Task {

    private RawDataFile[] dataFiles;

    private TaskStatus status;
    private String errorMessage;

    private float processedPercentage;

    private SimplePeakList alignmentResult;

    private double MZTolerance;
    private double MZvsRTBalance;
    private boolean RTToleranceUseAbs;
    private double RTToleranceValueAbs;
    private double RTToleranceValuePercent;
    
    private String peakListName;
    private static int peakListNumber = 1;

    /**
     * @param rawDataFile
     * @param parameters
     */
    JoinAlignerTask(RawDataFile[] dataFiles, SimpleParameterSet parameters) {

        status = TaskStatus.WAITING;
        this.dataFiles = dataFiles;

        // Get parameter values for easier use
        MZTolerance = (Float) parameters.getParameterValue(JoinAligner.MZTolerance);
        MZvsRTBalance = (Float) parameters.getParameterValue(JoinAligner.MZvsRTBalance);

        RTToleranceUseAbs = (parameters.getParameterValue(JoinAligner.RTToleranceType) == JoinAligner.RTToleranceTypeAbsolute);
        RTToleranceValueAbs = (Float) parameters.getParameterValue(JoinAligner.RTToleranceValueAbs);
        RTToleranceValuePercent = (Float) parameters.getParameterValue(JoinAligner.RTToleranceValuePercent);
        peakListName = "#" + peakListNumber + " " + parameters.getParameterValue(JoinAligner.PeakListName);
        peakListNumber++;
        
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
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        /*
         * Initialize master isotope list and isotope pattern utility vector
         */
        alignmentResult = new SimplePeakList(peakListName);

        // Add openedrawdatafiles to alignment result
        for (RawDataFile dataFile : dataFiles)
            alignmentResult.addRawDataFile(dataFile);

        int newRowID = 1;
        
        /*
         * Loop through all data files
         */
        for (RawDataFile dataFile : dataFiles) {

            if (status == TaskStatus.CANCELED)
                return;
            
            MZmineProject currentProject = MZmineCore.getCurrentProject();

            /*
             * Pickup peak list for this file and generate list of wrapped peaks
             */
            PeakList peakList = currentProject.getFilePeakList(dataFile);
            PeakWrapper wrappedPeakList[] = new PeakWrapper[peakList.getNumberOfRows()];
            for (int i = 0; i < peakList.getNumberOfRows(); i++) {
                Peak peakToWrap = peakList.getPeak(i, dataFile);
                wrappedPeakList[i] = new PeakWrapper(peakToWrap);
            }
                      
            Arrays.sort(wrappedPeakList, new PeakWrapperComparator());

            /*
             * Calculate scores between all pairs of isotope pattern and master
             * isotope list row
             */

            // Reset score tree
            TreeSet<PeakVsRowScore> scoreTree = new TreeSet<PeakVsRowScore>(
                    new ScoreSorter());

        	PeakListRow[] rows = alignmentResult.getRows();
        	//Arrays.sort(rows, new PeakListRowComparator());
        	Arrays.sort(rows, new PeakListRowSorterByMZ());
            Integer nextStartingRowIndex = 0;
            for (PeakWrapper wrappedPeak : wrappedPeakList) {
            	
            	if (nextStartingRowIndex==null) nextStartingRowIndex=0;
            	int startingRowIndex = nextStartingRowIndex;
            	nextStartingRowIndex = null;

            	for (int alignmentResultRowIndex = startingRowIndex; alignmentResultRowIndex<rows.length; alignmentResultRowIndex++) {
            		
            		PeakListRow row = rows[alignmentResultRowIndex];

            		// Check m/z difference for optimization
            		double mzDiff = row.getAverageMZ() - wrappedPeak.getPeak().getMZ();
            		// If alignment reuslt row's m/z is too small then jump to next row
            		if ( (Math.signum(mzDiff)<0.0) &&
            				(Math.abs(mzDiff)>MZTolerance) )
            			continue;
            		
            		// If alignment result row's m/z is too big then break
            		if ( (Math.signum(mzDiff)>0.0) &&
            				(Math.abs(mzDiff)>MZTolerance) )
            			break;
            			
            		if (nextStartingRowIndex==null)
            			nextStartingRowIndex = alignmentResultRowIndex; 

                    if (status == TaskStatus.CANCELED)
                        return;

                    PeakVsRowScore score = new PeakVsRowScore(
                            (SimplePeakListRow) row, wrappedPeak,
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

                SimplePeakListRow row = score.getRow();
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

                SimplePeakListRow row = new SimplePeakListRow(newRowID);
                newRowID++;
                
                row.addPeak(dataFile, wrappedPeak.getPeak(), wrappedPeak.getPeak());
                alignmentResult.addRow(row);
                
            }

        }

        status = TaskStatus.FINISHED;

    }

}
