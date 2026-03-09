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

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils.InterpolationWeights;
import io.github.mzmine.parameters.ParameterSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class StandardCompoundNormalizationTypeModule implements NormalizationTypeModule {

  @Override
  public @NotNull String getName() {
    return "Standard compounds";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return StandardCompoundNormalizationTypeParameters.class;
  }

  @NotNull
  public List<RawDataFile> getReferenceSamples(@NotNull final FeatureList flist,
      @NotNull final ParameterSet normalizationModuleParameters) {
    final var sampleTypeFilter = new SampleTypeFilter(normalizationModuleParameters.getParameter(
        StandardCompoundNormalizationTypeParameters.sampleTypes).getValue());
    return sampleTypeFilter.filterFiles(flist.getRawDataFiles());
  }

  @Override
  public @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ModularFeatureList featureList, @NotNull final MetadataTable metadata,
      @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {
    final FeatureListRow[] standardRows = moduleSpecificParameters.getParameter(
        StandardCompoundNormalizationTypeParameters.standardCompounds).getMatchingRows(featureList);
    if (standardRows.length == 0) {
      throw new IllegalStateException("No internal standard features selected.");
    }

    final StandardUsageType standardUsageType = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.standardUsageType);
    final Double mzVsRtBalance = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.mzVsRtBalance);
    final AbundanceMeasure abundanceMeasure = mainParameters.getValue(
        IntensityNormalizerParameters.featureMeasurementType);
    final boolean requireAllStandards = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.requireAllStandards);

    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> fileToFunction = new HashMap<>();
    for (final RawDataFile rawFile : referenceFiles) {
      final List<StandardCompoundReferencePoint> referencePoints = createReferencePoints(rawFile,
          standardRows, abundanceMeasure, requireAllStandards);
      final LocalDateTime acquisitionTimestamp = getAcquisitionTimestamp(rawFile, metadata);
      fileToFunction.put(rawFile,
          new StandardCompoundNormalizationFunction(rawFile, acquisitionTimestamp,
              standardUsageType, mzVsRtBalance, referencePoints));
    }
    return fileToFunction;
  }

  @Override
  public @NotNull NormalizationFunction createInterpolatedFunction(
      @NotNull final RawDataFile fileToInterpolate,
      @NotNull final NormalizationFunction previousRunCalibration,
      @NotNull final NormalizationFunction nextRunCalibration,
      @NotNull final InterpolationWeights interpolationWeights,
      @NotNull final MetadataTable metadata, @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet normalizerParameters) {
    final LocalDateTime runDate = getAcquisitionTimestamp(fileToInterpolate, metadata);
    return new InterpolatedNormalizationFunction(fileToInterpolate, runDate, previousRunCalibration,
        interpolationWeights.previousWeight(), nextRunCalibration,
        interpolationWeights.nextRunWeight());
  }

  private @NotNull List<StandardCompoundReferencePoint> createReferencePoints(
      @NotNull final RawDataFile rawFile, @NotNull final FeatureListRow[] standardRows,
      @NotNull final AbundanceMeasure abundanceMeasure, boolean requireAllStandards) {
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

      final ModularFeature standardFeature = (ModularFeature) standardRow.getFeature(rawFile);
      if (standardFeature == null && requireAllStandards) {
        throw new RuntimeException(
            "Standard " + standardRow.toString() + " was not detected in file "
                + rawFile.getName());
      } else if (standardFeature == null) {
        continue;
      }

      final Float standardAbundance = abundanceMeasure.get(standardFeature);
      if (standardAbundance == null || Float.compare(standardAbundance, 0.0f) == 0
          || !Float.isFinite(standardAbundance)) {
        if (!requireAllStandards) {
          continue;
        }
        throw new IllegalStateException(
            "Invalid standard abundance found for row %s in file %s: %.2E".formatted(
                standardRow.toString(), rawFile.getName(), standardAbundance));
      }
      referencePoints.add(
          new StandardCompoundReferencePoint(standardMz, standardRt, standardAbundance));
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
