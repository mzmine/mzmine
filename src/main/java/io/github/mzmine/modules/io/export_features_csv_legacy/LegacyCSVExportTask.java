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

package io.github.mzmine.modules.io.export_features_csv_legacy;

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.ProcessedItemsCounter;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.RangeUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class LegacyCSVExportTask extends AbstractTask implements ProcessedItemsCounter {

  private static final Logger logger = Logger.getLogger(LegacyCSVExportTask.class.getName());
  private final FeatureList[] featureLists;
  // parameter values
  private final File fileName;
  private final String fieldSeparator;
  private final LegacyExportRowDataFileElement[] dataFileElements;
  private final Boolean exportAllFeatureInfo;
  private final String idSeparator;
  private final FeatureListRowsFilter filter;
  // track number of exported items
  private final AtomicInteger exportedRows = new AtomicInteger(0);
  private LegacyExportRowCommonElement[] commonElements;
  private int processedRows = 0, totalRows = 0;

  public LegacyCSVExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(LegacyCSVExportParameters.featureLists).getValue()
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
    refineCommonElements();
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
      LegacyExportRowDataFileElement[] dataFileElements, Boolean exportAllFeatureInfo,
      String idSeparator, FeatureListRowsFilter filter, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
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
  public int getProcessedItems() {
    return exportedRows.get();
  }

  private void refineCommonElements() {
    List<LegacyExportRowCommonElement> list = Lists.newArrayList(commonElements);

    boolean hasIonMobility = Arrays.stream(featureLists)
        .anyMatch(flist -> flist.hasRowType(MobilityType.class));

    if (!hasIonMobility) {
      list.remove(LegacyExportRowCommonElement.ROW_ION_MOBILITY);
      list.remove(LegacyExportRowCommonElement.ROW_ION_MOBILITY_UNIT);
      list.remove(LegacyExportRowCommonElement.ROW_CCS);
    }

    if (list.contains(LegacyExportRowCommonElement.ROW_BEST_ANNOTATION) && list.contains(
        LegacyExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT)) {
      list.remove(LegacyExportRowCommonElement.ROW_BEST_ANNOTATION);
      commonElements = list.toArray(new LegacyExportRowCommonElement[0]);
    }
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
    return "Exporting feature list(s) " + Arrays.toString(featureLists)
           + " to CSV file(s) (legacy MZmine 2 format)";
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    String plNamePattern = "{}";
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (FeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      // Open file
      // Open file
      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {

        exportFeatureList(featureList, writer);

      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error during mgf export to " + curFile);
        logger.log(Level.WARNING,
            "Error during MZmine 2 legacy csv export of feature list: " + featureList.getName()
            + ": " + e.getMessage(), e);
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

  private void exportFeatureList(FeatureList featureList, BufferedWriter writer) throws IOException {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    RawDataFile[] rawDataFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);

    // Buffer for writing
    StringBuilder line = new StringBuilder();

    // Write column headers

    // Common elements
    int length = commonElements.length;
    String name;
    for (int i = 0; i < length; i++) {
      if (commonElements[i].equals(LegacyExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT)) {
        line.append("best ion").append(fieldSeparator);
        line.append("auto MS2 verify").append(fieldSeparator);
        line.append("identified by n=").append(fieldSeparator);
        line.append("partners").append(fieldSeparator);
      } else if (commonElements[i].equals(LegacyExportRowCommonElement.ROW_BEST_ANNOTATION)) {
        line.append("best ion").append(fieldSeparator);
      } else {
        name = commonElements[i].toString();
        name = name.replace("Export ", "");
        name = escapeStringForCSV(name);
        line.append(name).append(fieldSeparator);
      }
    }

    // feature Information
    Set<String> featureInformationFields = new HashSet<>();

    final List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());

    final int numRows = rows.size();
    final long numFeatures = rows.stream().count();
    final long numMS2 = rows.stream().filter(FeatureListRow::hasMs2Fragmentation).count();
    final long numFiltered = rows.stream().filter(filter::accept).count();

    for (FeatureListRow row : rows) {
      if (!filter.accept(row)) {
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

    // write header to file
    writer.write(line.toString());

    // Write data rows
    for (FeatureListRow featureListRow : rows) {

      if (!filter.accept(featureListRow)) {
        processedRows++;
        continue;
      }

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Reset the buffer
      line.setLength(0);

      try {

        // Common elements
        addCommonElementsToLine(mzForm, line, commonElements.length, featureListRow);

        // feature Information
        if (exportAllFeatureInfo) {
          if (featureListRow.getFeatureInformation() != null) {
            Map<String, String> allPropertiesMap = featureListRow.getFeatureInformation()
                .getAllProperties();

            for (String key : featureInformationFields) {
              String value = allPropertiesMap.get(key);
              if (value == null) {
                value = "";
              }
              line.append(value).append(fieldSeparator);
            }
          }
        }

        // Data file elements
        addDataFileElementsToLine(rawDataFiles, line, dataFileElements.length, featureListRow);
        line.append("\n");

      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error during line export in legacy CSV: " + ex.getMessage(), ex);
      }
      // write data row to file
      writer.write(line.toString());

      exportedRows.incrementAndGet();
      processedRows++;
    }

    // check that nothing has changed during processing
    checkConcurrentModification(featureList, rows, numRows, numFeatures, numMS2, numFiltered);
  }

  private void addDataFileElementsToLine(RawDataFile[] rawDataFiles, StringBuilder line, int length,
      FeatureListRow featureListRow) {
    for (RawDataFile dataFile : rawDataFiles) {
      for (int i = 0; i < length; i++) {
        Feature feature = featureListRow.getFeature(dataFile);
        if (feature != null) {
          switch (dataFileElements[i]) {
            case FEATURE_STATUS -> line.append(feature.getFeatureStatus()).append(fieldSeparator);
            case FEATURE_NAME -> line.append(FeatureUtils.featureToString(feature))
                .append(fieldSeparator);
            case FEATURE_MZ -> line.append(feature.getMZ()).append(fieldSeparator);
            case FEATURE_RT -> append(line, feature.getRT());
            case FEATURE_ION_MOBILITY -> append(line, feature.getMobility());
            case FEATURE_ION_MOBILITY_UNIT -> append(line, feature.getMobilityUnit());
            case FEATURE_CCS -> append(line, feature.getCCS());
            case FEATURE_RT_START -> line.append(feature.getRawDataPointsRTRange().lowerEndpoint())
                .append(fieldSeparator);
            case FEATURE_RT_END -> line.append(feature.getRawDataPointsRTRange().upperEndpoint())
                .append(fieldSeparator);
            case FEATURE_DURATION -> line.append(
                RangeUtils.rangeLength(feature.getRawDataPointsRTRange())).append(fieldSeparator);
            case FEATURE_HEIGHT -> line.append(feature.getHeight()).append(fieldSeparator);
            case FEATURE_AREA -> line.append(feature.getArea()).append(fieldSeparator);
            case FEATURE_CHARGE -> line.append(feature.getCharge()).append(fieldSeparator);
            case FEATURE_DATAPOINTS -> line.append(feature.getScanNumbers().size())
                .append(fieldSeparator);
            case FEATURE_FWHM -> line.append(feature.getFWHM()).append(fieldSeparator);
            case FEATURE_TAILINGFACTOR -> line.append(feature.getTailingFactor())
                .append(fieldSeparator);
            case FEATURE_ASYMMETRYFACTOR -> line.append(feature.getAsymmetryFactor())
                .append(fieldSeparator);
            case FEATURE_MZMIN -> line.append(feature.getRawDataPointsMZRange().lowerEndpoint())
                .append(fieldSeparator);
            case FEATURE_MZMAX -> line.append(feature.getRawDataPointsMZRange().upperEndpoint())
                .append(fieldSeparator);
          }
        } else {
          switch (dataFileElements[i]) {
            case FEATURE_STATUS -> line.append(FeatureStatus.UNKNOWN).append(fieldSeparator);
            default -> line.append("0").append(fieldSeparator);
          }
        }
      }
    }
  }

  private void append(StringBuilder line, Object val) {
    append(line, val, "");
  }

  private void append(StringBuilder line, Object val, String defaultVal) {
    line.append(val == null ? defaultVal : val).append(fieldSeparator);
  }

  private void addCommonElementsToLine(NumberFormat mzForm, StringBuilder line, int length,
      FeatureListRow featureListRow) {
    for (int i = 0; i < length; i++) {
      switch (commonElements[i]) {
        case ROW_ID:
          line.append(featureListRow.getID()).append(fieldSeparator);
          break;
        case ROW_MZ:
          line.append(featureListRow.getAverageMZ()).append(fieldSeparator);
          break;
        case ROW_RT:
          final Float rt = featureListRow.getAverageRT();
          line.append(rt == null ? "" : rt).append(fieldSeparator);
          break;
        case ROW_ION_MOBILITY:
          final Float mobility = featureListRow.getAverageMobility();
          line.append(mobility == null ? "" : mobility).append(fieldSeparator);
          break;
        case ROW_ION_MOBILITY_UNIT:
          final MobilityType unit = featureListRow.get(MobilityUnitType.class);
          line.append(unit == null ? "" : unit).append(fieldSeparator);
          break;
        case ROW_CCS:
          final Float ccs = featureListRow.getAverageCCS();
          line.append(ccs == null ? "" : ccs).append(fieldSeparator);
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
          line.append(propertyValue).append(fieldSeparator);
          break;
        case ROW_IDENTITY_ALL:
          // Identity elements
          propertyValue = featureListRow.getPeakIdentities().stream().filter(Objects::nonNull)
              .map(Object::toString).collect(Collectors.joining(idSeparator));
          propertyValue = escapeStringForCSV(propertyValue);
          line.append(propertyValue).append(fieldSeparator);
          break;
        case ROW_IDENTITY_DETAILS:
          featureId = featureListRow.getPreferredFeatureIdentity();
          if (featureId == null) {
            line.append(fieldSeparator);
            break;
          } else {
            propertyValue = featureId.getDescription();
            if (propertyValue != null) {
              propertyValue = propertyValue.replaceAll("\\n", ";");
            }
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue).append(fieldSeparator);
          }
          break;
        case ROW_COMMENT:
          String comment = escapeStringForCSV(featureListRow.getComment());
          line.append(comment).append(fieldSeparator);
          break;
        case ROW_FEATURE_NUMBER:
          int numDetected = 0;
          for (Feature p : featureListRow.getFeatures()) {
            if (p.getFeatureStatus() == FeatureStatus.DETECTED) {
              numDetected++;
            }
          }
          line.append(numDetected).append(fieldSeparator);
          break;
        case ROW_CORR_GROUP_ID:
          int gid = featureListRow.getGroupID();
          line.append(gid == -1 ? "" : gid).append(fieldSeparator);

          break;
        case ROW_MOL_NETWORK_ID:
          IonIdentity ion = featureListRow.getBestIonIdentity();
          line.append(ion == null ? "" : ion.getNetID()).append(fieldSeparator);
          break;
        case ROW_BEST_ANNOTATION:
          IonIdentity ion3 = featureListRow.getBestIonIdentity();
          line.append(ion3 == null ? "" : ion3.getNetID()).append(fieldSeparator);
          break;
        case ROW_BEST_ANNOTATION_AND_SUPPORT:
          IonIdentity ad = featureListRow.getBestIonIdentity();
          if (ad == null) {
            line.append(fieldSeparator).append(fieldSeparator).append(fieldSeparator)
                .append(fieldSeparator);
          } else {
            String msms = "";
            if (ad.getMSMSModVerify() > 0) {
              msms = "MS/MS verified: nloss";
            }
            if (ad.getMSMSMultimerCount() > 0) {
              msms += msms.isEmpty() ? "MS/MS verified: xmer" : (idSeparator + " xmer");
            }
            String partners = ad.getPartnerRowsString(idSeparator);
            line.append(ad.getIonType().toString(false)).append(fieldSeparator) //
                .append(msms).append(fieldSeparator) //
                .append(ad.getPartnerRows().toArray().length).append(fieldSeparator) //
                .append(partners).append(fieldSeparator);
          }
          break;
        case ROW_NEUTRAL_MASS:
          IonIdentity ion2 = featureListRow.getBestIonIdentity();
          if (ion2 == null || ion2.getNetwork() == null) {
            line.append(fieldSeparator);
          } else {
            line.append(mzForm.format(ion2.getNetwork().calcNeutralMass())).append(fieldSeparator);
          }
          break;
      }
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

  private void checkConcurrentModification(FeatureList featureList, List<FeatureListRow> rows,
      int numRows, long numFeatures, long numMS2, long numFiltered) {
    final int numRowsEnd = rows.size();
    final long numFeaturesEnd = rows.stream().count();
    final long numMS2End = rows.stream().filter(FeatureListRow::hasMs2Fragmentation).count();
    final long numFilteredEnd = rows.stream().filter(filter::accept).count();

    logger.finer(String.format(
        "flist=%s    MS2=%d    newMS2=%d    features=%d    newF=%d   filtered=%d   fitleredEnd=%d",
        featureList.getName(), numMS2, numMS2End, numFeatures, numFeaturesEnd, numFiltered,
        numFilteredEnd));

    if (numRows != numRowsEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) legacy csv export old=%d new=%d",
          featureList.getName(), numRows, numRowsEnd));
    }
    if (numFeatures != numFeaturesEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) legacy csv export old=%d new=%d",
          featureList.getName(), numFeatures, numFeaturesEnd));
    }
    if (numMS2 != numMS2End) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS WITH MS2 during featurelist (%s) legacy csv export old=%d new=%d",
          featureList.getName(), numMS2, numMS2End));
    }
  }
}
