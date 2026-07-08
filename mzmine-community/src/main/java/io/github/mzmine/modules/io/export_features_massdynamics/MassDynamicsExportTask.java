/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_features_massdynamics;

import com.opencsv.ICSVWriter;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters.MetadataFileFormat;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataWriter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MassDynamicsExportTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(MassDynamicsExportTask.class.getName());

  private static final String METABOLITE_EXTENSION = "tsv";
  private static final String METABOLITE_SUFFIX = "_massdynamics_metabolite";
  private static final String EXPERIMENT_METADATA_SUFFIX = "_massdynamics_experiment_metadata";
  private static final String[] HEADER = {"MetaboliteId", "MetaboliteName", "Smiles",
      "IsomericSmiles", "InChI", "InChIKey", "HMDB", "KEGG", "mz", "RetentionTime", "SampleName",
      "MetaboliteIntensity", "Imputed"};

  private final FeatureList featureList;
  private final File baseFileName;
  private final AbundanceMeasure abundanceMeasure;

  protected MassDynamicsExportTask(@Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      @NotNull final FeatureList featureList) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureList = featureList;
    this.baseFileName = parameters.getValue(MassDynamicsExportParameters.filename);
    this.abundanceMeasure = parameters.getValue(MassDynamicsExportParameters.abundanceMeasure);
    totalItems = (long) featureList.getNumberOfRows() * featureList.getRawDataFiles().size() + 2;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

  @Override
  protected void process() {
    final File flistFilename = getFileForFeatureList(featureList, this.baseFileName);
    if (flistFilename == null) {
      error("Could not create directories for MassDynamics export file " + this.baseFileName);
      return;
    }

    final File metaboliteFile = FileAndPathUtil.getRealFilePathWithSuffix(flistFilename,
        METABOLITE_SUFFIX + ".tsv");

    try (final ICSVWriter writer = CSVParsingUtils.createDefaultWriter(metaboliteFile, '\t',
        WriterOptions.REPLACE)) {
      exportFeatureList(writer);
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Error during MassDynamics metabolite export to " + flistFilename.getAbsolutePath(), e);
      error("Could not export MassDynamics metabolite TSV to " + flistFilename.getAbsolutePath(),
          e);
      return;
    }

    final ProjectMetadataWriter metadataWriter = new ProjectMetadataWriter(
        ProjectService.getProject().getProjectMetadata(), MetadataFileFormat.MASS_DYNAMICS,
        ProjectMetadataExportParameters.MASS_DYNAMICS_DEFAULT_MAPPINGS);
    if (!exportMetadata(metadataWriter, getExperimentMetadataFile(flistFilename))) {
      return;
    }

    logger.info(
        () -> "Exported MassDynamics files for feature list " + featureList.getName() + " to "
            + flistFilename.getParent());
  }

  private boolean exportMetadata(@NotNull final ProjectMetadataWriter metadataWriter,
      @NotNull final File file) {
    if (!metadataWriter.exportTo(file, featureList.getRawDataFiles())) {
      error("Could not export MassDynamics metadata CSV to " + file.getAbsolutePath());
      return false;
    }
    incrementFinishedItems();
    return true;
  }

  private void exportFeatureList(@NotNull final ICSVWriter writer) {
    writer.writeNext(HEADER);
    final List<RawDataFile> rawDataFiles = featureList.getRawDataFiles();
    for (final FeatureListRow row : featureList.getRows()) {
      for (final RawDataFile rawDataFile : rawDataFiles) {
        if (isCanceled()) {
          return;
        }

        writer.writeNext(createLine(row, rawDataFile));
        incrementFinishedItems();
      }
    }
  }

  private @NotNull String[] createLine(@NotNull final FeatureListRow row,
      @NotNull final RawDataFile rawDataFile) {
    final FeatureAnnotation annotation = row.getPreferredAnnotation();
    final Feature feature = row.getFeature(rawDataFile);
    final Float abundance = getAbundance(feature);
    final boolean imputed = abundance == null || !Float.isFinite(abundance) || abundance <= 0f;
    final String metaboliteId = getMetaboliteId(row);

    return new String[]{metaboliteId,
        firstNotBlank(annotation == null ? null : annotation.getCompoundName(), metaboliteId),
        text(annotation == null ? null : annotation.getSmiles()),
        text(annotation == null ? null : annotation.getIsomericSmiles()),
        text(annotation == null ? null : annotation.getInChI()),
        text(annotation == null ? null : annotation.getInChIKey()), "", "",
        text(row.getAverageMZ()), text(row.getAverageRT()), rawDataFile.getName(),
        imputed ? "0.0" : abundance.toString(), imputed ? "1" : "0"};
  }

  private @Nullable Float getAbundance(@Nullable final Feature feature) {
    if (feature == null) {
      return null;
    }
    return abundanceMeasure.get((ModularDataModel) feature);
  }

  private static @NotNull String getMetaboliteId(@NotNull final FeatureListRow row) {
    return "row_" + row.getID();
  }

  private static @NotNull String firstNotBlank(@Nullable final String first,
      @NotNull final String fallback) {
    return first == null || first.isBlank() ? fallback : first;
  }

  private static @NotNull String text(@Nullable final Object value) {
    return Objects.toString(value, "");
  }

  static @Nullable File getFileForFeatureList(@NotNull final FeatureList featureList,
      @NotNull final File file) {
    return SiriusExportTask.getFileForFeatureList(featureList, file,
        SiriusExportTask.MULTI_NAME_PATTERN, METABOLITE_EXTENSION);
  }

  static @NotNull File getExperimentMetadataFile(@NotNull final File metaboliteFile) {
    return FileAndPathUtil.getRealFilePathWithSuffix(metaboliteFile, EXPERIMENT_METADATA_SUFFIX,
        "csv");
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Exporting feature list " + featureList.getName() + " for MassDynamics";
  }
}
