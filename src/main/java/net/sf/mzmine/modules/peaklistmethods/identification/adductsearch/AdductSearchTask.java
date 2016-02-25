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

package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import static net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductSearchParameters.ADDUCTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductSearchParameters.MAX_ADDUCT_HEIGHT;
import static net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductSearchParameters.MZ_TOLERANCE;
import static net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductSearchParameters.RT_TOLERANCE;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
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

public class AdductSearchTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(AdductSearchTask.class
	    .getName());

    private int finishedRows;
    private int totalRows;
    private final PeakList peakList;

    private final RTTolerance rtTolerance;
    private final MZTolerance mzTolerance;
    private final double maxAdductHeight;
    private final AdductType[] selectedAdducts;

    private final ParameterSet parameters;

    /**
     * Create the task.
     *
     * @param parameterSet
     *            the parameters.
     * @param list
     *            peak list.
     */
    public AdductSearchTask(final ParameterSet parameterSet, final PeakList list) {

	peakList = list;
	parameters = parameterSet;

	finishedRows = 0;
	totalRows = 0;

	rtTolerance = parameterSet.getParameter(RT_TOLERANCE).getValue();
	mzTolerance = parameterSet.getParameter(MZ_TOLERANCE).getValue();
	selectedAdducts = parameterSet.getParameter(ADDUCTS).getValue();
	maxAdductHeight = parameterSet.getParameter(MAX_ADDUCT_HEIGHT)
		.getValue();
    }

    @Override
    public double getFinishedPercentage() {

	return totalRows == 0 ? 0.0 : (double) finishedRows
		/ (double) totalRows;
    }

    @Override
    public String getTaskDescription() {

	return "Identification of adducts in " + peakList;
    }

    @Override
    public void run() {

	setStatus(TaskStatus.PROCESSING);
	LOG.info("Starting adducts search in " + peakList);

	try {

	    // Search the peak list for adducts.
	    searchAdducts();

	    if (!isCanceled()) {

		// Add task description to peakList.
		peakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Identification of adducts", parameters));

		// Repaint the window to reflect the change in the peak list
		Desktop desktop = MZmineCore.getDesktop();
		if (!(desktop instanceof HeadLessDesktop))
		    desktop.getMainWindow().repaint();

		// Done.
		setStatus(TaskStatus.FINISHED);
		LOG.info("Finished adducts search in " + peakList);
	    }
	} catch (Throwable t) {

	    LOG.log(Level.SEVERE, "Adduct search error", t);
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage(t.getMessage());
	}
    }

    /**
     * Search peak-list for adducts.
     */
    private void searchAdducts() {

	// Get rows.
	final PeakListRow[] rows = peakList.getRows();
	totalRows = rows.length;

	// Start with the highest peaks.
	Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Height,
		SortingDirection.Descending));

	// Compare each pair of rows against each other.
	for (int i = 0; !isCanceled() && i < totalRows; i++) {
	    for (int j = 0; !isCanceled() && j < totalRows; j++) {

		if (i == j)
		    continue;

		findAdducts(rows[i], rows[j]);
	    }

	    finishedRows++;
	}
    }

    /**
     * Check if candidate peak may be a possible adduct of a given main peak.
     *
     * @param mainRow
     *            main peak.
     * @param possibleAdduct
     *            candidate adduct peak.
     */
    private void findAdducts(final PeakListRow mainRow,
	    final PeakListRow possibleAdduct) {

	for (final AdductType adduct : selectedAdducts) {

	    if (checkAdduct(mainRow, possibleAdduct, adduct)) {

		// Add adduct identity and notify GUI.
		possibleAdduct.addPeakIdentity(new AdductIdentity(mainRow,
			adduct), false);
		MZmineCore.getProjectManager().getCurrentProject()
			.notifyObjectChanged(possibleAdduct, false);
	    }
	}
    }

    /**
     * Check if candidate peak is a given type of adduct of given main peak.
     *
     * @param mainPeak
     *            main peak.
     * @param possibleAdduct
     *            candidate adduct peak.
     * @param adduct
     *            adduct.
     * @return true if mass difference, retention time tolerance and adduct peak
     *         height conditions are met.
     */
    private boolean checkAdduct(final PeakListRow mainPeak,
	    final PeakListRow possibleAdduct, final AdductType adduct) {

	return
	// Check mass difference condition.
	mzTolerance.checkWithinTolerance(
		mainPeak.getAverageMZ() + adduct.getMassDifference(),
		possibleAdduct.getAverageMZ())

	// Check retention time condition.
		&& rtTolerance.checkWithinTolerance(mainPeak.getAverageRT(),
			possibleAdduct.getAverageRT())

		// Check height condition.
		&& possibleAdduct.getAverageHeight() <= mainPeak
			.getAverageHeight() * maxAdductHeight;
    }
}
