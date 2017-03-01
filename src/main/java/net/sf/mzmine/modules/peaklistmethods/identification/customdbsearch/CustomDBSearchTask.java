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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.customdbsearch;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;

class CustomDBSearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList peakList;

    private String[][] databaseValues;
    private int finishedLines = 0;

    private File dataBaseFile;
    private String fieldSeparator;
    private FieldItem[] fieldOrder;
    private boolean ignoreFirstLine;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private ParameterSet parameters;

    CustomDBSearchTask(PeakList peakList, ParameterSet parameters) {

	this.peakList = peakList;
	this.parameters = parameters;

	dataBaseFile = parameters.getParameter(
		CustomDBSearchParameters.dataBaseFile).getValue();
	fieldSeparator = parameters.getParameter(
		CustomDBSearchParameters.fieldSeparator).getValue();

	fieldOrder = parameters.getParameter(
		CustomDBSearchParameters.fieldOrder).getValue();

	ignoreFirstLine = parameters.getParameter(
		CustomDBSearchParameters.ignoreFirstLine).getValue();
	mzTolerance = parameters.getParameter(
		CustomDBSearchParameters.mzTolerance).getValue();
	rtTolerance = parameters.getParameter(
		CustomDBSearchParameters.rtTolerance).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (databaseValues == null)
	    return 0;
	return ((double) finishedLines) / databaseValues.length;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Peak identification of " + peakList + " using database "
		+ dataBaseFile;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);

	try {
	    // read database contents in memory
	    FileReader dbFileReader = new FileReader(dataBaseFile);
	    databaseValues = CSVParser.parse(dbFileReader,
		    fieldSeparator.charAt(0));
	    if (ignoreFirstLine)
		finishedLines++;
	    for (; finishedLines < databaseValues.length; finishedLines++) {
	        if (isCanceled()) {
	            dbFileReader.close();
	            return;
	        }
		try {
		    processOneLine(databaseValues[finishedLines]);
		} catch (Exception e) {
		    // ingore incorrect lines
		}
	    }
	    dbFileReader.close();

	} catch (Exception e) {
	    logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage(e.toString());
	    return;
	}

	// Add task description to peakList
	peakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
		"Peak identification using database " + dataBaseFile,
		parameters));

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

	setStatus(TaskStatus.FINISHED);

    }

    private void processOneLine(String values[]) {

	int numOfColumns = Math.min(fieldOrder.length, values.length);

	String lineID = null, lineName = null, lineFormula = null;
	double lineMZ = 0, lineRT = 0;

	for (int i = 0; i < numOfColumns; i++) {
	    if (fieldOrder[i] == FieldItem.FIELD_ID)
		lineID = values[i];
	    if (fieldOrder[i] == FieldItem.FIELD_NAME)
		lineName = values[i];
	    if (fieldOrder[i] == FieldItem.FIELD_FORMULA)
		lineFormula = values[i];
	    if (fieldOrder[i] == FieldItem.FIELD_MZ)
		lineMZ = Double.parseDouble(values[i]);
	    if (fieldOrder[i] == FieldItem.FIELD_RT)
		lineRT = Double.parseDouble(values[i]);
	}

	SimplePeakIdentity newIdentity = new SimplePeakIdentity(lineName,
		lineFormula, dataBaseFile.getName(), lineID, null);

	for (PeakListRow peakRow : peakList.getRows()) {

	    Range<Double> mzRange = mzTolerance.getToleranceRange(peakRow
		    .getAverageMZ());
	    Range<Double> rtRange = rtTolerance.getToleranceRange(peakRow
		    .getAverageRT());

	    boolean mzMatches = (lineMZ == 0d) || mzRange.contains(lineMZ);
	    boolean rtMatches = (lineRT == 0d) || rtRange.contains(lineRT);

	    if (mzMatches && rtMatches) {

		logger.finest("Found compound " + lineName + " (m/z " + lineMZ
			+ ", RT " + lineRT + ")");

		// add new identity to the row
		peakRow.addPeakIdentity(newIdentity, false);

		// Notify the GUI about the change in the project
		MZmineCore.getProjectManager().getCurrentProject()
			.notifyObjectChanged(peakRow, false);

	    }
	}

    }
}
