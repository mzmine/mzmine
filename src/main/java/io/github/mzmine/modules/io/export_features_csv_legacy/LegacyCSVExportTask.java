/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.io.export_features_csv_legacy;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.io.export_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.util.FeatureUtils;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RangeUtils;

public class LegacyCSVExportTask extends AbstractTask {

  private final FeatureList[] featureLists;
  private int processedRows = 0, totalRows = 0;

  // parameter values
  private final File fileName;
  private final String plNamePattern = "{}";
  private final String fieldSeparator;
  private final LegacyExportRowCommonElement[] commonElements;
  private final LegacyExportRowDataFileElement[] dataFileElements;
  private final Boolean exportAllFeatureInfo;
  private final String idSeparator;
  private final FeatureListRowsFilter filter;

  public LegacyCSVExportTask(ParameterSet parameters) {
    this.featureLists =
        parameters.getParameter(LegacyCSVExportParameters.featureLists).getValue()
            .getMatchingFeatureLists();
    fileName = parameters.getParameter(LegacyCSVExportParameters.filename).getValue();
    fieldSeparator = parameters.getParameter(LegacyCSVExportParameters.fieldSeparator).getValue();
    commonElements = parameters.getParameter(LegacyCSVExportParameters.exportCommonItems)
        .getValue();
    dataFileElements = parameters.getParameter(LegacyCSVExportParameters.exportDataFileItems)
        .getValue();
    exportAllFeatureInfo = parameters.getParameter(LegacyCSVExportParameters.exportAllFeatureInfo)
        .getValue();
    idSeparator = parameters.getParameter(LegacyCSVExportParameters.idSeparator).getValue();
    this.filter = parameters.getParameter(LegacyCSVExportParameters.filter).getValue();
  }

  /**
   * @param featureLists         feature lists to export
   * @param fileName             export to file name
   * @param fieldSeparator       separator for each column
   * @param commonElements       common columns (average values etc)
   * @param dataFileElements     columns for each data file
   * @param exportAllFeatureInfo export all feature information
   * @param idSeparator          separator for identity fields
   * @param filter               Row filter
   */
  public LegacyCSVExportTask(FeatureList[] featureLists, File fileName, String fieldSeparator,
      LegacyExportRowCommonElement[] commonElements,
      LegacyExportRowDataFileElement[] dataFileElements,
      Boolean exportAllFeatureInfo, String idSeparator, FeatureListRowsFilter filter) {
    super();
    this.featureLists = featureLists;
    this.fileName = fileName;
    this.fieldSeparator = fieldSeparator;
    this.commonElements = commonElements;
    this.dataFileElements = dataFileElements;
    this.exportAllFeatureInfo = exportAllFeatureInfo;
    this.idSeparator = idSeparator;
    this.filter = filter;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to CSV file(s)";
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (FeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      // Open file
      FileWriter writer;
      try {
        writer = new FileWriter(curFile);
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        return;
      }

      exportFeatureList(featureList, writer, curFile);

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

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute) {
        break;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }

  }

  private void exportFeatureList(FeatureList featureList, FileWriter writer, File fileName) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    RawDataFile[] rawDataFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);

    // Buffer for writing
    StringBuilder line = new StringBuilder();

    // Write column headers

    // Common elements
    int length = commonElements.length;
    String name;
    for (int i = 0; i < length; i++) {
      name = commonElements[i].toString();
      name = name.replace("Export ", "");
      name = escapeStringForCSV(name);
      line.append(name);
      line.append(fieldSeparator);
    }

    // feature Information
    Set<String> featureInformationFields = new HashSet<>();

    for (FeatureListRow row : featureList.getRows()) {
      if (!filter.filter(row)) {
        continue;
      }
      if (row.getFeatureInformation() != null) {
        featureInformationFields.addAll(row.getFeatureInformation().getAllProperties().keySet());
      }
    }

    if (exportAllFeatureInfo) {
      for (String field : featureInformationFields) {
        line.append(field);
        line.append(fieldSeparator);
      }
    }

    // Data file elements
    length = dataFileElements.length;
    for (int df = 0; df < featureList.getNumberOfRawDataFiles(); df++) {
      for (int i = 0; i < length; i++) {
        name = rawDataFiles[df].getName();
        name = name + " " + dataFileElements[i].toString();
        name = escapeStringForCSV(name);
        line.append(name);
        line.append(fieldSeparator);
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
    for (FeatureListRow featureListRow : featureList.getRows()) {

      if (!filter.filter(featureListRow)) {
        processedRows++;
        continue;
      }

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
            line.append(featureListRow.getID() + fieldSeparator);
            break;
          case ROW_MZ:
            line.append(featureListRow.getAverageMZ() + fieldSeparator);
            break;
          case ROW_RT:
            line.append(featureListRow.getAverageRT() + fieldSeparator);
            break;
          case ROW_IDENTITY:
            // Identity elements
            FeatureIdentity featureId = featureListRow.getPreferredFeatureIdentity();
            if (featureId == null) {
              line.append(fieldSeparator);
              break;
            }
            String propertyValue = featureId.toString();
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_IDENTITY_ALL:
            // Identity elements
            FeatureIdentity[] featureIdentities = featureListRow.getPeakIdentities()
                .toArray(new FeatureIdentity[0]);
            propertyValue = "";
            for (int x = 0; x < featureIdentities.length; x++) {
              if (x > 0) {
                propertyValue += idSeparator;
              }
              propertyValue += featureIdentities[x].toString();
            }
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_IDENTITY_DETAILS:
            featureId = featureListRow.getPreferredFeatureIdentity();
            if (featureId == null) {
              line.append(fieldSeparator);
              break;
            }
            propertyValue = featureId.getDescription();
            if (propertyValue != null) {
              propertyValue = propertyValue.replaceAll("\\n", ";");
            }
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_COMMENT:
            String comment = escapeStringForCSV(featureListRow.getComment());
            line.append(comment + fieldSeparator);
            break;
          case ROW_FEATURE_NUMBER:
            int numDetected = 0;
            for (Feature p : featureListRow.getFeatures()) {
              if (p.getFeatureStatus() == FeatureStatus.DETECTED) {
                numDetected++;
              }
            }
            line.append(numDetected + fieldSeparator);
            break;
        }
      }

      // feature Information
      if (exportAllFeatureInfo) {
        if (featureListRow.getFeatureInformation() != null) {
          Map<String, String> allPropertiesMap =
              featureListRow.getFeatureInformation().getAllProperties();

          for (String key : featureInformationFields) {
            String value = allPropertiesMap.get(key);
            if (value == null) {
              value = "";
            }
            line.append(value + fieldSeparator);
          }
        }
      }

      // Data file elements
      length = dataFileElements.length;
      for (RawDataFile dataFile : rawDataFiles) {
        for (int i = 0; i < length; i++) {
          Feature feature = featureListRow.getFeature(dataFile);
          if (feature != null) {
            switch (dataFileElements[i]) {
              case FEATURE_STATUS:
                line.append(feature.getFeatureStatus() + fieldSeparator);
                break;
              case FEATURE_NAME:
                line.append(FeatureUtils.featureToString(feature) + fieldSeparator);
                break;
              case FEATURE_MZ:
                line.append(feature.getMZ() + fieldSeparator);
                break;
              case FEATURE_RT:
                line.append(feature.getRT() + fieldSeparator);
                break;
              case FEATURE_RT_START:
                line.append(feature.getRawDataPointsRTRange().lowerEndpoint() + fieldSeparator);
                break;
              case FEATURE_RT_END:
                line.append(feature.getRawDataPointsRTRange().upperEndpoint() + fieldSeparator);
                break;
              case FEATURE_DURATION:
                line.append(
                    RangeUtils.rangeLength(feature.getRawDataPointsRTRange()) + fieldSeparator);
                break;
              case FEATURE_HEIGHT:
                line.append(feature.getHeight() + fieldSeparator);
                break;
              case FEATURE_AREA:
                line.append(feature.getArea() + fieldSeparator);
                break;
              case FEATURE_CHARGE:
                line.append(feature.getCharge() + fieldSeparator);
                break;
              case FEATURE_DATAPOINTS:
                line.append(feature.getScanNumbers().size() + fieldSeparator);
                break;
              case FEATURE_FWHM:
                line.append(feature.getFWHM() + fieldSeparator);
                break;
              case FEATURE_TAILINGFACTOR:
                line.append(feature.getTailingFactor() + fieldSeparator);
                break;
              case FEATURE_ASYMMETRYFACTOR:
                line.append(feature.getAsymmetryFactor() + fieldSeparator);
                break;
              case FEATURE_MZMIN:
                line.append(feature.getRawDataPointsMZRange().lowerEndpoint() + fieldSeparator);
                break;
              case FEATURE_MZMAX:
                line.append(feature.getRawDataPointsMZRange().upperEndpoint() + fieldSeparator);
                break;
            }
          } else {
            switch (dataFileElements[i]) {
              case FEATURE_STATUS:
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

    if (inputString == null) {
      return "";
    }

    // Remove all special characters (particularly \n would mess up our CSV
    // format).
    String result = inputString.replaceAll("[\\p{Cntrl}]", " ");

    // Skip too long strings (see Excel 2007 specifications)
    if (result.length() >= 32766) {
      result = result.substring(0, 32765);
    }

    // If the text contains fieldSeparator, we will add
    // parenthesis
    if (result.contains(fieldSeparator) || result.contains("\"")) {
      result = "\"" + result.replaceAll("\"", "'") + "\"";
    }

    return result;
  }
}
