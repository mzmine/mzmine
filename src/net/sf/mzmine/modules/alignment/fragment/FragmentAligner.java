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

package net.sf.mzmine.modules.alignment.fragment;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakListRowSorter.SortingDirection;
import net.sf.mzmine.util.PeakListRowSorter.SortingProperty;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * This aligner first divides m/z range to fragments and then aligns peaks for
 * each fragment independently.
 * 
 */
public class FragmentAligner implements BatchStep, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private FragmentAlignerParameters parameters;

    private Desktop desktop;

    private ArrayList<PeakList> fragmentResults;

    private ArrayList<Task> startedTasks;

    // This task and Task[] concatenate fragment results and signal
    // completion of the whole alignment method to caller
    private ConcatenateFragmentsTask concatenateFragmentsTask;
    // private Task[] concatenateFragmentsTask[];

    private final String helpID = "net/sf/mzmine/modules/alignment/fragment/help/FragmentAlignment.html";

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new FragmentAlignerParameters();

        desktop.addMenuItem(MZmineMenu.ALIGNMENT, toString(),
                "Parallel alignment of divided peak list m/z intervals",
                KeyEvent.VK_F, false, this, null);

    }

    public String toString() {
        return "Fragment aligner";
    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (FragmentAlignerParameters) parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) currentParameters, helpID);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] peakLists = desktop.getSelectedPeakLists();

        if (peakLists.length == 0) {
            desktop.displayErrorMessage("Please select peak lists for alignment");
            return;
        }

        // Setup parameters
        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, peakLists, parameters.clone());

    }

    public void taskStarted(Task task) {
    }

    public synchronized void taskFinished(Task task) {

        concatenateFragmentsTask.taskFinished(task);

        startedTasks.remove(task);

		/*
		 * if (task.getStatus() == Task.TaskStatus.FINISHED) {
		 * 
		 * // Check if a task has been canceled for (Task t : startedTasks) //
		 * If yes, then cancel all tasks if (t.getStatus() ==
		 * TaskStatus.CANCELED) { for (Task tt : startedTasks) { tt.cancel(); }
		 * break; }
		 * 
		 * // All fragments done? if (startedTasks.size() == 0) {
		 * 
		 * // Run a task to concatenate fragment results //
		 * concatenateFragmentsTask[].start();
		 * 
		 * }
		 * 
		 * }
		 * 
		 * if (task.getStatus() == Task.TaskStatus.CANCELED) {
		 * 
		 * logger.info("Fragment aliger canceled.");
		 * 
		 * // Cancel tasks for all other fragments for (Task t : startedTasks)
		 * t.cancel();
		 * 
		 * // Run task just to signal finishing the task group. //
		 * concatenateFragmentsTask[].start();
		 * 
		 * }
		 * 
		 * if (task.getStatus() == Task.TaskStatus.ERROR) {
		 * 
		 * // Cancel tasks for all other fragments for (Task t : startedTasks)
		 * t.cancel();
		 * 
		 * String msg = "Error while aligning peak lists: " +
		 * task.getErrorMessage(); logger.severe(msg);
		 * desktop.displayErrorMessage(msg);
		 * 
		 * // Run task just to signal finishing the task group. //
		 * concatenateFragmentsTask[].start();
		 * 
		 * }
		 */

    }

    protected void addFragmentResult(PeakList peakList) {
        fragmentResults.add(peakList);
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.Task[]Listener)
     */
    public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {

        this.parameters = (FragmentAlignerParameters) parameters;

        // check peak lists
        if ((peakLists == null) || (peakLists.length == 0)) {
            desktop.displayErrorMessage("Please select peak lists for alignment");
            return null;
        }

        // Collect m/z and rt values for all rows in all peak lists and sort
        // them
        int numberOfRows = 0;
        for (PeakList peakList : peakLists)
            numberOfRows += peakList.getNumberOfRows();

        double[] mzValues = new double[numberOfRows];
        double[] rtValues = new double[numberOfRows];
        int valueIndex = 0;
        for (PeakList peakList : peakLists)
            for (PeakListRow row : peakList.getRows()) {
                mzValues[valueIndex] = row.getAverageMZ();
                rtValues[valueIndex] = row.getAverageRT();
                valueIndex++;
            }
        Arrays.sort(mzValues);
        Arrays.sort(rtValues);

        // Find all possible boundaries between fragments
        double mzTolerance = (Double) this.parameters.getParameterValue(FragmentAlignerParameters.MZTolerance);

        ArrayList<Double> allMZFragmentLimits = new ArrayList<Double>();
        ArrayList<Integer> allMZFragmentPeaks = new ArrayList<Integer>();
        int prevIndex = 0;
        for (valueIndex = 0; valueIndex < (mzValues.length - 1); valueIndex++) {
            double mzDiff = mzValues[valueIndex + 1] - mzValues[valueIndex];
            if (mzDiff > mzTolerance) {
                allMZFragmentLimits.add(mzValues[valueIndex]);
                allMZFragmentPeaks.add((valueIndex - prevIndex));
                prevIndex = valueIndex;
            }
        }

        logger.finest("Found " + allMZFragmentLimits.size()
                + " possible m/z positions for fragment boundaries.");

        // Filter fragments if too many
        int maxNumberOfFragments = (Integer) this.parameters.getParameterValue(FragmentAlignerParameters.MaxFragments);

        ArrayList<Double> filteredMZFragmentLimits;

        if (maxNumberOfFragments < allMZFragmentLimits.size()) {

            filteredMZFragmentLimits = new ArrayList<Double>();

            int peaksPerFragment = (int) Math.ceil((double) mzValues.length
                    / (double) maxNumberOfFragments);

            int sumPeaks = 0;
            for (int mzFragmentIndex = 0; mzFragmentIndex < allMZFragmentLimits.size(); mzFragmentIndex++) {

                sumPeaks += allMZFragmentPeaks.get(mzFragmentIndex);
                if (sumPeaks >= peaksPerFragment) {
                    filteredMZFragmentLimits.add(allMZFragmentLimits.get(mzFragmentIndex));
                    sumPeaks = 0;
                }

            }

        } else {
            filteredMZFragmentLimits = allMZFragmentLimits;
        }

        logger.finest("Using " + (filteredMZFragmentLimits.size() + 1)
                + " fragments.");

        /*
         * for (double mz : filteredMZFragmentLimits) { System.out.print("" + mz + ",
         * "); } System.out.println();
         */

        Double[] mzFragmentLimits = filteredMZFragmentLimits.toArray(new Double[0]);

        // Initialize a temp peak list for each fragment and peak list
        PeakList[][] peakListsForFragments = new PeakList[allMZFragmentLimits.size() + 1][peakLists.length];
        for (int mzFragmentIndex = 0; mzFragmentIndex < (mzFragmentLimits.length + 1); mzFragmentIndex++) {
            for (int peakListIndex = 0; peakListIndex < peakLists.length; peakListIndex++) {
                PeakList peakList = peakLists[peakListIndex];
                peakListsForFragments[mzFragmentIndex][peakListIndex] = new SimplePeakList(
                        peakList.toString() + " fragment  "
                                + (mzFragmentIndex + 1) + " of "
                                + (mzFragmentLimits.length + 1),
                        peakList.getRawDataFiles());
            }
        }

        // Divide rows of each peak list to fragments
        for (int peakListIndex = 0; peakListIndex < peakLists.length; peakListIndex++) {
            PeakList peakList = peakLists[peakListIndex];

            PeakListRow[] peakListRows = peakList.getRows();
            Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.MZ,
                    SortingDirection.Ascending));

            int mzFragmentIndex = 0;
            for (PeakListRow peakListRow : peakListRows) {

                double mz = peakListRow.getAverageMZ();

                // Move to next fragment if possible and necessary
                while ((mzFragmentIndex < mzFragmentLimits.length)
                        && (mzFragmentLimits[mzFragmentIndex] < mz)) {
                    mzFragmentIndex++;
                }

                peakListsForFragments[mzFragmentIndex][peakListIndex].addRow(peakListRow);

            }

        }

        // Initialize concatenate task and task group
        fragmentResults = new ArrayList<PeakList>();
        concatenateFragmentsTask = new ConcatenateFragmentsTask(
                fragmentResults, this.parameters);
        return null;
		/*
		 * concatenateFragmentsTask[] = new
		 * Task[](concatenateFragmentsTask, null, Task[]Listener);
		 * 
		 * // Start a task for each fragment startedTasks = new
		 * ArrayList<Task>(); for (int mzFragmentIndex = 0; mzFragmentIndex <
		 * (mzFragmentLimits.length + 1); mzFragmentIndex++) { peakLists =
		 * peakListsForFragments[mzFragmentIndex]; String name; if
		 * (mzFragmentIndex < mzFragmentLimits.length) { name = "m/z up to " +
		 * mzFragmentLimits[mzFragmentIndex]; } else { if
		 * (mzFragmentLimits.length > 0) name = "m/z after " +
		 * mzFragmentLimits[mzFragmentLimits.length - 1]; else name =
		 * "single fragment"; }
		 * 
		 * Task t = new AlignFragmentTask(peakLists, this.parameters, name,
		 * this); startedTasks.add(t); MZmineCore.getTaskController().addTask(t,
		 * this); }
		 * 
		 * return concatenateFragmentsTask[];
		 */
    }

    public BatchStepCategory getBatchStepCategory() {
        return BatchStepCategory.ALIGNMENT;
    }

}
