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
import static io.github.mzmine.util.StringUtils.inQuotes;

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
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchWizardCreateBatchChecker {

  private static final Logger logger = Logger.getLogger(
      BatchWizardCreateBatchChecker.class.getName());
  private final WizardSequence sequenceSteps;

  public BatchWizardCreateBatchChecker(WizardSequence sequenceSteps) {
    this.sequenceSteps = sequenceSteps;
  }

  /**
   * Will open dialogs if needed
   *
   * @return true if all checks run
   */
  public boolean checks() {
    return checkSampleFilterValid() && checkMetadataAndFiltersValid();
  }


  /**
   * If RSD QC filter active: Check QC samples available
   * <p>
   * If metadata import: check format and all files covered by metadata
   *
   * @return true if ok or if user clicks proceed
   */
  private boolean checkMetadataAndFiltersValid() {
    final List<String> errors = new ArrayList<>();
    //
    final Optional<WizardStepParameters> importStep = sequenceSteps.get(DATA_IMPORT);
    final OptionalValue<File> metadata = getOptional(importStep,
        DataImportWizardParameters.metadataFile);

    // map to actual filenames that will be the name after import
    File[] dataFiles = getValue(importStep, DataImportWizardParameters.fileNames);
    dataFiles = AllSpectralDataImportParameters.streamValidatedFiles(dataFiles)
        .map(ImportFile::importedFile).toArray(File[]::new);

    // qc filter needs qc samples by filename or by
    final boolean rsdQcFilter = sequenceSteps.get(FILTER).get()
        .getValue(FilterWizardParameters.rsdQcFilter);
    boolean rsdFilterMatched = false;

    if (rsdQcFilter) {
      // any file needs to match the QC sample type
      rsdFilterMatched = Arrays.stream(dataFiles).map(f -> SampleType.ofString(f.getName()))
          .anyMatch(type -> type == SampleType.QC);
    }

    if (metadata.active()) {
      try {
        // use placeholders so that RawDataFiles do not need to be present.
        ProjectMetadataReader reader = new ProjectMetadataReader(false, false, true);
        final MetadataTable table = reader.readFile(metadata.value());
        errors.addAll(reader.getErrors());

        // maybe add more issues if
        final List<String> tabError = MetadataTableUtils.checkTableAgainstFiles(table, dataFiles);
        errors.addAll(tabError);

        // evaluate rsd filter if not already done by filename
        if (rsdQcFilter && !rsdFilterMatched) {
          rsdFilterMatched = checkAnyQcSampleInMetadataTable(table, dataFiles, rsdFilterMatched);
        }
      } catch (Exception e) {
        errors.add("Error reading metadata file: " + e.getMessage());
        logger.log(Level.WARNING, "Error reading metadata file: " + e.getMessage(), e);
      }
    }

    if (rsdQcFilter && !rsdFilterMatched) {
      errors.add(
          "No QC samples found in the filenames or metadata file. Add _qc to the filename or define the metadata column=%s and value=%s".formatted(
              MetadataColumn.SAMPLE_TYPE_HEADER, SampleType.QC));
    }

    if (!errors.isEmpty()) {
      // continue? y/n
      return DialogLoggerUtil.showDialogYesNo("Warning", """
          The metadata file "%s" had issues during import.
          Continue anyway?
          
          %s""".formatted(metadata.value(), String.join("\n", errors)));
    }
    return true;
  }

  private static boolean checkAnyQcSampleInMetadataTable(MetadataTable table, File[] dataFiles,
      boolean rsdFilterMatched) {
    final List<RawDataFile> qcSamples = table.getFilesOfSampleType(SampleType.QC);
    for (RawDataFile qc : qcSamples) {
      // make sure the sample is on the import list
      if (Arrays.stream(dataFiles)
          .anyMatch(file -> MetadataTableUtils.matchesFilename(file.getName(), qc))) {
        rsdFilterMatched = true;
        break;
      }
    }
    return rsdFilterMatched;
  }

  /**
   * @return true if imported samples > min num samples
   */
  private boolean checkSampleFilterValid() {
    int numFiles = getOrElse(sequenceSteps.get(DATA_IMPORT), DataImportWizardParameters.fileNames,
        new File[0]).length;

    var minSamples = getOrElse(sequenceSteps.get(FILTER), FilterWizardParameters.minNumberOfSamples,
        new AbsoluteAndRelativeInt(0, 0));
    if (minSamples.getMaximumValue(numFiles) > numFiles) {
      // continue? y/n
      return DialogLoggerUtil.showDialogYesNo("Warning", """
          The number of %s (Filters tab) does not match the number of imported data files. This may cause issues like empty feature lists.
          Continue anyway?""".formatted(
          inQuotes(FilterWizardParameters.minNumberOfSamples.getName())));
    }
    return true;
  }
}
