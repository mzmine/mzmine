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

package net.sf.mzmine.modules.io.peaklistexport;

import java.io.FileWriter;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;

class PeakListExportTask implements Task {

    private PeakList peakList;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private int processedRows, totalRows;

    // parameter values
    private String fileName, fieldSeparator;
    private boolean exportRowID, exportRowMZ, exportRowRT, exportRowComment,
            exportRowIdentity, exportRowFormula, exportRowNumDetected;
    private boolean exportPeakStatus, exportPeakMZ, exportPeakRT,
            exportPeakHeight, exportPeakArea;

    PeakListExportTask(PeakList peakList, PeakListExportParameters parameters) {

        this.peakList = peakList;

        fileName = (String) parameters.getParameterValue(PeakListExportParameters.filename);
        fieldSeparator = (String) parameters.getParameterValue(PeakListExportParameters.fieldSeparator);
        exportRowID = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportRowID);
        exportRowMZ = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportRowMZ);
        exportRowRT = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportRowRT);
        exportRowComment = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportRowComment);
        exportRowIdentity = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportRowIdentity);
        exportRowFormula = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportRowFormula);
        exportRowNumDetected = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportRowNumberOfDetected);
        exportPeakStatus = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportPeakStatus);
        exportPeakMZ = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportPeakMZ);
        exportPeakRT = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportPeakRT);
        exportPeakHeight = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportPeakHeight);
        exportPeakArea = (Boolean) parameters.getParameterValue(PeakListExportParameters.exportPeakArea);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0.0f;
        return (double) processedRows / (double) totalRows;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Exporting peak list " + peakList + " to " + fileName;
    }

    public void run() {

        // Open file
        FileWriter writer;
        try {
            writer = new FileWriter(fileName);
        } catch (Exception e) {
            status = TaskStatus.ERROR;
            errorMessage = "Could not open file " + fileName + " for writing.";
            return;
        }

        // Get number of rows
        totalRows = peakList.getNumberOfRows();

        // Buffer for writing
        StringBuffer line = new StringBuffer();

        // Write column headers
        if (exportRowID)
            line.append("ID" + fieldSeparator);
        if (exportRowMZ)
            line.append("Average m/z" + fieldSeparator);
        if (exportRowRT)
            line.append("Average retention time" + fieldSeparator);
        if (exportRowComment)
            line.append("Comment" + fieldSeparator);
        if (exportRowIdentity)
            line.append("Name" + fieldSeparator);
        if (exportRowFormula)
            line.append("Formula" + fieldSeparator);
        if (exportRowNumDetected)
            line.append("Number of detected peaks" + fieldSeparator);

        for (RawDataFile dataFile : peakList.getRawDataFiles()) {
            if (exportPeakStatus)
                line.append(dataFile.getFileName() + " status" + fieldSeparator);
            if (exportPeakMZ)
                line.append(dataFile.getFileName() + " m/z" + fieldSeparator);
            if (exportPeakRT)
                line.append(dataFile.getFileName() + " retention time"
                        + fieldSeparator);
            if (exportPeakHeight)
                line.append(dataFile.getFileName() + " height" + fieldSeparator);
            if (exportPeakArea)
                line.append(dataFile.getFileName() + " area" + fieldSeparator);
        }

        line.append("\n");

        try {
            writer.write(line.toString());
        } catch (Exception e) {
            status = TaskStatus.ERROR;
            errorMessage = "Could not write to file " + fileName;
            return;
        }

        // Write data rows

        for (PeakListRow peakListRow : peakList.getRows()) {

            // Cancel?
            if (status == TaskStatus.CANCELED)
                return;

            // Reset the buffer
            line.setLength(0);

            // Write row data
            if (exportRowID)
                line.append(peakListRow.getID() + fieldSeparator);
            if (exportRowMZ)
                line.append(peakListRow.getAverageMZ() + fieldSeparator);
            if (exportRowRT)
                line.append(peakListRow.getAverageRT() + fieldSeparator);
            if (exportRowComment) {
                if (peakListRow.getComment() == null)
                    line.append(fieldSeparator);
                else
                    line.append(peakListRow.getComment() + fieldSeparator);
            }
            if (exportRowIdentity) {
                if (peakListRow.getPreferredCompoundIdentity() == null)
                    line.append(fieldSeparator);
                else
                    line.append(peakListRow.getPreferredCompoundIdentity().getName()
                            + fieldSeparator);
            }
            if (exportRowFormula) {
                if (peakListRow.getPreferredCompoundIdentity() == null)
                    line.append(fieldSeparator);
                else
                    line.append(peakListRow.getPreferredCompoundIdentity().getCompoundFormula()
                            + fieldSeparator);
            }
            if (exportRowNumDetected) {
                int numDetected = 0;
                for (ChromatographicPeak p : peakListRow.getPeaks())
                    if (p.getPeakStatus() == PeakStatus.DETECTED)
                        numDetected++;

                line.append(numDetected + fieldSeparator);

            }

            for (RawDataFile dataFile : peakList.getRawDataFiles()) {
                ChromatographicPeak peak = peakListRow.getPeak(dataFile);
                if (peak != null) {
                    if (exportPeakStatus)
                        line.append(peak.getPeakStatus() + fieldSeparator);
                    if (exportPeakMZ)
                        line.append(peak.getMZ() + fieldSeparator);
                    if (exportPeakRT)
                        line.append(peak.getRT() + fieldSeparator);
                    if (exportPeakHeight)
                        line.append(peak.getHeight() + fieldSeparator);
                    if (exportPeakArea)
                        line.append(peak.getArea() + fieldSeparator);
                } else {
                    if (exportPeakStatus)
                        line.append("N/A" + fieldSeparator);
                    if (exportPeakMZ)
                        line.append("N/A" + fieldSeparator);
                    if (exportPeakRT)
                        line.append("N/A" + fieldSeparator);
                    if (exportPeakHeight)
                        line.append("N/A" + fieldSeparator);
                    if (exportPeakArea)
                        line.append("N/A" + fieldSeparator);
                }
            }

            line.append("\n");

            try {
                writer.write(line.toString());
            } catch (Exception e) {
                status = TaskStatus.ERROR;
                errorMessage = "Could not write to file " + fileName;
                return;
            }

            processedRows++;

        }

        // Close file
        try {
            writer.close();
        } catch (Exception e) {
            status = TaskStatus.ERROR;
            errorMessage = "Could not close file " + fileName;
            return;
        }

        status = TaskStatus.FINISHED;

    }

}