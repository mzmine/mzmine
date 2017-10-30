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

package net.sf.mzmine.modules.peaklistmethods.io.csvexport;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RangeUtils;

class CSVExportTask extends AbstractTask {

    private PeakList[] peakLists;
    private int processedRows = 0, totalRows = 0;

    // parameter values
    private File fileName;
    private String plNamePattern = "{}";
    private String fieldSeparator;
    private ExportRowCommonElement[] commonElements;
    private ExportRowDataFileElement[] dataFileElements;
    private Boolean exportAllPeakInfo;
    private String idSeparator;

    CSVExportTask(ParameterSet parameters) {

        this.peakLists = parameters.getParameter(CSVExportParameters.peakLists)
                .getValue().getMatchingPeakLists();
        fileName = parameters.getParameter(CSVExportParameters.filename)
                .getValue();
        fieldSeparator = parameters
                .getParameter(CSVExportParameters.fieldSeparator).getValue();
        commonElements = parameters
                .getParameter(CSVExportParameters.exportCommonItems).getValue();
        dataFileElements = parameters
                .getParameter(CSVExportParameters.exportDataFileItems)
                .getValue();
        exportAllPeakInfo = parameters
                .getParameter(CSVExportParameters.exportAllPeakInfo).getValue();
        idSeparator = parameters.getParameter(CSVExportParameters.idSeparator)
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
                + " to CSV file(s)";
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

            // Open file
            FileWriter writer;
            try {
                writer = new FileWriter(curFile);
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage(
                        "Could not open file " + curFile + " for writing.");
                return;
            }

            exportPeakList(peakList, writer, curFile);

            // Cancel?
            if (isCanceled()) {
                return;
            }

            // Close file
            try {
                writer.close();
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not close file " + curFile);
                return;
            }

            // If peak list substitution pattern wasn't found,
            // treat one peak list only
            if (!substitute)
                break;
        }

        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);

    }

    private void exportPeakList(PeakList peakList, FileWriter writer,
            File fileName) {

        RawDataFile rawDataFiles[] = peakList.getRawDataFiles();

        // Buffer for writing
        StringBuffer line = new StringBuffer();

        // Write column headers

        // Common elements
        int length = commonElements.length;
        String name;
        for (int i = 0; i < length; i++) {
            name = commonElements[i].toString();
            name = name.replace("Export ", "");
            name = escapeStringForCSV(name);
            line.append(name + fieldSeparator);
        }

        // peak Information
        Set<String> peakInformationFields = new HashSet<>();

        for (PeakListRow row : peakList.getRows()) {
            if (row.getPeakInformation() != null) {
                for (String key : row.getPeakInformation().getAllProperties()
                        .keySet()) {
                    peakInformationFields.add(key);
                }
            }
        }

        if (exportAllPeakInfo)
            for (String field : peakInformationFields)
                line.append(field + fieldSeparator);

        // Data file elements
        length = dataFileElements.length;
        for (int df = 0; df < peakList.getNumberOfRawDataFiles(); df++) {
            for (int i = 0; i < length; i++) {
                name = rawDataFiles[df].getName();
                name = name + " " + dataFileElements[i].toString();
                name = escapeStringForCSV(name);
                line.append(name + fieldSeparator);
            }
        }

        line.append("\n");

        try {
            writer.write(line.toString());
        } catch (Exception e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Could not write to file " + fileName);
            return;
        }

        // Write data rows
        for (PeakListRow peakListRow : peakList.getRows()) {

            // Cancel?
            if (isCanceled()) {
                return;
            }

            // Reset the buffer
            line.setLength(0);

            // Common elements
            length = commonElements.length;
            for (int i = 0; i < length; i++) {
                switch (commonElements[i]) {
                case ROW_ID:
                    line.append(peakListRow.getID() + fieldSeparator);
                    break;
                case ROW_MZ:
                    line.append(peakListRow.getAverageMZ() + fieldSeparator);
                    break;
                case ROW_RT:
                    line.append(peakListRow.getAverageRT() + fieldSeparator);
                    break;
                case ROW_IDENTITY:
                    // Identity elements
                    PeakIdentity peakId = peakListRow.getPreferredPeakIdentity();
                    if (peakId == null) {
                      line.append(fieldSeparator);
                      break;
                    }
                    String propertyValue = peakId.toString();
                    propertyValue = escapeStringForCSV(propertyValue);
                    line.append(propertyValue + fieldSeparator);
                    break;
                case ROW_IDENTITY_ALL:
                  // Identity elements
                  PeakIdentity[] peakIdentities = peakListRow
                          .getPeakIdentities();
                  propertyValue = "";
                  for (int x = 0; x < peakIdentities.length; x++) {
                      if (x > 0)
                          propertyValue += idSeparator;
                      propertyValue += peakIdentities[x].toString();
                  }
                  propertyValue = escapeStringForCSV(propertyValue);
                  line.append(propertyValue + fieldSeparator);
                  break;
                case ROW_IDENTITY_DETAILS:
                  peakId = peakListRow.getPreferredPeakIdentity();
                  if (peakId == null) {
                    line.append(fieldSeparator);
                    break;
                  }
                  propertyValue = peakId.getDescription();
                  if (propertyValue != null) 
                    propertyValue = propertyValue.replaceAll("\\n", ";");
                  propertyValue = escapeStringForCSV(propertyValue);
                  line.append(propertyValue + fieldSeparator);
                  break;
                case ROW_COMMENT:
                    String comment = escapeStringForCSV(
                            peakListRow.getComment());
                    line.append(comment + fieldSeparator);
                    break;
                case ROW_PEAK_NUMBER:
                    int numDetected = 0;
                    for (Feature p : peakListRow.getPeaks()) {
                        if (p.getFeatureStatus() == FeatureStatus.DETECTED) {
                            numDetected++;
                        }
                    }
                    line.append(numDetected + fieldSeparator);
                    break;
                }
            }

            // peak Information
            if (exportAllPeakInfo) {
                if (peakListRow.getPeakInformation() != null) {
                    Map<String, String> allPropertiesMap = peakListRow
                            .getPeakInformation().getAllProperties();

                    for (String key : peakInformationFields) {
                        String value = allPropertiesMap.get(key);
                        if (value == null)
                            value = "";
                        line.append(value + fieldSeparator);
                    }
                }
            }

            // Data file elements
            length = dataFileElements.length;
            for (RawDataFile dataFile : rawDataFiles) {
                for (int i = 0; i < length; i++) {
                    Feature peak = peakListRow.getPeak(dataFile);
                    if (peak != null) {
                        switch (dataFileElements[i]) {
                        case PEAK_STATUS:
                            line.append(
                                    peak.getFeatureStatus() + fieldSeparator);
                            break;
                        case PEAK_MZ:
                            line.append(peak.getMZ() + fieldSeparator);
                            break;
                        case PEAK_RT:
                            line.append(peak.getRT() + fieldSeparator);
                            break;
                        case PEAK_RT_START:
                            line.append(peak.getRawDataPointsRTRange()
                                    .lowerEndpoint() + fieldSeparator);
                            break;
                        case PEAK_RT_END:
                            line.append(peak.getRawDataPointsRTRange()
                                    .upperEndpoint() + fieldSeparator);
                            break;
                        case PEAK_DURATION:
                            line.append(RangeUtils
                                    .rangeLength(peak.getRawDataPointsRTRange())
                                    + fieldSeparator);
                            break;
                        case PEAK_HEIGHT:
                            line.append(peak.getHeight() + fieldSeparator);
                            break;
                        case PEAK_AREA:
                            line.append(peak.getArea() + fieldSeparator);
                            break;
                        case PEAK_CHARGE:
                            line.append(peak.getCharge() + fieldSeparator);
                            break;
                        case PEAK_DATAPOINTS:
                            line.append(peak.getScanNumbers().length
                                    + fieldSeparator);
                            break;
                        case PEAK_FWHM:
                            line.append(peak.getFWHM() + fieldSeparator);
                            break;
                        case PEAK_TAILINGFACTOR:
                            line.append(
                                    peak.getTailingFactor() + fieldSeparator);
                            break;
                        case PEAK_ASYMMETRYFACTOR:
                            line.append(
                                    peak.getAsymmetryFactor() + fieldSeparator);
                            break;
                        case PEAK_MZMIN:
                            line.append(peak.getRawDataPointsMZRange()
                                    .lowerEndpoint() + fieldSeparator);
                            break;
                        case PEAK_MZMAX:
                            line.append(peak.getRawDataPointsMZRange()
                                    .upperEndpoint() + fieldSeparator);

                            break;

                        }
                    } else {
                        switch (dataFileElements[i]) {
                        case PEAK_STATUS:
                            line.append(FeatureStatus.UNKNOWN + fieldSeparator);
                            break;
                        default:
                            line.append("0" + fieldSeparator);
                            break;
                        }
                    }
                }
            }

            line.append("\n");

            try {
                writer.write(line.toString());
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not write to file " + fileName);
                return;
            }

            processedRows++;
        }
    }

    private String escapeStringForCSV(final String inputString) {

        if (inputString == null)
            return "";

        // Remove all special characters (particularly \n would mess up our CSV
        // format).
        String result = inputString.replaceAll("[\\p{Cntrl}]", " ");

        // Skip too long strings (see Excel 2007 specifications)
        if (result.length() >= 32766)
            result = result.substring(0, 32765);

        // If the text contains fieldSeparator, we will add
        // parenthesis
        if (result.contains(fieldSeparator) || result.contains("\"")) {
            result = "\"" + result.replaceAll("\"", "'") + "\"";
        }

        return result;
    }
}
