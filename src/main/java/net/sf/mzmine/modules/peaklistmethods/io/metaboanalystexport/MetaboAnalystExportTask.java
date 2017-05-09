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

package net.sf.mzmine.modules.peaklistmethods.io.metaboanalystexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

class MetaboAnalystExportTask extends AbstractTask {

    private static final String fieldSeparator = ",";

    private final MZmineProject project;
    private final PeakList[] peakLists;
    private String plNamePattern = "{}";
    private int processedRows = 0, totalRows = 0;

    // parameter values
    private File fileName;
    private UserParameter<?, ?> groupParameter;

    MetaboAnalystExportTask(MZmineProject project, ParameterSet parameters) {

        this.project = project;
        this.peakLists = parameters
                .getParameter(MetaboAnalystExportParameters.peakLists)
                .getValue().getMatchingPeakLists();

        fileName = parameters
                .getParameter(MetaboAnalystExportParameters.filename)
                .getValue();
        groupParameter = parameters
                .getParameter(MetaboAnalystExportParameters.groupParameter)
                .getValue();

    }

    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0;
        }
        return (double) processedRows / (double) totalRows;
    }

    public String getTaskDescription() {
        return "Exporting peak list(s) " + Arrays.toString(peakLists)
                + " to MetaboAnalyst CSV file(s)";
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        // Total number of rows
        for (PeakList peakList : peakLists) {
            totalRows += peakList.getNumberOfRows();
        }

        // Process peak lists
        for (PeakList peakList : peakLists) {

            // Filename
            File curFile = fileName;
            if (substitute) {
                // Cleanup from illegal filename characters
                String cleanPlName = peakList.getName()
                        .replaceAll("[^a-zA-Z0-9.-]", "_");
                // Substitute
                String newFilename = fileName.getPath()
                        .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
                curFile = new File(newFilename);
            }

            // Check the peak list for MetaboAnalyst requirements
            boolean checkResult = checkPeakList(peakList);
            if (checkResult == false) {
                MZmineCore.getDesktop().displayErrorMessage(null, "Peak list " + peakList.getName()
                        + " does not conform to MetaboAnalyst requirement: at least 3 samples (raw data files) in each group");
            }

            try {

                // Open file
                FileWriter writer = new FileWriter(curFile);

                // Get number of rows
                totalRows = peakList.getNumberOfRows();

                exportPeakList(peakList, writer);

                // Close file
                writer.close();

            } catch (Exception e) {
                e.printStackTrace();
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not export peak list to file " + curFile
                        + ": " + e.getMessage());
                return;
            }
        }

        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);

    }

    private boolean checkPeakList(PeakList peakList) {

        // Check if each sample group has at least 3 samples
        final RawDataFile rawDataFiles[] = peakList.getRawDataFiles();
        for (RawDataFile file : rawDataFiles) {
            final String fileValue = String
                    .valueOf(project.getParameterValue(groupParameter, file));
            int count = 0;
            for (RawDataFile countFile : rawDataFiles) {
                final String countValue = String.valueOf(
                        project.getParameterValue(groupParameter, countFile));
                if (countValue.equals(fileValue))
                    count++;
            }
            if (count < 3)
                return false;
        }
        return true;
    }

    private void exportPeakList(PeakList peakList, FileWriter writer)
            throws IOException {

        final RawDataFile rawDataFiles[] = peakList.getRawDataFiles();

        // Buffer for writing
        StringBuffer line = new StringBuffer();

        // Write sample (raw data file) names
        line.append("\"Sample\"");
        for (RawDataFile file : rawDataFiles) {
            // Cancel?
            if (isCanceled()) {
                return;
            }


            line.append(fieldSeparator);
            final String value = file.getName().replace('"', '\'');
            line.append("\"");
            line.append(value);
            line.append("\"");
        }

        line.append("\n");

        // Write grouping parameter values
        line.append("\"");
        line.append(groupParameter.getName().replace('"', '\''));
        line.append("\"");

        for (RawDataFile file : rawDataFiles) {

            // Cancel?
            if (isCanceled()) {
                return;
            }

            line.append(fieldSeparator);
            String value = String
                    .valueOf(project.getParameterValue(groupParameter, file));
            value = value.replace('"', '\'');
            line.append("\"");
            line.append(value);
            line.append("\"");
        }

        line.append("\n");
        writer.write(line.toString());

        // Write data rows
        for (PeakListRow peakListRow : peakList.getRows()) {

            // Cancel?
            if (isCanceled()) {
                return;
            }

            // Reset the buffer
            line.setLength(0);

            final String rowName = generateUniquePeakListRowName(peakListRow);

            line.append("\"" + rowName + "\"");

            for (RawDataFile dataFile : rawDataFiles) {
                line.append(fieldSeparator);

                Feature peak = peakListRow.getPeak(dataFile);
                if (peak != null) {
                    final double area = peak.getArea();
                    line.append(String.valueOf(area));
                }
            }

            line.append("\n");
            writer.write(line.toString());

            processedRows++;
        }
    }

    /**
     * Generates a unique name for each peak list row
     */
    private String generateUniquePeakListRowName(PeakListRow row) {

        final double mz = row.getAverageMZ();
        final double rt = row.getAverageRT();
        final int rowId = row.getID();

        String generatedName = rowId + "/"
                + MZmineCore.getConfiguration().getMZFormat().format(mz) + "mz/"
                + MZmineCore.getConfiguration().getRTFormat().format(rt)
                + "min";
        PeakIdentity peakIdentity = row.getPreferredPeakIdentity();

        if (peakIdentity == null)
            return generatedName;

        String idName = peakIdentity
                .getPropertyValue(PeakIdentity.PROPERTY_NAME);

        if (idName == null)
            return generatedName;

        idName = idName.replace('"', '\'');
        generatedName = generatedName + " (" + idName + ")";

        return generatedName;

    }

}
