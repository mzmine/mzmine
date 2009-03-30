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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.custom;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.taskcontrol.Task;

import com.Ostermiller.util.CSVParser;

/**
 * 
 */
class CustomDBSearchTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList peakList;

    private TaskStatus status;
    private String errorMessage;
    private String[][] databaseValues;
    private int finishedLines = 0;

    private String dataBaseFile;
    private String fieldSeparator;
    private Object[] fieldOrder;
    private boolean ignoreFirstLine;
    private double mzTolerance;
    private double rtTolerance;
    private CustomDBSearchParameters parameters;

    CustomDBSearchTask(PeakList peakList, CustomDBSearchParameters parameters) {
        status = TaskStatus.WAITING;
        this.peakList = peakList;

        dataBaseFile = (String) parameters.getParameterValue(CustomDBSearchParameters.dataBaseFile);
        fieldSeparator = (String) parameters.getParameterValue(CustomDBSearchParameters.fieldSeparator);

        fieldOrder = (Object[]) parameters.getParameterValue(CustomDBSearchParameters.fieldOrder);

        ignoreFirstLine = (Boolean) parameters.getParameterValue(CustomDBSearchParameters.ignoreFirstLine);
        mzTolerance = (Double) parameters.getParameterValue(CustomDBSearchParameters.mzTolerance);
        rtTolerance = (Double) parameters.getParameterValue(CustomDBSearchParameters.rtTolerance);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
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
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
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

        status = TaskStatus.PROCESSING;

        File dbFile = new File(dataBaseFile);

        try {
            // read database contents in memory
            FileReader dbFileReader = new FileReader(dbFile);
            databaseValues = CSVParser.parse(dbFileReader,
                    fieldSeparator.charAt(0));
            if (ignoreFirstLine)
                finishedLines++;
            for (; finishedLines < databaseValues.length; finishedLines++) {
                try {
                    processOneLine(databaseValues[finishedLines]);
                } catch (Exception e) {
                    // ingore incorrect lines
                }
            }
            dbFileReader.close();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not read file " + dbFile, e);
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        // Add task description to peakList
        ((SimplePeakList) peakList).addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                "Peak identification using database " + dataBaseFile,
                parameters));
        
        // Notify the project manager that peaklist contents have changed
        MZmineCore.getProjectManager().fireProjectListeners(
                ProjectEvent.PEAKLIST_CONTENTS_CHANGED);

        status = TaskStatus.FINISHED;

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
                lineRT = Double.parseDouble(values[i]) * 60;
        }

        File dbFile = new File(dataBaseFile);
        SimplePeakIdentity newIdentity = new SimplePeakIdentity(lineID,
                lineName, null, lineFormula, null, dbFile.getName());

        for (PeakListRow peakRow : peakList.getRows()) {

            boolean mzOK = (Math.abs(peakRow.getAverageMZ() - lineMZ) < mzTolerance);
            boolean rtOK = (Math.abs(peakRow.getAverageRT() - lineRT) < rtTolerance);

            if (mzOK && rtOK) {

                logger.finest("Found compound " + lineName + " (m/z " + lineMZ
                        + ", RT " + lineRT + ")");

                // add new identity to the row
                peakRow.addPeakIdentity(newIdentity, false);

            }
        }

    }

}
