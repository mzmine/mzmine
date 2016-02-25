/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.fragmentsearch;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import com.google.common.collect.Range;

public class FragmentSearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int finishedRows, totalRows;
    private PeakList peakList;

    private RTTolerance rtTolerance;
    private MZTolerance ms2mzTolerance;
    private double maxFragmentHeight, minMS2peakHeight;

    private ParameterSet parameters;

    /**
     * @param parameters
     * @param peakList
     */
    public FragmentSearchTask(ParameterSet parameters, PeakList peakList) {

	this.peakList = peakList;
	this.parameters = parameters;

	rtTolerance = parameters.getParameter(
		FragmentSearchParameters.rtTolerance).getValue();
	ms2mzTolerance = parameters.getParameter(
		FragmentSearchParameters.ms2mzTolerance).getValue();
	maxFragmentHeight = parameters.getParameter(
		FragmentSearchParameters.maxFragmentHeight).getValue();
	minMS2peakHeight = parameters.getParameter(
		FragmentSearchParameters.minMS2peakHeight).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (totalRows == 0)
	    return 0;
	return ((double) finishedRows) / totalRows;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Identification of fragments in " + peakList;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);

	logger.info("Starting fragments search in " + peakList);

	PeakListRow rows[] = peakList.getRows();
	totalRows = rows.length;

	// Start with the highest peaks
	Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Height,
		SortingDirection.Descending));

	// Compare each two rows against each other
	for (int i = 0; i < totalRows; i++) {

	    for (int j = i + 1; j < rows.length; j++) {

		// Task canceled?
		if (isCanceled())
		    return;

		// Treat the higher m/z peak as main peak and check if the
		// smaller one may be a fragment
		if (rows[i].getAverageMZ() > rows[j].getAverageMZ()) {
		    if (checkFragment(rows[i], rows[j]))
			addFragmentInfo(rows[i], rows[j]);
		} else {
		    if (checkFragment(rows[j], rows[i]))
			addFragmentInfo(rows[j], rows[i]);
		}

	    }

	    finishedRows++;

	}

	// Add task description to peakList
	((SimplePeakList) peakList)
		.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Identification of fragments", parameters));

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

	setStatus(TaskStatus.FINISHED);

	logger.info("Finished fragments search in " + peakList);

    }

    /**
     * Check if candidate peak may be a possible fragment of a given main peak
     * 
     * @param mainPeak
     * @param possibleFragment
     */
    private boolean checkFragment(PeakListRow mainPeak,
	    PeakListRow possibleFragment) {

	// Check retention time condition
	boolean rtCheck = rtTolerance.checkWithinTolerance(
		mainPeak.getAverageRT(), possibleFragment.getAverageRT());
	if (!rtCheck)
	    return false;

	// Check height condition
	if (possibleFragment.getAverageHeight() > mainPeak.getAverageHeight()
		* maxFragmentHeight)
	    return false;

	// Get MS/MS scan, if exists
	int fragmentScanNumber = mainPeak.getBestPeak()
		.getMostIntenseFragmentScanNumber();
	if (fragmentScanNumber <= 0)
	    return false;

	RawDataFile dataFile = mainPeak.getBestPeak().getDataFile();
	Scan fragmentScan = dataFile.getScan(fragmentScanNumber);
	if (fragmentScan == null)
	    return false;

	// Get MS/MS data points in the tolerance range
	Range<Double> ms2mzRange = ms2mzTolerance
		.getToleranceRange(possibleFragment.getAverageMZ());

	DataPoint fragmentDataPoints[] = fragmentScan
		.getDataPointsByMass(ms2mzRange);

	// If there is a MS/MS peak of required height, we have a hit
	for (DataPoint dp : fragmentDataPoints) {
	    if (dp.getIntensity() > minMS2peakHeight)
		return true;
	}

	return false;

    }

    /**
     * Add new identity to the fragment row
     * 
     * @param mainRow
     * @param fragmentRow
     */
    private void addFragmentInfo(PeakListRow mainRow, PeakListRow fragmentRow) {
	FragmentIdentity newIdentity = new FragmentIdentity(mainRow);
	fragmentRow.addPeakIdentity(newIdentity, false);

	// Notify the GUI about the change in the project
	MZmineCore.getProjectManager().getCurrentProject()
		.notifyObjectChanged(fragmentRow, false);

    }

}
