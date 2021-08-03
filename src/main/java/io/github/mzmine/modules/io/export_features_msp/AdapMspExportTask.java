/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.export_features_msp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class AdapMspExportTask extends AbstractTask {

  private static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("^[\\w]+$");

  private final FeatureList[] featureLists;
  private final File fileName;
  private final String plNamePattern = "{}";
  private final boolean addRetTime;
  private final String retTimeAttributeName;
  private final boolean addAnovaPValue;
  private final String anovaAttributeName;
  private final boolean integerMZ;
  private final IntegerMode roundMode;

  AdapMspExportTask(ParameterSet parameters) {
    super(null); // no new data stored -> null
    this.featureLists = parameters.getParameter(AdapMspExportParameters.FEATURE_LISTS).getValue()
        .getMatchingFeatureLists();

    this.fileName = parameters.getParameter(AdapMspExportParameters.FILENAME).getValue();

    this.addRetTime = parameters.getParameter(AdapMspExportParameters.ADD_RET_TIME).getValue();
    this.retTimeAttributeName = parameters.getParameter(AdapMspExportParameters.ADD_RET_TIME)
        .getEmbeddedParameter().getValue();

    this.addAnovaPValue =
        parameters.getParameter(AdapMspExportParameters.ADD_ANOVA_P_VALUE).getValue();
    this.anovaAttributeName = parameters.getParameter(AdapMspExportParameters.ADD_ANOVA_P_VALUE)
        .getEmbeddedParameter().getValue();

    this.integerMZ = parameters.getParameter(AdapMspExportParameters.INTEGER_MZ).getValue();

    this.roundMode = parameters.getParameter(AdapMspExportParameters.INTEGER_MZ)
        .getEmbeddedParameter().getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return 0.0;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to MSP file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    /*
     * // Total number of rows for (PeakList peakList: peakLists) { totalRows +=
     * peakList.getNumberOfRows(); }
     */

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

      try {
        exportFeatureList(featureList, writer, curFile);
      } catch (IOException | IllegalArgumentException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error while writing into file " + curFile + ": " + e.getMessage());
        return;
      }

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
      if (!substitute)
        break;
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private void exportFeatureList(FeatureList featureList, FileWriter writer, File curFile)
      throws IOException {
    final String newLine = System.lineSeparator();

    for (FeatureListRow row : featureList.getRows()) {
      IsotopePattern ip = row.getBestIsotopePattern();
      if (ip == null)
        continue;

      String name = row.toString();
      if (name != null)
        writer.write("Name: " + name + newLine);

      FeatureIdentity identity = row.getPreferredFeatureIdentity();
      if (identity != null) {
        // String name = identity.getName();
        // if (name != null) writer.write("Name: " + name + newLine);

        String formula = identity.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA);
        if (formula != null)
          writer.write("Formula: " + formula + newLine);

        String id = identity.getPropertyValue(FeatureIdentity.PROPERTY_ID);
        if (id != null)
          writer.write("Comments: " + id + newLine);
      }

      String rowID = Integer.toString(row.getID());
      if (rowID != null)
        writer.write("DB#: " + rowID + newLine);

      if (addRetTime) {
        String attributeName = checkAttributeName(retTimeAttributeName);
        writer.write(attributeName + ": " + row.getAverageRT() + newLine);
      }

      FeatureInformation featureInformation = row.getFeatureInformation();
      if (addAnovaPValue && featureInformation != null
          && featureInformation.getAllProperties().containsKey("ANOVA_P_VALUE")) {
        String attributeName = checkAttributeName(anovaAttributeName);
        String value = featureInformation.getPropertyValue("ANOVA_P_VALUE");
        if (value.trim().length() > 0)
          writer.write(attributeName + ": " + value + newLine);
      }

      DataPoint[] dataPoints = ScanUtils.extractDataPoints(ip);

      if (integerMZ)
        dataPoints = ScanUtils.integerDataPoints(dataPoints, roundMode);

      String numFeatures = Integer.toString(dataPoints.length);
      if (numFeatures != null)
        writer.write("Num Features: " + numFeatures + newLine);

      for (DataPoint point : dataPoints) {
        String line = point.getMZ() + " " + point.getIntensity();
        writer.write(line + newLine);
      }

      writer.write(newLine);
    }
  }

  private String checkAttributeName(String name) {
    Matcher matcher = ATTRIBUTE_NAME_PATTERN.matcher(name);
    if (matcher.find())
      return name;
    throw new IllegalArgumentException(String.format(
        "Incorrect attribute name \"%s\". Attribute name may contain only latin letters, digits, and underscore '_'",
        name));
  }
}
