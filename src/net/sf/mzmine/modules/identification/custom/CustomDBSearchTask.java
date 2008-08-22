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

package net.sf.mzmine.modules.identification.custom;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleCompoundIdentity;
import net.sf.mzmine.taskcontrol.Task;

import com.Ostermiller.util.CSVParser;

/**
 * 
 */
class CustomDBSearchTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList peakList;
    private CustomDBSearchParameters parameters;

    private TaskStatus status;
    private String errorMessage;
    private String[][] databaseValues;
    private int finishedLines = 0;

    CustomDBSearchTask(PeakList peakList, CustomDBSearchParameters parameters) {
        status = TaskStatus.WAITING;
        this.peakList = peakList;
        this.parameters = parameters;
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
    public float getFinishedPercentage() {
        if (databaseValues == null)
            return 0;
        return ((float) finishedLines) / databaseValues.length;
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
                + parameters.getDataBaseFile();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;
        
        File dbFile = new File(parameters.getDataBaseFile());

        try {
            // read database contents in memory
            FileReader dbFileReader = new FileReader(dbFile);
            databaseValues = CSVParser.parse(dbFileReader);
            if (parameters.isIgnoreFirstLine())
                finishedLines++;
            for (; finishedLines < databaseValues.length; finishedLines++) {
                processOneLine(databaseValues[finishedLines]);
            }
            dbFileReader.close();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not read file " + dbFile, e);
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }
        
        status = TaskStatus.FINISHED;

    }

    private void processOneLine(String values[]) {

        Object fieldOrder[] = parameters.getFieldOrder();
        int numOfColumns = Math.min(fieldOrder.length, values.length);

        String lineID = null, lineName = null, lineFormula = null;
        double lineMZ = 0, lineRT = 0;

        for (int i = 0; i < numOfColumns; i++) {
            if (fieldOrder[i].equals(CustomDBSearchParameters.fieldID))
                lineID = values[i];
            if (fieldOrder[i].equals(CustomDBSearchParameters.fieldName))
                lineName = values[i];
            if (fieldOrder[i].equals(CustomDBSearchParameters.fieldFormula))
                lineFormula = values[i];
            if (fieldOrder[i].equals(CustomDBSearchParameters.fieldMZ))
                lineMZ = Double.parseDouble(values[i]);
            if (fieldOrder[i].equals(CustomDBSearchParameters.fieldRT))
                lineRT = Double.parseDouble(values[i]) * 60;
        }

        File dbFile = new File(parameters.getDataBaseFile());
        SimpleCompoundIdentity newIdentity = new SimpleCompoundIdentity(lineID,
                lineName, null, lineFormula, null, dbFile.getName(), null);

        for (PeakListRow peakRow : peakList.getRows()) {
            
            boolean mzOK = (Math.abs(peakRow.getAverageMZ() - lineMZ) < parameters.getMzTolerance());
            boolean rtOK = (Math.abs(peakRow.getAverageRT() - lineRT) < parameters.getRtTolerance());
            if (mzOK && rtOK) {
                
                logger.finest("Found compound " + lineName + " (m/z " + lineMZ + ", RT " + lineRT + ")");

                // add new identity to the row
                peakRow.addCompoundIdentity(newIdentity);

            }
        }

    }

}
