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

package net.sf.mzmine.modules.gapfilling.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.BatchStep;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

// TODO: Code for this method must be rewritten

public class SimpleGapFiller implements BatchStep, TaskListener,
        ActionListener {

    public static final String RTToleranceTypeAbsolute = "Absolute";
    public static final String RTToleranceTypeRelative = "Relative";
    public static final Object[] RTToleranceTypePossibleValues = {
            RTToleranceTypeAbsolute, RTToleranceTypeRelative };

    public static final Parameter IntTolerance = new SimpleParameter(
            ParameterType.DOUBLE,
            "Intensity tolerance",
            "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
            "%", new Double(0.20), new Double(0.0), null);

    public static final Parameter MZTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z tolerance",
            "Search range size in M/Z direction", "Da", new Double(0.050),
            new Double(0.0), null);

    public static final Parameter RTToleranceType = new SimpleParameter(
            ParameterType.STRING, "RT range type",
            "How to determine search range size in RT direction",
            RTToleranceTypeAbsolute, RTToleranceTypePossibleValues);

    public static final Parameter RTToleranceValueAbs = new SimpleParameter(
            ParameterType.DOUBLE, "Absolute RT tolerance",
            "Absolute search range size in RT direction", "seconds",
            new Double(15.0), new Double(0.0), null);

    public static final Parameter RTToleranceValuePercent = new SimpleParameter(
            ParameterType.DOUBLE, "Relative RT tolerance",
            "Relative search range size in RT direction", "%",
            new Double(0.15), new Double(0.0), null);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ParameterSet parameters;

    private TaskController taskController;
    private Desktop desktop;

    // Maps raw data files to an array of gaps which must be filled from the raw
    // data. Used when distributing tasks.
    private Hashtable<OpenedRawDataFile, Vector<EmptyGap>> gapsForRawData;

    // Maps an empty gap to a opened raw data file. Used when constructing a
    // peak from an empty gap and placing it on alignment row.
    private Hashtable<EmptyGap, OpenedRawDataFile> rawDataForGap;

    // Maps an alignment row to an array of all empty gaps on that row. Used
    // when constructing new alignment result
    private Hashtable<PeakList, Vector<EmptyGap>> gapsForRow;

    // Maps raw data file to results of processing task (array of empty gaps)
    private Hashtable<OpenedRawDataFile, EmptyGap[]> resultsForRawData;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        gapsForRawData = new Hashtable<OpenedRawDataFile, Vector<EmptyGap>>();
        rawDataForGap = new Hashtable<EmptyGap, OpenedRawDataFile>();
        gapsForRow = new Hashtable<PeakList, Vector<EmptyGap>>();
        resultsForRawData = new Hashtable<OpenedRawDataFile, EmptyGap[]>();

        parameters = new SimpleParameterSet(new Parameter[] { IntTolerance,
                MZTolerance, RTToleranceType, RTToleranceValueAbs,
                RTToleranceValuePercent });

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

        PeakList[] selectedPeakLists = desktop.getSelectedAlignedPeakLists();
        if (selectedPeakLists.length < 1) {
            desktop.displayErrorMessage("Please select alignment result");
            return;
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, selectedPeakLists, parameters, null);

    }

    public void taskStarted(Task task) {
        logger.info("Running simple gap filter");
    }

    public void taskFinished(Task task) {

        /*
         *  // Did the task fail? if (task.getStatus() == TaskStatus.ERROR) {
         * overallStatus = TaskStatus.ERROR; // Cancel all remaining tasks for
         * (Task t : startedTasks) if ((t.getStatus() != TaskStatus.FINISHED) ||
         * (t.getStatus() != TaskStatus.ERROR)) t.cancel();
         *  }
         * 
         * return; }
         *  // Pickup results Object[] results = (Object[]) task.getResult();
         * OpenedRawDataFile openedRawDataFile = (OpenedRawDataFile) results[0];
         * EmptyGap[] emptyGaps = (EmptyGap[]) results[1];
         * resultsForRawData.put(openedRawDataFile, emptyGaps);
         * 
         * completedTasks.add(task);
         *  // All results received already? if (completedTasks.size() ==
         * startedTasks.size()) {
         *  // Yes, then construct new alignment result & copy opened raw data //
         * files from original alignment result to the new one
         * processedPeakList = new SimplePeakList( "Result from
         * gap-filling"); for (OpenedRawDataFile loopOpenedRawDataFile :
         * originalPeakList.getRawDataFiles()) {
         * processedPeakList.addOpenedRawDataFile(loopOpenedRawDataFile); }
         *  // Add rows to the new alignment result for (PeakList
         * alignmentRow : originalPeakList.getRows()) {
         * SimplePeakList processedAlignmentRow = new
         * SimplePeakList(); //
         * processedAlignmentRow.setIsotopePattern(alignmentRow.getIsotopePattern());
         * processedAlignmentRow.addData(IsotopePattern.class,
         * alignmentRow.getLastData(IsotopePattern.class));
         *  // Copy old peaks to new row for (OpenedRawDataFile
         * loopOpenedRawDataFile : alignmentRow.getOpenedRawDataFiles()) { Peak
         * p = alignmentRow.getPeak(loopOpenedRawDataFile);
         * processedAlignmentRow.addPeak(loopOpenedRawDataFile, p); }
         *  // Construct new peaks from empty gaps and put them on same row
         * Vector<EmptyGap> filledGaps = gapsForRow.get(alignmentRow); for
         * (EmptyGap filledGap : filledGaps) { Peak p =
         * filledGap.getEstimatedPeak(); p.addData(IsotopePattern.class,
         * alignmentRow.getLastData(IsotopePattern.class)); OpenedRawDataFile
         * peakRawData = rawDataForGap.get(filledGap);
         * processedAlignmentRow.addPeak(peakRawData, p); }
         *  // Add row to the new alignment result
         * processedPeakList.addRow(processedAlignmentRow);
         *  }
         *  // TODO: Add method and parameters to history of an alignment result
         *  // Add new alignment result to the project
         * MZmineProject.getCurrentProject().addPeakList(
         * processedPeakList);
         * 
         * 
         *  }
         */

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
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.OpenedRawDataFile[],
     *      net.sf.mzmine.data.PeakList[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(OpenedRawDataFile[] dataFiles,
            PeakList[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        logger.info("Running " + toString() + " on " + alignmentResults.length
                + " alignment results.");

        /*
         * Loop rows of original alignment result For each row with some missing
         * peaks, generate "a working row" containing EmptyGap objects for each
         * missing peak
         * 
         * OpenedRawDataFile[] rawDataFiles =
         * originalPeakList.getRawDataFiles(); int i = 0; for
         * (PeakList alignmentRow : originalPeakList.getRows()) {
         * 
         * Vector<EmptyGap> gapsOfTheCurrentRow = gapsForRow.get(alignmentRow);
         * if (gapsOfTheCurrentRow == null) { gapsOfTheCurrentRow = new Vector<EmptyGap>();
         * gapsForRow.put(alignmentRow, gapsOfTheCurrentRow); }
         * 
         * double mz = alignmentRow.getAverageMZ(); double rt =
         * alignmentRow.getAverageRT(); for (OpenedRawDataFile openedRawDataFile :
         * rawDataFiles) { if (alignmentRow.getPeak(openedRawDataFile) == null) {
         * EmptyGap emptyGap = new EmptyGap(mz, rt, parameters);
         * 
         * Vector<EmptyGap> emptyGaps = gapsForRawData.get(openedRawDataFile);
         * if (emptyGaps == null) { emptyGaps = new Vector<EmptyGap>();
         * gapsForRawData.put(openedRawDataFile, emptyGaps); }
         * emptyGaps.add(emptyGap);
         * 
         * rawDataForGap.put(emptyGap, openedRawDataFile);
         * gapsOfTheCurrentRow.add(emptyGap);
         *  } } i++; }
         *  // Start a task for filling gaps in each raw data file
         * 
         * startedTasks = new Vector<Task>(); completedTasks = new Vector<Task>();
         * for (OpenedRawDataFile openedRawDataFile : rawDataFiles) {
         * 
         * Vector<EmptyGap> emptyGapsV = gapsForRawData.get(openedRawDataFile);
         * if (emptyGapsV == null) continue; if (emptyGapsV.size() == 0)
         * continue; EmptyGap[] emptyGaps = emptyGapsV.toArray(new EmptyGap[0]);
         * 
         * Task gapFillingTask = new SimpleGapFillerTask(openedRawDataFile,
         * emptyGaps, parameters); startedTasks.add(gapFillingTask);
         * taskController.addTask(gapFillingTask, this);
         *  }
         */

        return null;

    }

}