/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_standardcompound;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.dataprocessing.norm_linear.NormalizationFunction;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StandardCompoundNormalizerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      StandardCompoundNormalizerTask.class.getName());

  private final OriginalFeatureListOption handleOriginal;
  private final MZmineProject project;
  private final ModularFeatureList originalFeatureList;
  private ModularFeatureList normalizedFeatureList;

  private int processedRows, totalRows;

  private final String suffix;
  private final StandardUsageType normalizationType;
  private final AbundanceMeasure abundanceMeasure;
  private final double MZvsRTBalance;
  private final FeatureListRow[] standardRows;
  private final ParameterSet parameters;

  public StandardCompoundNormalizerTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.originalFeatureList = (ModularFeatureList) featureList;

    suffix = parameters.getParameter(StandardCompoundNormalizerParameters.suffix).getValue();
    normalizationType = parameters.getParameter(
        StandardCompoundNormalizerParameters.standardUsageType).getValue();
    abundanceMeasure = parameters.getParameter(
        StandardCompoundNormalizerParameters.featureMeasurementType).getValue();
    MZvsRTBalance = parameters.getParameter(StandardCompoundNormalizerParameters.MZvsRTBalance)
        .getValue();
    handleOriginal = parameters.getValue(StandardCompoundNormalizerParameters.handleOriginal);
    standardRows = parameters.getParameter(StandardCompoundNormalizerParameters.standardCompounds)
        .getMatchingRows(featureList);
    this.parameters = parameters;
  }

  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  public String getTaskDescription() {
    return "Standard compound normalization of " + originalFeatureList;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.finest("Starting standard compound normalization of " + originalFeatureList + " using "
        + normalizationType + " (total " + standardRows.length + " standard features)");

    // Check if we have standards
    if (standardRows.length == 0) {
      setErrorMessage("No internal standard features selected");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // Initialize new alignment result for the normalized result
    normalizedFeatureList = originalFeatureList.createCopy(originalFeatureList + " " + suffix,
        getMemoryMapStorage(), false);

    // Copy raw data files from original alignment result to new alignment
    // result
    totalRows = normalizedFeatureList.getNumberOfRows();

    final NormalizedAreaType normAreaType = DataTypes.get(NormalizedAreaType.class);
    final NormalizedHeightType normHeightType = DataTypes.get(NormalizedHeightType.class);
    final MetadataTable metadata = ProjectService.getMetadata();
    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> fileToFunction = createNormalizationFunctions(
        normalizedFeatureList.getRawDataFiles(), metadata);

    // Loop through all rows
    rowIteration:
    for (FeatureListRow row : normalizedFeatureList.getRows()) {

      // Cancel ?
      if (isCanceled()) {
        return;
      }

      // Do not add the standard rows to the new peaklist
      for (int i = 0; i < standardRows.length; i++) {
        if (row == standardRows[i]) {
          processedRows++;
          continue rowIteration;
        }
      }

      // Get m/z and RT of the current row
      final Double mz = row.getAverageMZ();
      if (mz == null) {
        throw new IllegalStateException("No average m/z found for row: " + row.getID());
      }
      final Float rt = row.getAverageRT();
      if (rt == null) {
        throw new IllegalStateException("No average RT found for row: " + row.getID());
      }

      // Loop through all raw data files
      for (final RawDataFile file : normalizedFeatureList.getRawDataFiles()) {
        final NormalizationFunction normalizationFunction = fileToFunction.get(file);
        if (normalizationFunction == null) {
          throw new IllegalStateException(
              "No normalization function available for file: " + file.getName());
        }
        final double normalizationFactor = normalizationFunction.getFactor(mz, rt);
        logger.finest("Normalizing row #" + row.getID() + "[" + file + "] using factor "
            + normalizationFactor);

        // Normalize feature
        final ModularFeature originalFeature = (ModularFeature) row.getFeature(file);
        if (originalFeature != null) {
          final float normalizedHeight = (float) (originalFeature.getHeight()
              * normalizationFactor);
          final float normalizedArea = (float) (originalFeature.getArea() * normalizationFactor);
          originalFeature.set(normHeightType, normalizedHeight);
          originalFeature.set(normAreaType, normalizedArea);
        }

      }

      processedRows++;
    }
    // add or remove lists
    handleOriginal.reflectNewFeatureListToProject(suffix, project, normalizedFeatureList,
        originalFeatureList);

    // Add task description to feature list
    normalizedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Standard compound normalization",
            StandardCompoundNormalizerModule.class, parameters, getModuleCallDate()));

    logger.info("Finished standard compound normalizer");
    setStatus(TaskStatus.FINISHED);

  }

  private @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createNormalizationFunctions(
      @NotNull final List<RawDataFile> rawFiles, @NotNull final MetadataTable metadata) {
    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> fileToFunction = new HashMap<>();
    for (final RawDataFile rawFile : rawFiles) {
      final List<StandardCompoundReferencePoint> referencePoints = createReferencePoints(rawFile);
      final LocalDateTime acquisitionTimestamp = getAcquisitionTimestamp(rawFile, metadata);
      fileToFunction.put(rawFile,
          new StandardCompoundNormalizationFunction(rawFile, acquisitionTimestamp,
              normalizationType, MZvsRTBalance, referencePoints));
    }
    return fileToFunction;
  }

  private @NotNull List<StandardCompoundReferencePoint> createReferencePoints(
      @NotNull final RawDataFile rawFile) {
    final List<StandardCompoundReferencePoint> referencePoints = new ArrayList<>(
        standardRows.length);
    for (final FeatureListRow standardRow : standardRows) {
      final Double standardMz = standardRow.getAverageMZ();
      if (standardMz == null) {
        throw new IllegalStateException(
            "No average m/z found for standard row: " + standardRow.getID());
      }
      final Float standardRt = standardRow.getAverageRT();
      if (standardRt == null) {
        throw new IllegalStateException(
            "No average RT found for standard row: " + standardRow.getID());
      }
      final Feature standardFeature = standardRow.getFeature(rawFile);
      if (standardFeature == null) {
        // decision: keep legacy behavior for missing standards in a specific file.
        referencePoints.add(new StandardCompoundReferencePoint(standardMz, standardRt, 1.0d, true));
        continue;
      }
      final Float standardAbundance = switch (abundanceMeasure) {
        case Height, NORMALIZED_HEIGHT -> standardFeature.getHeight();
        case Area, NORMALIZED_AREA -> standardFeature.getArea();
      };
      if (standardAbundance == null) {
        throw new IllegalStateException(
            "No standard abundance found for row %d in file %s".formatted(standardRow.getID(),
                rawFile.getName()));
      }
      referencePoints.add(
          new StandardCompoundReferencePoint(standardMz, standardRt, standardAbundance, false));
    }
    return referencePoints;
  }

  private @NotNull LocalDateTime getAcquisitionTimestamp(@NotNull final RawDataFile rawFile,
      @NotNull final MetadataTable metadata) {
    final LocalDateTime startTimeStamp = rawFile.getStartTimeStamp();
    if (startTimeStamp != null) {
      return startTimeStamp;
    }

    final LocalDateTime metadataRunDate = metadata.getValue(metadata.getRunDateColumn(), rawFile);
    if (metadataRunDate != null) {
      return metadataRunDate;
    }
    throw new IllegalStateException(
        "No acquisition timestamp found for file: " + rawFile.getName());
  }

}
