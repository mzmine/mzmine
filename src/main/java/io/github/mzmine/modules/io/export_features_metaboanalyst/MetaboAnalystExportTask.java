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

package io.github.mzmine.modules.io.export_features_metaboanalyst;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CSVUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

class MetaboAnalystExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MetaboAnalystExportTask.class.getName());
  private static final String fieldSeparator = ",";

  private final FeatureList[] featureLists;
  private final @NotNull MetadataTable metadata;
  private final MetadataColumn<?> metadataColumn;
  private final String grouping;
  private int processedRows = 0, totalRows = 0;

  // parameter values
  private final File fileName;

  MetaboAnalystExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.featureLists = parameters.getValue(MetaboAnalystExportParameters.featureLists)
        .getMatchingFeatureLists();

    fileName = parameters.getValue(MetaboAnalystExportParameters.filename);
//    statsFormat = parameters.getValue(MetaboAnalystExportParameters.format);
    grouping = parameters.getValue(MetaboAnalystExportParameters.grouping);
    metadata = MZmineCore.getProjectMetadata();
    metadataColumn = metadata.getColumnByName(grouping);
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    if (metadataColumn == null) {
      return "Error: Metadata column not found for " + grouping;
    }
    return "Exporting feature list(s) " + Arrays.toString(featureLists)
        + " to MetaboAnalyst CSV file(s) for metadata column " + grouping;
  }

  @Override
  public void run() {
    if (metadataColumn == null) {
      setErrorMessage("Error: Metadata column not found for " + grouping);
      setStatus(TaskStatus.ERROR);
      return;
    }

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

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = FileAndPathUtil.safePathEncode(featureList.getName());
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      // Check the feature list for MetaboAnalyst requirements
      boolean checkResult = checkFeatureList(featureList);
      if (!checkResult) {
        MZmineCore.getDesktop().displayErrorMessage("Feature list " + featureList.getName()
            + " does not conform to MetaboAnalyst requirement: at least 3 samples (raw data files) in each group");
      }

      // Open file
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(curFile, false))) {
        // Get number of rows
        totalRows = featureList.getNumberOfRows();

        exportFeatureList(featureList, writer);

      } catch (Exception e) {
        logger.log(Level.WARNING, "Error during MetaboAnalyst export. " + e.getMessage(), e);
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not export feature list to file " + curFile + ": " + e.getMessage());
        return;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }

  }

  private boolean checkFeatureList(FeatureList featureList) {
    var raws = new HashSet<>(featureList.getRawDataFiles());
    // Check if each sample group has at least 3 samples
    Map<RawDataFile, Object> data = metadata.getData().get(metadataColumn);
    Map<Object, Integer> counts = data.entrySet().stream().filter(e -> raws.contains(e.getKey()))
        .map(Entry::getValue).collect(Collectors.toMap(v -> v, value -> 1, Math::addExact));

    return counts.values().stream().noneMatch(count -> count < 3);
  }

  private void exportFeatureList(FeatureList featureList, BufferedWriter writer)
      throws IOException {

    final RawDataFile[] rawDataFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);

    // Write sample (raw data file) names
    writer.append("\"Filename\"");
    for (RawDataFile raw : rawDataFiles) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      writer.append(fieldSeparator);
      final String value = raw.getName().replace('"', '\'');
      writer.append(CSVUtils.escape(value, fieldSeparator));
    }
    writer.append("\n");

    // Write grouping parameter title followed by values
    writer.append(CSVUtils.escape(metadataColumn.getTitle(), fieldSeparator));

    for (RawDataFile raw : rawDataFiles) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      writer.append(fieldSeparator);
      Object value = metadata.getValue(metadataColumn, raw);
      if (value != null) {
        writer.append(CSVUtils.escape(value.toString(), fieldSeparator));
      }
    }

    writer.append("\n");

    // Write data rows
    for (FeatureListRow featureListRow : featureList.getRows()) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      final String rowName = generateUniqueFeatureListRowName(featureListRow);

      writer.append(CSVUtils.escape(rowName, fieldSeparator));

      for (RawDataFile dataFile : rawDataFiles) {
        writer.append(fieldSeparator);

        Feature feature = featureListRow.getFeature(dataFile);
        if (feature != null) {
          final double area = feature.getArea();
          writer.append(String.valueOf(area));
        }
      }

      writer.append("\n");
      processedRows++;
    }
  }

  /**
   * Generates a unique name for each feature list row
   */
  private String generateUniqueFeatureListRowName(FeatureListRow row) {

    final double mz = row.getAverageMZ();
    final Float rt = row.getAverageRT();
    final Float mobility = row.getAverageMobility();
    final int rowId = row.getID();

    final StringBuilder generatedName = new StringBuilder();
    generatedName.append(rowId);

    String name = row.getPreferredAnnotationName();
    if (name != null) {
      generatedName.append("/").append(CSVUtils.escape(name, fieldSeparator));
    }

    generatedName.append("/").append(MZmineCore.getConfiguration().getMZFormat().format(mz))
        .append("mz");

    if (rt != null) {
      generatedName.append("/").append(MZmineCore.getConfiguration().getRTFormat().format(rt))
          .append("min");
    }
    if (mobility != null) {
      generatedName.append("/")
          .append(MZmineCore.getConfiguration().getMobilityFormat().format(mobility));
    }

    return generatedName.toString();
  }

}
