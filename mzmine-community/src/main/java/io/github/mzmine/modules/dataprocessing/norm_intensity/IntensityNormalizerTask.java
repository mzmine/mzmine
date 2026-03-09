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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils.InterpolationWeights;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class IntensityNormalizerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IntensityNormalizerTask.class.getName());

  private final OriginalFeatureListOption handleOriginal;

  private final MZmineProject project;
  private final ModularFeatureList originalFeatureList;
  private ModularFeatureList normalizedFeatureList;

  private final long totalRows;
  private long processedRows;

  private final String suffix;
  private final NormalizationType normalizationType;
  private final NormalizationTypeModule normalizationTypeModule;
  private final ParameterSet normalizationTypeModuleParameters;
  private final ParameterSet mainParameters;

  public IntensityNormalizerTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate); // no new data stored -> null

    this.project = project;
    this.originalFeatureList = (ModularFeatureList) featureList;
    this.mainParameters = parameters;

    suffix = parameters.getParameter(IntensityNormalizerParameters.suffix).getValue();
    final ValueWithParameters<NormalizationType> normalizationTypeWithParameters = parameters.getParameter(
        IntensityNormalizerParameters.normalizationType).getValueWithParameters();
    normalizationType = normalizationTypeWithParameters.value();
    normalizationTypeModule = normalizationType.getModuleInstance();
    normalizationTypeModuleParameters = normalizationTypeWithParameters.parameters();
    handleOriginal = parameters.getParameter(IntensityNormalizerParameters.handleOriginal)
        .getValue();
    totalRows = originalFeatureList.getNumberOfRows();
  }

  public double getFinishedPercentage() {
    return (double) processedRows / (double) totalRows;
  }

  public String getTaskDescription() {
    return "Intensity normalization of " + originalFeatureList + " by " + normalizationType;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running Intensity normalizer");

    // Create new feature list
    normalizedFeatureList = new ModularFeatureList(originalFeatureList + " " + suffix,
        getMemoryMapStorage(), originalFeatureList.getRawDataFiles());
    // do not transfer types add them later
    FeatureListUtils.transferMetadata(originalFeatureList, normalizedFeatureList, true);

    final List<RawDataFile> referenceFiles = normalizationTypeModule.getReferenceSamples(
        normalizedFeatureList, normalizationTypeModuleParameters);
    if (referenceFiles.isEmpty()) {
      error("No reference files found for normalization. %s: %s".formatted(
          normalizationTypeModule.getName(), normalizationTypeModuleParameters.toString()));
      return;
    }

    final MetadataTable metadata = ProjectService.getMetadata();
    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> fileToFunction = normalizationTypeModule.createReferenceFunctions(
        referenceFiles, originalFeatureList, metadata, mainParameters,
        normalizationTypeModuleParameters);

    final NormalizedAreaType normAreaType = DataTypes.get(NormalizedAreaType.class);
    final NormalizedHeightType normHeightType = DataTypes.get(NormalizedHeightType.class);
    normalizedFeatureList.addRowType(normHeightType);
    normalizedFeatureList.addRowType(normAreaType);
    for (final RawDataFile fileToInterpolate : originalFeatureList.getRawDataFiles()) {
      if (isCanceled()) {
        return;
      }
      if (fileToFunction.containsKey(fileToInterpolate)) {
        continue;
      }

      final InterpolationWeights result = MetadataTableUtils.extractAcquisitionDateInterpolationWeights(
          fileToInterpolate, referenceFiles, metadata);
      final NormalizationFunction previousFunction = fileToFunction.get(result.previousRun());
      final NormalizationFunction nextFunction = fileToFunction.get(result.nextRun());
      if (previousFunction == null || nextFunction == null) {
        throw new IllegalStateException("No reference normalization functions available for file: "
            + fileToInterpolate.getName());
      }
      final NormalizationFunction interpolatedFunction = normalizationTypeModule.createInterpolatedFunction(
          fileToInterpolate, previousFunction, nextFunction, result, metadata, mainParameters,
          normalizationTypeModuleParameters);
      fileToFunction.put(fileToInterpolate, interpolatedFunction);
    }

    for (final FeatureListRow originalRow : originalFeatureList.getRowsCopy()) {
      if (isCanceled()) {
        return;
      }
      // when we copy features here and set the new height/area directly, we get the best progress
      // estimate and no hiccups for large feature tables
      final ModularFeatureListRow newRow = new ModularFeatureListRow(normalizedFeatureList,
          (ModularFeatureListRow) originalRow, true);
      normalizedFeatureList.addRow(newRow);

      for (ModularFeature feature : newRow.getFeatures()) {
        final RawDataFile file = feature.getRawDataFile();
        final NormalizationFunction normalizationFunction = fileToFunction.get(file);
        if (normalizationFunction == null) {
          throw new IllegalStateException(
              "No normalization function available for file: " + file.getName());
        }
        final Double mz = feature.getMZ();
        if (mz == null) {
          throw new IllegalStateException("No m/z found for feature in file: " + file.getName());
        }
        final Float rt = feature.getRT();
        if (rt == null) {
          throw new IllegalStateException("No RT found for feature in file: " + file.getName());
        }
        final double normFactor = normalizationFunction.getNormalizationFactor(mz, rt);
        feature.set(normHeightType, (float) (feature.getHeight() * normFactor));
        feature.set(normAreaType, (float) (feature.getArea() * normFactor));
      }
      processedRows++;
    }

    if (isCanceled()) {
      return;
    }
    // Add new feature list to the project
    handleOriginal.reflectNewFeatureListToProject(suffix, project, normalizedFeatureList,
        originalFeatureList);

    final List<NormalizationFunction> finalNormalizationFunctions = new ArrayList<>(
        originalFeatureList.getNumberOfRawDataFiles());
    for (final RawDataFile rawDataFile : originalFeatureList.getRawDataFiles()) {
      final NormalizationFunction normalizationFunction = fileToFunction.get(rawDataFile);
      if (normalizationFunction == null) {
        throw new IllegalStateException(
            "No normalization function available for file: " + rawDataFile.getName());
      }
      finalNormalizationFunctions.add(normalizationFunction);
    }

    final ParameterSet appliedMethodParameters = mainParameters.cloneParameterSet(true);
    appliedMethodParameters.setParameter(IntensityNormalizerParameters.normalizationFunctions,
        List.copyOf(finalNormalizationFunctions));

    // Add task description to feature List
    normalizedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Intensity normalization by " + normalizationType,
            IntensityNormalizerModule.class, appliedMethodParameters, getModuleCallDate()));

    logger.info("Finished linear normalizer");
    setStatus(TaskStatus.FINISHED);

  }
}
