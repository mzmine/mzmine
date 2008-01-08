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

package net.sf.mzmine.modules.alignment.gapfiller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStepAlignment;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

// TODO: Code for this method must be rewritten

public class SimpleGapFiller implements BatchStepAlignment, TaskListener,
        ActionListener {


    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ParameterSet parameters;

    private Desktop desktop;

    private PeakList sourcePeakList;

    private Hashtable<RawDataFile, EmptyGap[]> resultCollection;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new SimpleGapFillerParameters();

        desktop.addMenuItem(MZmineMenu.ALIGNMENT, "Simple gap filler", this,
                null, KeyEvent.VK_S, false, true);

    }

    public String toString() {
        return "Simple Gap filler";
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();
        if (selectedPeakLists.length != 1) {
            desktop.displayErrorMessage("Please select a single peak list for gap-filling");
            return;
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, selectedPeakLists, parameters.clone(), null);

    }

    public void taskStarted(Task task) {
        logger.info("Running simple gap filter");
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished gap-filling on "
                    + ((SimpleGapFillerTask) task).getDataFile());

            Object[] result = ((SimpleGapFillerTask) task).getResult();
            RawDataFile dataFile = (RawDataFile) result[0];
            EmptyGap[] emptyGaps = (EmptyGap[]) result[1];
            ParameterSet parameters = (ParameterSet) result[2];

            resultCollection.put(dataFile, emptyGaps);

            // If all results have been received, then create a new aligned peak
            // list
            if (resultCollection.size() == sourcePeakList.getNumberOfRawDataFiles()) {

                // TODO: Create a copy and fill gaps
                String newName = (String) ((SimpleParameterSet) parameters).getParameterValue(SimpleGapFillerParameters.peakListName);
                SimplePeakList processedPeakList = new SimplePeakList(newName);

                for (RawDataFile rawData : sourcePeakList.getRawDataFiles()) {
                    processedPeakList.addRawDataFile(rawData);
                }

                for (int peakListRowNumber = 0; peakListRowNumber < sourcePeakList.getNumberOfRows(); peakListRowNumber++) {

                    PeakListRow sourcePeakListRow = sourcePeakList.getRow(peakListRowNumber);

                    SimplePeakListRow newRow = new SimplePeakListRow(
                            sourcePeakListRow.getID());

                    for (RawDataFile rawDataFile : sourcePeakList.getRawDataFiles()) {

                        Peak sourceOriginalPeak = sourcePeakListRow.getOriginalPeakListEntry(rawDataFile);
                        Peak sourcePeak = sourcePeakListRow.getPeak(rawDataFile);

                        if (sourcePeak != null) {
                            newRow.addPeak(rawDataFile, sourceOriginalPeak,
                                    sourcePeak);
                        } else {
                            // No peak, get estimated peak from empty gap
                            emptyGaps = resultCollection.get(rawDataFile);
                            EmptyGap emptyGap = emptyGaps[peakListRowNumber];
                            Peak estimatedPeak = emptyGap.getEstimatedPeak();

                            newRow.addPeak(rawDataFile, estimatedPeak,
                                    estimatedPeak);
                        }

                    }

                    processedPeakList.addRow(newRow);

                }

                // Append aligned peak list to project
                MZmineProject currentProject = MZmineCore.getCurrentProject();
                currentProject.addPeakList(processedPeakList);

            }

        }
        
        if (task.getStatus() == Task.TaskStatus.ERROR) {

            String msg = "Error while gap filling peak list " + sourcePeakList + ": "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check parameter values for "
                        + toString(), (SimpleParameterSet) currentParameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters, TaskGroupListener methodListener) {

        // TODO why only 1?
        if (peakLists == null || peakLists.length != 1) {
            throw new IllegalArgumentException(
                    "Gap-filling requires exactly one aligned peak list");
        }

        // prepare a new sequence of tasks
        sourcePeakList = peakLists[0];
        RawDataFile[] rawDataFiles = sourcePeakList.getRawDataFiles();

        resultCollection = new Hashtable<RawDataFile, EmptyGap[]>();

        Task tasks[] = new SimpleGapFillerTask[rawDataFiles.length];
        for (int rawDataFileIndex = 0; rawDataFileIndex < rawDataFiles.length; rawDataFileIndex++) {

            RawDataFile rawDataFile = rawDataFiles[rawDataFileIndex];

            // Initialize empty gaps array (one element for each aligned peak
            // list row)
            EmptyGap[] emptyGaps = new EmptyGap[sourcePeakList.getNumberOfRows()];

            // Find empty gaps in the column of the current raw data file in the
            // aligned peak list
            for (int peakListRow = 0; peakListRow < sourcePeakList.getNumberOfRows(); peakListRow++) {
                Peak peak = sourcePeakList.getPeak(peakListRow, rawDataFile);
                if (peak == null) {
                    emptyGaps[peakListRow] = new EmptyGap(rawDataFile,
                            sourcePeakList.getRow(peakListRow).getAverageMZ(),
                            sourcePeakList.getRow(peakListRow).getAverageRT(),
                            (SimpleParameterSet) parameters);
                }
            }

            tasks[rawDataFileIndex] = new SimpleGapFillerTask(rawDataFile,
                    emptyGaps, (SimpleParameterSet) parameters);

        }

        // execute the sequence
        TaskGroup newSequence = new TaskGroup(tasks, this, methodListener);
        newSequence.start();

        return newSequence;

    }

}