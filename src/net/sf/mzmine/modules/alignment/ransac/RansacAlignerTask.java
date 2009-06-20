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
package net.sf.mzmine.modules.alignment.ransac;

import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;

/**
 * 
 */
class RansacAlignerTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private PeakList peakLists[],  alignedPeakList;
    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // Processed rows counter
    private int processedRows,  totalRows;
    private String peakListName;
    private double mzTolerance;
    private double rtToleranceValueAbs;
    private RansacAlignerParameters parameters;

    /**
     * @param rawDataFile
     * @param parameters
     */
    RansacAlignerTask(PeakList[] peakLists, RansacAlignerParameters parameters) {

        this.peakLists = peakLists;
        this.parameters = parameters;

        // Get parameter values for easier use
        peakListName = (String) parameters.getParameterValue(RansacAlignerParameters.peakListName);

        mzTolerance = (Double) parameters.getParameterValue(RansacAlignerParameters.MZTolerance);

        rtToleranceValueAbs = (Double) parameters.getParameterValue(RansacAlignerParameters.RTToleranceValueAbs);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Ransac aligner, " + peakListName + " (" + peakLists.length + " peak lists)";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0f;
        }
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
        status = TaskStatus.PROCESSING;
        logger.info("Running RANSAC aligner");
        int rowID = 1;

        // Remember how many rows we need to process.
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
                    errorMessage = "Cannot run alignment, because file " + dataFile + " is present in multiple peak lists";
                    return;
                }

                allDataFiles.add(dataFile);
            }
        }

        // Create a new aligned peak list
        alignedPeakList = new SimplePeakList(peakListName,
                allDataFiles.toArray(new RawDataFile[0]));


        // For each peak list
        for (PeakList peakList : peakLists) {

            Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

            for (PeakListRow row : peakList.getRows()) {

                if (status == TaskStatus.CANCELED) {
                    return;
                }

                // Calculate limits for a row with which the row can be aligned
                double mzMin = row.getAverageMZ() - mzTolerance;
                double mzMax = row.getAverageMZ() + mzTolerance;
                double rtMin, rtMax;
                double rtToleranceValue = rtToleranceValueAbs;
                rtMin = row.getAverageRT() - rtToleranceValue;
                rtMax = row.getAverageRT() + rtToleranceValue;

                // Get all rows of the aligned peaklist within parameter limits
                PeakListRow candidateRows[] = alignedPeakList.getRowsInsideScanAndMZRange(
                        new Range(rtMin, rtMax), new Range(mzMin, mzMax));

                for (PeakListRow candidateRow : candidateRows) {
                    alignMol.addElement(new AlignStructMol(row, candidateRow));
                }
                processedRows++;
            }

            // The first peak list is added to the aligned peak list directly
            if (alignedPeakList.getNumberOfRows() == 0) {
                for (PeakListRow row : peakList.getRows()) {
                    PeakListRow newRow = new SimplePeakListRow(rowID++);
                    for (ChromatographicPeak p : row.getPeaks()) {
                        newRow.addPeak(p.getDataFile(), p);
                    }
                    alignedPeakList.addRow(newRow);
                    processedRows++;
                }
                alignedPeakList.setName(peakListName);
            } else {

                // RANSAC algorithm
                for (RawDataFile data : alignedPeakList.getRawDataFiles()) {
                    for (AlignStructMol mols : alignMol) {
                        mols.addResult(mols.Aligned);
                        mols.setRT(data);
                    }
                    
                    RANSAC ransac = new RANSAC(parameters, peakList.getName());
                    ransac.alignment(alignMol);
                }

                // Write the result
                for (PeakListRow row : peakList.getRows()) {
                    boolean mark = false;
                    for (PeakListRow alignedPeakListRow : alignedPeakList.getRows()) {
                        for (AlignStructMol mols : alignMol) {

                            // If the peak list row and the aligned peak list row are aligned
                            // add all the row peaks into the aligned peak list row and the loop stops
                            if (mols.isMols(row, alignedPeakListRow) && mols.getResult()) {
                                for (RawDataFile file : row.getRawDataFiles()) {
                                    alignedPeakListRow.addPeak(file, row.getPeak(file));
                                }
                                mark = true;
                                break;
                            }
                        }
                        if (mark) {
                            break;
                        }
                    }
                    if (!mark) {

                        PeakListRow row3 = new SimplePeakListRow(rowID++);
                        for (ChromatographicPeak p : row.getPeaks()) {
                            row3.addPeak(p.getDataFile(), p);
                        }
                        alignedPeakList.addRow(row3);
                    }
                    processedRows++;
                }

            }

        }

        // Add new aligned peak list to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();

        currentProject.addPeakList(alignedPeakList);

        // Add task description to peakList
        alignedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("RANSAC aligner", parameters));

        logger.info(
                "Finished RANSAC aligner");
        status = TaskStatus.FINISHED;

    }

    public Object[] getCreatedObjects() {
        return new Object[]{alignedPeakList};
    }
}
