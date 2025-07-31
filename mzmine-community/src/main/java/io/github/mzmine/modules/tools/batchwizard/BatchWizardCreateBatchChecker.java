/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import static io.github.mzmine.modules.tools.batchwizard.WizardPart.DATA_IMPORT;
import static io.github.mzmine.modules.tools.batchwizard.WizardPart.FILTER;
import static io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder.getOptional;
import static io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder.getOrElse;
import static io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder.getValue;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.ImportFile;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.FilterWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataReader;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesFilter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesFilterConfig;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.constraints.Null;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatchWizardCreateBatchChecker {

  private static final Logger logger = Logger.getLogger(
      BatchWizardCreateBatchChecker.class.getName());

  private final WizardSequence sequenceSteps;

  private final List<String> errors = new ArrayList<>();
  private final @Null File metadata;
  private final File[] dataFiles;
  private final boolean rsdQcFilter;
  private final @Nullable MetadataTable table;
  // only present if metadata loaded
  private final @NotNull Map<String, RawDataFile> nameFileMap;
  // only present if metadata loaded
  private final List<RawDataFile> rawFilesInMetadata;

  public BatchWizardCreateBatchChecker(WizardSequence sequenceSteps) {
    this.sequenceSteps = sequenceSteps;
    //
    final Optional<WizardStepParameters> importStep = sequenceSteps.get(DATA_IMPORT);
    metadata = getOptional(importStep, DataImportWizardParameters.metadataFile).orElse(null);

    // map to actual filenames that will be the name after import
    File[] importFiles = getValue(importStep, DataImportWizardParameters.fileNames);
    dataFiles = AllSpectralDataImportParameters.streamValidatedFiles(importFiles)
        .map(ImportFile::importedFile).toArray(File[]::new);

    // qc filter needs qc samples by filename or by
    rsdQcFilter = sequenceSteps.get(FILTER).get().getValue(FilterWizardParameters.rsdQcFilter);

    table = readTable();
    nameFileMap = table == null ? Map.of() : MetadataTableUtils.matchFileNames(table, dataFiles);
    rawFilesInMetadata = List.copyOf(nameFileMap.values());
  }

  private @Nullable MetadataTable readTable() {
    if (metadata == null) {
      return null;
    }
    if (metadata.getName().isBlank()) {
      errors.add("Metadata file name is empty.");
      errors.add("");
      return null;
    }
    try {
      // use placeholders so that RawDataFiles do not need to be present.
      ProjectMetadataReader reader = new ProjectMetadataReader(false, false, true);
      MetadataTable table = reader.readFile(metadata);

      if (!reader.getErrors().isEmpty()) {
        errors.add("Issues when reading metadata file from path: " + metadata.getAbsolutePath());
        errors.addAll(reader.getErrors());
        errors.add("");
      }
      return table;
    } catch (Exception e) {
      errors.add("Error reading metadata file: " + e.getMessage());
      errors.add("");
      logger.log(Level.WARNING, "Error reading metadata file: " + e.getMessage(), e);
    }
    return null;
  }

  /**
   * Will open dialogs if needed
   *
   * @return true if all checks run
   */
  public boolean checks() {

    checkMetadataTableValid();
    checkSampleFilterValid();
    checkMinSamplesAnyGroup();
    checkRsdQcFilter();

    if (!errors.isEmpty()) {
      // continue? y/n
      return DialogLoggerUtil.showDialogYesNo("Warning", """
          %s
          Continue anyway?""".formatted(String.join("\n", errors)));
    }
    return true;
  }

  private void checkMetadataTableValid() {
    if (table == null) {
      return;
    }

    final List<String> missingInMetadata = new ArrayList<>();

    for (File file : dataFiles) {
      final RawDataFile raw = getRaw(file);
      if (raw == null) {
        missingInMetadata.add(file.getName());
      }
    }
    if (!missingInMetadata.isEmpty()) {
      errors.add("The following files are not present in the metadata file: %s".formatted(
          String.join(", ", missingInMetadata)));
      errors.add("");
    }
  }

  private RawDataFile getRaw(File file) {
    return nameFileMap.get(file.getName());
  }

  /**
   * If RSD QC filter active: Check QC samples available
   */
  private void checkRsdQcFilter() {
    if (!rsdQcFilter) {
      return;
    }

    // two files need to match the QC sample type
    long rsdFilterMatched = Arrays.stream(dataFiles).map(f -> SampleType.ofString(f.getName()))
        .filter(type -> type == SampleType.QC).count();

    if (rsdFilterMatched >= 2 || (table != null && checkAtLeastTwoQcSampleInMetadataTable())) {
      return;
    }

    // qc issue
    errors.add("""
        No QC samples found in the filenames or metadata file. Add _qc to the filename or define the metadata column as "%s" with value "%s".""".formatted(
        MetadataColumn.SAMPLE_TYPE_HEADER, SampleType.QC));
  }

  private void checkMinSamplesAnyGroup() {
    // also check
    var minSamplesAnyGroup = getOptional(sequenceSteps.get(FILTER),
        FilterWizardParameters.minNumberOfSamplesInAnyGroup);
    if (!minSamplesAnyGroup.active()) {
      return;
    }
    final MinimumSamplesFilterConfig config = minSamplesAnyGroup.value();

    if (table == null) {
      errors.add(
          "Metadata file missing. To use %s (Filters tab), a metadata table needs to be imported. Define one in the Data tab.");
      errors.add("");
      return;
    }
    final MetadataColumn<?> column = table.getColumnByName(config.columnName());

    if (column == null) {
      errors.add("""
          Metadata column missing. To use using "%s" (Filters tab), the metadata file must contain a column named "%s".""".formatted(
          FilterWizardParameters.minNumberOfSamples.getName(), config.columnName()));
      errors.add("");
      return;
    }

    try {
      final MinimumSamplesFilter filter = config.createFilter(rawFilesInMetadata, table);

      final int maxGroupSize = table.groupFilesByColumn(column).values().stream()
          .mapToInt(List::size).max().orElse(0);

      final AbsoluteAndRelativeInt minSamples = filter.minSamples();

      if (minSamples.getMaximumValue(maxGroupSize) > maxGroupSize) {
        final String error = """
            The minimum samples number of "%s" (Filters tab; %s) does not match the number of imported data files per group (max=%d). This may cause issues like empty feature lists.""".formatted(
            FilterWizardParameters.minNumberOfSamplesInAnyGroup.getName(), minSamples,
            maxGroupSize);
        errors.add(error);
        errors.add("");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error creating filter for %s (Filters tab).".formatted(
          FilterWizardParameters.minNumberOfSamplesInAnyGroup.getName()));
    }
  }

  private boolean checkAtLeastTwoQcSampleInMetadataTable() {
    if (table == null) {
      return false;
    }

    int qcSamples = 0;

    // actually imported raw data files
    final MetadataColumn<String> sampleTypeColumn = table.getSampleTypeColumn();
    for (RawDataFile raw : nameFileMap.values()) {
      final String value = table.getValue(sampleTypeColumn, raw);
      if (SampleType.ofString(value) == SampleType.QC) {
        qcSamples++;
      }
      if (qcSamples >= 2) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   */
  private void checkSampleFilterValid() {
    int numFiles = dataFiles.length;

    var minSamples = getOrElse(sequenceSteps.get(FILTER), FilterWizardParameters.minNumberOfSamples,
        new AbsoluteAndRelativeInt(0, 0));
    if (minSamples.getMaximumValue(numFiles) > numFiles) {
      final String error = """
          The minimum samples of "%s" (Filters tab; %s) does not match the number of imported data files %d. This may cause issues like empty feature lists.""".formatted(
          FilterWizardParameters.minNumberOfSamples.getName(), minSamples, numFiles);
      errors.add(error);
      errors.add("");
    }
  }
}
