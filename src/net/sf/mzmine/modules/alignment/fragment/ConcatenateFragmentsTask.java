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

import java.util.ArrayList;
import java.util.Iterator;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;

class ConcatenateFragmentsTask implements Task {

    private TaskStatus status;
    private String errorMessage;

    private ArrayList<PeakList> fragmentPeakLists;

    private String peakListName;

    ConcatenateFragmentsTask(ArrayList<PeakList> fragmentPeakLists,
            FragmentAlignerParameters parameters) {
        this.fragmentPeakLists = fragmentPeakLists;

        peakListName = (String) parameters.getParameterValue(FragmentAlignerParameters.peakListName);
    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinishedPercentage() {
        return 0.0f;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Fragment aligner: concatenate fragments";
    }

    public void run() {
        if (status == TaskStatus.ERROR)
            return;

        if (status == TaskStatus.CANCELED)
            return;

        status = TaskStatus.PROCESSING;

        // Initialize a new peak list
        PeakList fragmentPeakList = fragmentPeakLists.get(0);
        SimplePeakList alignedPeakList = new SimplePeakList(peakListName,
                fragmentPeakList.getRawDataFiles());

        // Copy rows from fragment peak lists to the final aligned peak list
        Iterator<PeakList> fragmentPeakListIterator = fragmentPeakLists.iterator();
        int newRowID = 1;
        while (fragmentPeakListIterator.hasNext()) {
            fragmentPeakList = fragmentPeakListIterator.next();
            for (PeakListRow fragmentRow : fragmentPeakList.getRows()) {

                // Create a new row
                PeakListRow targetRow = new SimplePeakListRow(newRowID);
                newRowID++;
                alignedPeakList.addRow(targetRow);

                // Add all peaks from the original row to the aligned row
                for (RawDataFile file : fragmentRow.getRawDataFiles()) {
                    targetRow.addPeak(file, fragmentRow.getPeak(file));
                }

                // Add all non-existing identities from the original row to the
                // aligned row
                for (PeakIdentity identity : fragmentRow.getPeakIdentities()) {
                    if (!PeakUtils.containsIdentity(targetRow, identity))
                        targetRow.addPeakIdentity(identity, false);
                }

                targetRow.setPreferredPeakIdentity(fragmentRow.getPreferredPeakIdentity());

            }

        }

        // Put result to project
        MZmineCore.getCurrentProject().addPeakList(alignedPeakList);
        
        // Add task description to peakList
        alignedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(getTaskDescription()));

        status = TaskStatus.FINISHED;

    }

    public void taskStarted(Task task) {
    }

    /**
     * Listens to results of aligning each fragment. If align fails for a
     * fragment, then also concatenating must fail
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == TaskStatus.ERROR)
            status = TaskStatus.ERROR;

        if (task.getStatus() == TaskStatus.CANCELED)
            if (status != TaskStatus.ERROR)
                status = TaskStatus.CANCELED;

    }

	public Object[] getCreatedObjects() {
		// TODO Auto-generated method stub
		return null;
	}

}
