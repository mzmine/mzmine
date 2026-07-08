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
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataColumnMapping;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters.MetadataFileFormat;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportTask;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfig;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class MassDynamicsExportTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(MassDynamicsExportTask.class.getName());

  private static final String METABOLITE_EXTENSION = "tsv";
  private static final String METABOLITE_SUFFIX = "_massdynamics_metabolite";
  private static final String EXPERIMENT_METADATA_SUFFIX = "_massdynamics_experiment_metadata";
  public static final String[] HEADER = {"MetaboliteId", "MetaboliteName", "Smiles",
      "IsomericSmiles", "InChI", "InChIKey", "HMDB", "KEGG", "mz", "RetentionTime", "CCS",
      "IonMobility", "SampleName",
      "MetaboliteIntensity", "Imputed"};

  private final FeatureList featureList;
  private final File baseFileName;
  private final AbundanceMeasure abundanceMeasure;
  private final ImputationFunctions missingValueImputation;
  private final String conditionColumn;
  private final String defaultCondition;
  private final List<RawDataFile> rawDataFiles;
  private FeaturesDataTable dataTable;
  private @NotNull MetadataTable metadata;
  private @NotNull MetadataColumn<String> sampleNameColumn;
  private @Nullable Map<RawDataFile, Object> sampleNamesMap;

  protected MassDynamicsExportTask(@Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      @NotNull final FeatureList featureList) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureList = featureList;
    this.baseFileName = parameters.getValue(MassDynamicsExportParameters.filename);
    this.abundanceMeasure = parameters.getValue(MassDynamicsExportParameters.abundanceMeasure);
    this.missingValueImputation = parameters.getValue(
        MassDynamicsExportParameters.missingValueImputation);
    this.conditionColumn = parameters.getValue(MassDynamicsExportParameters.conditionColumn);
    this.defaultCondition = parameters.getEmbeddedParameterValueIfSelectedOrElseGet(
        MassDynamicsExportParameters.defaultCondition, () -> "");

    rawDataFiles = featureList.getRawDataFiles();
    totalItems = (long) featureList.getNumberOfRows() * rawDataFiles.size() + 1;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

  @Override
  protected void process() {
    final File flistFilename = getFileForFeatureList(featureList, this.baseFileName);
    if (flistFilename == null) {
      error("Could not create directories for Mass Dynamics export file " + this.baseFileName);
      return;
    }

    metadata = ProjectService.getMetadata();
    sampleNameColumn = metadata.getSampleNameColumn();
    sampleNamesMap = metadata.getColumnData(sampleNameColumn);

    final File metaboliteFile = FileAndPathUtil.getRealFilePathWithSuffix(flistFilename,
        METABOLITE_SUFFIX + "." + METABOLITE_EXTENSION);

    try (final ICSVWriter writer = CSVParsingUtils.createDefaultWriter(metaboliteFile, '\t',
        WriterOptions.REPLACE)) {
      exportFeatureList(writer);
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Error during Mass Dynamics metabolite export to " + flistFilename.getAbsolutePath(), e);
      error("Could not export Mass Dynamics metabolite TSV to " + flistFilename.getAbsolutePath(),
          e);
      return;
    }

    if (isCanceled()) {
      return;
    }

    if (!exportMetadata(getExperimentMetadataFile(flistFilename))) {
      return;
    }

    logger.info(
        () -> "Exported Mass Dynamics files for feature list " + featureList.getName() + " to "
            + flistFilename.getParent());
  }

  private boolean exportMetadata(@NotNull final File file) {
    final ParameterSet metadataParameters = ProjectMetadataExportParameters.create(file, false,
        MetadataFileFormat.MASS_DYNAMICS);
    metadataParameters.setParameter(ProjectMetadataExportParameters.columnMappings, true,
        List.of(new ProjectMetadataColumnMapping(conditionColumn, "condition", defaultCondition)));

    final ProjectMetadataExportTask metadataTask = new ProjectMetadataExportTask(moduleCallDate,
        metadataParameters, featureList.getRawDataFiles());
    metadataTask.run();

    if (metadataTask.getStatus() == TaskStatus.ERROR) {
      error("Could not export Mass Dynamics metadata CSV to " + file.getAbsolutePath() + ": "
          + Objects.toString(metadataTask.getErrorMessage(), ""));
      return false;
    }
    if (metadataTask.getStatus() == TaskStatus.CANCELED) {
      cancel();
      return false;
    }
    if (metadataTask.getStatus() != TaskStatus.FINISHED) {
      error("Could not export Mass Dynamics metadata CSV to " + file.getAbsolutePath());
      return false;
    }

    incrementFinishedItems();
    return true;
  }

  private void exportFeatureList(@NotNull final ICSVWriter writer) {
    writer.writeNext(HEADER);

    prepareImputedIntensities();
    for (final FeatureListRow row : featureList.getRows()) {
      for (final RawDataFile rawDataFile : rawDataFiles) {
        final @NotNull String[] line = createLine(row, rawDataFile);
        if (isCanceled()) {
          return;
        }

        writer.writeNext(line);
        incrementFinishedItems();
      }
    }
  }

  private @Nullable String[] createLine(@NotNull final FeatureListRow row,
      @NotNull final RawDataFile rawDataFile) {

    final Object sampleName = sampleNamesMap.get(rawDataFile);
    if (sampleName == null) {
      final String missingNames = rawDataFiles.stream()
          .filter(raw -> sampleNamesMap.get(raw) == null).map(RawDataFile::getName).sorted()
          .collect(Collectors.joining("\n"));
      error("""
          %s metadata column has sample name for data files:
          %s""".formatted(sampleNameColumn.getTitle(), missingNames));
      return null;
    }


    final FeatureAnnotation annotation = row.getPreferredAnnotation();
    final Feature feature = row.getFeature(rawDataFile);
    final Float rawAbundance = getAbundance(feature);
    final boolean imputed = isMissingAbundance(rawAbundance);
    final String metaboliteId = getMetaboliteId(row);

    // maybe missing value imputed
    final double actualAbundance = getActualImputedAbundance(row, rawDataFile);
    return new String[]{metaboliteId,
        firstNotBlank(annotation == null ? null : annotation.getCompoundName(), metaboliteId),
        text(annotation == null ? null : annotation.getSmiles()),
        text(annotation == null ? null : annotation.getIsomericSmiles()),
        text(annotation == null ? null : annotation.getInChI()),
        text(annotation == null ? null : annotation.getInChIKey()), "", "",
        text(row.getAverageMZ()), //
        text(row.getAverageRT()),  //
        text(row.getAverageCCS()),  //
        text(row.getAverageMobility()),  //
        sampleName.toString(), //
        imputed ? Double.toString(actualAbundance) : rawAbundance.toString(), imputed ? "1" : "0"};
  }

  /**
   * Always a finite value, never NaN
   */
  private double getActualImputedAbundance(@NonNull FeatureListRow row,
      @NonNull RawDataFile rawDataFile) {
    final double value = dataTable.getValue(row, rawDataFile);
    if (Double.compare(value, 0d) <= 0 || !Double.isFinite(value)) {
      return 0d;
    }
    return value;
  }

  private @Nullable Float getAbundance(@Nullable final Feature feature) {
    if (feature == null) {
      return null;
    }
    return switch (abundanceMeasure) {
      case Area -> feature.getArea();
      case Height -> feature.getHeight();
      case NORMALIZED_AREA, NORMALIZED_HEIGHT ->
          feature instanceof ModularDataModel dataModel ? abundanceMeasure.get(dataModel) : null;
    };
  }

  private void prepareImputedIntensities() {
    final var config = new AbundanceDataTablePreparationConfig(abundanceMeasure,
        missingValueImputation);
    dataTable = StatisticUtils.extractAbundancesPrepareData(featureList.getRows(),
        featureList.getRawDataFiles(), config);
  }

  private static boolean isMissingAbundance(@Nullable final Float abundance) {
    return abundance == null || !Float.isFinite(abundance) || abundance <= 0f;
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
    return "Exporting feature list " + featureList.getName() + " for Mass Dynamics";
  }
}
