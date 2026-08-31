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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.combowithinput.StandardCompoundNormalizationRequirement;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.CSVParsingUtils.CompoundDbLoadResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal standards normalization
 */
public class StandardCompoundNormalizationTypeModule extends NormalizationTypeWithReferencesModule {

  @Override
  public @NotNull String getName() {
    return NormalizationType.StandardCompounds.toString();
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return StandardCompoundNormalizationTypeParameters.class;
  }

  @NotNull
  public List<RawDataFile> getReferenceSamples(@NotNull final FeatureList flist,
      @NotNull SamplesBatch samplesBatch,
      @NotNull final ParameterSet normalizationModuleParameters) {
    return NormalizationFunctionUtils.getReferenceSamplesOrThrow(false, samplesBatch,
        normalizationModuleParameters.getValue(
            StandardCompoundNormalizationTypeParameters.sampleTypes));
  }

  @Override
  public @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull final MetadataTable metadata, @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {
    final StandardUsageType standardUsageType = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.standardUsageType);
    final Double mzVsRtBalance = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.mzVsRtBalance);
    final MZTolerance mzTolerance = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.mzTolerance);
    final RTTolerance rtTolerance = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.rtTolerance);
    final List<ImportType<?>> standardImportTypes = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.standardCompounds);
    final MobilityTolerance mobilityTolerance =
        ImportType.isDataTypeSelectedInImportTypes(standardImportTypes, MobilityType.class)
            ? moduleSpecificParameters.getValue(
            StandardCompoundNormalizationTypeParameters.mobilityTolerance) : null;
    final AbundanceMeasure abundanceMeasure = mainParameters.getValue(
        IntensityNormalizerParameters.featureMeasurementType);
    final StandardCompoundNormalizationRequirement requirement = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.requirement);

    final List<CompoundDBAnnotation> standardAnnotations = loadStandardAnnotations(
        moduleSpecificParameters);
    final List<StandardCompoundMatch> standardMatches = findBestStandardMatches(summary,
        featureList, standardAnnotations, mzTolerance, rtTolerance, mobilityTolerance,
        referenceFiles, abundanceMeasure);
    if (standardMatches.isEmpty()) {
      throw new IllegalStateException("No internal standard compounds matched the feature list.");
    }

    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> fileToFunction = new HashMap<>();
    for (final RawDataFile rawFile : referenceFiles) {
      final List<StandardCompoundReferencePoint> referencePoints = createReferencePoints(summary,
          rawFile, standardMatches, abundanceMeasure, requirement);
      if (referencePoints.isEmpty()) {
        // only reachable in SKIP_FILES_WITHOUT_STANDARD mode. Leaving the file out of the result
        // means it is normalized by interpolation between the neighboring reference samples
        continue;
      }
      NormalizationFunction function = new StandardCompoundNormalizationFunction(standardUsageType,
          mzVsRtBalance, referencePoints);

      // add or merge function into a new instance within summary
      summary.addMergeFunction(rawFile, function);

      // return the actual function of this step for interpolation
      fileToFunction.put(rawFile, function);
    }
    if (fileToFunction.isEmpty()) {
      throw new IllegalStateException(
          "No internal standard was detected in any of the reference samples.");
    }
    return fileToFunction;
  }

  private @NotNull List<CompoundDBAnnotation> loadStandardAnnotations(
      @NotNull final ParameterSet moduleSpecificParameters) {
    final CompoundDbLoadResult compoundResult = CSVParsingUtils.getAnnotationsFromCsvFile(
        moduleSpecificParameters.getValue(
            StandardCompoundNormalizationTypeParameters.standardCompoundsFile),
        moduleSpecificParameters.getValue(
            StandardCompoundNormalizationTypeParameters.fieldSeparator),
        moduleSpecificParameters.getValue(
            StandardCompoundNormalizationTypeParameters.standardCompounds), null);

    if (compoundResult.status() == TaskStatus.ERROR) {
      throw new IllegalStateException(compoundResult.errorMessage());
    }

    for (final CompoundDBAnnotation annotation : compoundResult.annotations()) {
      if (annotation.getRT() == null) {
        throw new IllegalStateException(
            "Standard compound annotation is missing an RT value: " + annotation);
      }
    }

    return compoundResult.annotations();
  }

  private @NotNull List<StandardCompoundMatch> findBestStandardMatches(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final ModularFeatureList featureList,
      @NotNull final List<CompoundDBAnnotation> standardAnnotations,
      @NotNull final MZTolerance mzTolerance, @NotNull final RTTolerance rtTolerance,
      @Nullable final MobilityTolerance mobilityTolerance,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final AbundanceMeasure abundanceMeasure) {
    final List<StandardCompoundMatch> standardMatches = new ArrayList<>(standardAnnotations.size());

    for (final CompoundDBAnnotation standardAnnotation : standardAnnotations) {
      final StandardCompoundMatch bestMatch = findBestStandardMatch(summary, featureList,
          standardAnnotation, mzTolerance, rtTolerance, mobilityTolerance, referenceFiles,
          abundanceMeasure);
      if (bestMatch != null) {
        bestMatch.row().addCompoundAnnotation(bestMatch.annotation());
        standardMatches.add(bestMatch);
      }
    }

    return standardMatches;
  }

  /**
   * Selects the best feature list row for a standard. The user defined m/z, RT, and mobility may
   * deviate from the measured values, so the deviation is only used as a filter (all candidates are
   * within tolerances) and never as the primary ranking. A standard is only useful if it is
   * actually detected in the reference samples, therefore candidates are ranked by
   * <ol>
   *   <li>the number of reference samples with a usable abundance (detection rate),</li>
   *   <li>the summed abundance over those reference samples (tie breaker),</li>
   *   <li>the annotation score and finally the row ID to stay deterministic.</li>
   * </ol>
   */
  private @Nullable StandardCompoundMatch findBestStandardMatch(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final ModularFeatureList featureList,
      @NotNull final CompoundDBAnnotation standardAnnotation,
      @NotNull final MZTolerance mzTolerance, @NotNull final RTTolerance rtTolerance,
      @Nullable final MobilityTolerance mobilityTolerance,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final AbundanceMeasure abundanceMeasure) {
    StandardCompoundMatch bestMatch = null;

    for (final FeatureListRow row : featureList.getRows()) {
      final CompoundDBAnnotation matchedAnnotation = standardAnnotation.checkMatchAndCalculateDeviation(
          row, mzTolerance, rtTolerance, mobilityTolerance, null, null);
      if (matchedAnnotation == null) {
        continue;
      }

      final StandardCompoundMatch candidate = createMatch(summary, row, matchedAnnotation,
          referenceFiles, abundanceMeasure);
      if (bestMatch == null || candidate.isBetterThan(bestMatch)) {
        bestMatch = candidate;
      }
    }

    return bestMatch;
  }

  /**
   * Counts in how many reference samples the row is detected with a usable abundance and sums those
   * abundances. Already applied normalization functions are taken into account, just like in
   * {@link #createReferencePoints}.
   */
  private @NotNull StandardCompoundMatch createMatch(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final FeatureListRow row, @NotNull final CompoundDBAnnotation annotation,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final AbundanceMeasure abundanceMeasure) {
    int detectedInReferences = 0;
    int estimatedFeatures = 0;
    double summedAbundance = 0d;

    for (final RawDataFile referenceFile : referenceFiles) {
      final ModularFeature feature = (ModularFeature) row.getFeature(referenceFile);
      if (feature == null) {
        continue;
      }
      final float abundance = abundanceMeasure.getOrNaN(feature,
          summary.functions().get(referenceFile));
      if (!Float.isFinite(abundance) || abundance <= 0f) {
        continue;
      }
      detectedInReferences++;
      summedAbundance += abundance;
      if (feature.getFeatureStatus() == FeatureStatus.ESTIMATED) {
        estimatedFeatures++;
      }
    }

    return new StandardCompoundMatch(row, annotation, detectedInReferences, summedAbundance,
        estimatedFeatures);
  }

  /**
   * @return the reference points of this file. May be empty in
   * {@link StandardCompoundNormalizationMode#SKIP_FILES_WITHOUT_STANDARD} mode, then the caller
   * skips the file. The other modes throw instead.
   */
  private @NotNull List<StandardCompoundReferencePoint> createReferencePoints(
      @NotNull IntensityNormalizationSearchableSummary summary, @NotNull final RawDataFile rawFile,
      @NotNull final List<StandardCompoundMatch> standardMatches,
      @NotNull final AbundanceMeasure abundanceMeasure,
      @NotNull final StandardCompoundNormalizationRequirement requirement) {
    final boolean requireAllStandards =
        requirement.mode() == StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES;
    final List<StandardCompoundReferencePoint> referencePoints = new ArrayList<>(
        standardMatches.size());
    for (final StandardCompoundMatch standardMatch : standardMatches) {
      final FeatureListRow standardRow = standardMatch.row();
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

      // apply existing function to abundance to normalize on already normalized values
      final @Nullable RawFileNormalizationFunction existingFunction = summary.functions()
          .get(rawFile);

      final float standardAbundance = abundanceMeasure.getOrNaN(standardFeature, existingFunction);
      if (Float.compare(standardAbundance, 0.0f) == 0 || !Float.isFinite(standardAbundance)) {
        if (!requireAllStandards) {
          continue; // skip standard
        }
        throw new IllegalStateException(
            "Invalid standard abundance found for row %s in file %s: %.2E".formatted(
                standardRow.toString(), rawFile.getName(), standardAbundance));
      }
      referencePoints.add(
          new StandardCompoundReferencePoint(standardMz, standardRt, standardAbundance));
    }
    // throw exception if minimum requirements are not met
    requirement.assertMinReferencePoints(standardMatches.size(), referencePoints.size(), rawFile);
    return referencePoints;
  }

  /**
   * A standard annotation matched to a feature list row, together with how well that row is covered
   * by the reference samples. See {@link #findBestStandardMatch}.
   *
   * @param detectedInReferences number of reference samples with a usable abundance (detected and
   *                             estimated)
   * @param summedAbundance      sum of those abundances
   * @param estimatedFeatures    number of reference samples with {@link FeatureStatus#ESTIMATED}
   *                             (gap-filled)
   */
  private record StandardCompoundMatch(@NotNull FeatureListRow row,
                                       @NotNull CompoundDBAnnotation annotation,
                                       int detectedInReferences, double summedAbundance,
                                       int estimatedFeatures) {

    /**
     * Highest detection rate wins, ties are broken by the highest summed abundance, then by the
     * annotation score, and finally by the lowest row ID to stay deterministic.
     */
    boolean isBetterThan(@NotNull final StandardCompoundMatch other) {
      if (detectedInReferences != other.detectedInReferences) {
        return detectedInReferences > other.detectedInReferences;
      }
      if (estimatedFeatures != other.estimatedFeatures) {
        return estimatedFeatures < other.estimatedFeatures;
      }
      if (Double.compare(summedAbundance, other.summedAbundance) != 0) {
        return summedAbundance > other.summedAbundance;
      }
      final int scoreCompare = Float.compare(score(), other.score());
      if (scoreCompare != 0) {
        return scoreCompare > 0;
      }
      return row.getID() < other.row.getID();
    }

    private float score() {
      final Float score = annotation.getScore();
      return score == null ? Float.NEGATIVE_INFINITY : score;
    }
  }

}
