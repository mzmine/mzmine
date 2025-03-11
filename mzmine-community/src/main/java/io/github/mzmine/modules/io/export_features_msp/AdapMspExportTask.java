/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.export_features_msp;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.AnnotationUtils;
import org.jetbrains.annotations.NotNull;

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

  AdapMspExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
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

  private void exportFeatureList(FeatureList featureList,final FileWriter writer, File curFile)
      throws IOException {
    final String newLine = System.lineSeparator();

    for (FeatureListRow row : featureList.getRows()) {
      Scan ip = row.getMostIntenseFragmentScan();
      if (ip == null)
        continue;

      String name = row.toString();
      if (name != null)
        writer.write("Name: " + name + newLine);

      var identity = CompoundAnnotationUtils.getBestFeatureAnnotation(row).orElse(null);
      if(identity!=null) {
        String formula = identity.getFormula();
        if (formula != null)
          writer.write("Formula: " + formula + newLine);
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
