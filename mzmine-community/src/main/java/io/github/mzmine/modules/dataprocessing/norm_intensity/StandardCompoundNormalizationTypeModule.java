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
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.CSVParsingUtils.CompoundDbLoadResult;
import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal standards normalization
 */
public class StandardCompoundNormalizationTypeModule extends
    NormalizationTypeWithReferencesModule implements InternalStandardSelectingNormalizer {

  /**
   * Prefix of the informational messages this step adds to
   * {@link IntensityNormalizationSearchableSummary}.
   */
  static final String MESSAGE_PREFIX = "IS: ";

  /**
   * Prefix of messages that report a problem the user should look at, e.g. a standard that matched
   * no row or a sample that had to be normalized without any standard.
   */
  static final String WARNING_PREFIX = "IS " + IntensityNormalizationSummary.WARNING_MARKER;

  /**
   * Maximum number of detail lines per aggregated warning, so that the report stays readable for
   * hundreds of files.
   */
  private static final int MAX_DETAIL_MESSAGES = 10;

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

  /**
   * Same as
   * {@link #createAllNormalizationFunctionsToSummary(IntensityNormalizationSearchableSummary,
   * ModularFeatureList, SamplesBatch, MetadataTable, ParameterSet, ParameterSet)} but with a
   * selection of standard rows that was already resolved for the whole feature list. All batches
   * have to use the same rows and the same reference levels, otherwise the same standard could be
   * represented by a different row in each batch.
   */
  public void createAllNormalizationFunctionsToSummary(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull MetadataTable metadata, @NotNull ParameterSet mainParameters,
      @NotNull ParameterSet moduleSpecificParameters,
      @NotNull StandardCompoundSelection selection) {

    final List<RawDataFile> referenceSamples = getReferenceSamples(featureList, samplesBatch,
        moduleSpecificParameters);
    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> refFunctions = createReferenceFunctions(
        summary, referenceSamples, mainParameters, moduleSpecificParameters, selection);

    NormalizationFunctionUtils.interpolateLinearBinary(summary, samplesBatch, refFunctions,
        metadata);
  }

  /**
   * Should use the overloaded method with already applied selection of standards to use the same
   * standards for all sample batches
   */
  @Deprecated
  @Override
  public @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull final MetadataTable metadata, @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {
    throw new UnsupportedOperationException("Use the method with preselected standard selection.");
    // no precomputed selection: resolve the standards for the reference files of this batch
//    final StandardCompoundSelection selection = selectStandards(summary, featureList,
//        referenceFiles, mainParameters, moduleSpecificParameters);
//
//    return createReferenceFunctions(summary, referenceFiles, mainParameters,
//        moduleSpecificParameters, selection);
  }

  /**
   * Package private so that tests can drive the two phases directly. Production code goes through
   * {@link #createAllNormalizationFunctionsToSummary(IntensityNormalizationSearchableSummary,
   * ModularFeatureList, SamplesBatch, MetadataTable, ParameterSet, ParameterSet,
   * StandardCompoundSelection)}.
   */
  protected @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleSpecificParameters,
      @NotNull final StandardCompoundSelection selection) {
    final StandardUsageType standardUsageType = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.standardUsageType);
    final Double mzVsRtBalance = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.mzVsRtBalance);
    final AbundanceMeasure abundanceMeasure = mainParameters.getValue(
        IntensityNormalizerParameters.featureMeasurementType);
    final StandardCompoundNormalizationMode mode = moduleSpecificParameters.getValue(
        StandardCompoundNormalizationTypeParameters.mode);

    final List<String> skippedStandardMessages = new ArrayList<>();
    final List<String> skippedFiles = new ArrayList<>();

    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> fileToFunction = new HashMap<>();
    for (final RawDataFile rawFile : referenceFiles) {
      final List<StandardCompoundReferencePoint> referencePoints = createReferencePoints(summary,
          rawFile, selection.matches(), abundanceMeasure, mode, skippedStandardMessages);
      if (referencePoints.isEmpty()) {
        // only reachable in SKIP_FILES_WITHOUT_STANDARD mode. Leaving the file out of the result
        // means it is normalized by interpolation between the neighboring reference samples
        skippedFiles.add(rawFile.getName());
        continue;
      }
      NormalizationFunction function = new StandardCompoundNormalizationFunction(standardUsageType,
          mzVsRtBalance, StandardCompoundFactorMode.getDefault(), referencePoints);

      // add or merge function into a new instance within summary
      summary.addMergeFunction(rawFile, function);

      // return the actual function of this step for interpolation
      fileToFunction.put(rawFile, function);
    }
    addAggregatedMessages(summary, skippedStandardMessages,
        "%d standards were skipped in individual reference samples".formatted(
            skippedStandardMessages.size()));
    addAggregatedMessages(summary, skippedFiles.stream().map(
                "no usable standard in reference sample %s, normalized by interpolation between the neighboring reference samples"::formatted)
            .toList(),
        "%d reference samples without any usable standard".formatted(skippedFiles.size()));

    if (fileToFunction.isEmpty()) {
      throw new IllegalStateException(
          "No internal standard was detected in any of the reference samples.");
    }

    return fileToFunction;
  }

  /**
   * Resolves the standard compounds file to feature list rows and determines the reference level of
   * each standard. Call this once for the whole feature list, before the samples are split into
   * batches, so that all batches use the same rows and the same reference levels.
   *
   * @param referenceFiles all reference samples of the whole feature list, not of a single batch
   */
  @Override
  public @NotNull StandardCompoundSelection selectStandards(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final ModularFeatureList featureList,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {
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

    Set<RawDataFile> referenceFileSet = Set.copyOf(referenceFiles);
    final List<RawDataFile> otherFiles = featureList.getRawDataFiles().stream()
        .filter(file -> !referenceFileSet.contains(file)).toList();
    summary.messages().add(MESSAGE_PREFIX
        + "%d of %d files are reference samples that define the standard levels, %d other files are normalized by interpolation between the neighboring reference samples.".formatted(
        referenceFiles.size(), featureList.getNumberOfRawDataFiles(), otherFiles.size()));

    final List<CompoundDBAnnotation> standardAnnotations = loadStandardAnnotations(
        moduleSpecificParameters);
    final List<StandardCompoundMatch> standardMatches = findBestStandardMatches(summary,
        featureList, standardAnnotations, mzTolerance, rtTolerance, mobilityTolerance,
        referenceFiles, otherFiles, abundanceMeasure);
    if (standardMatches.isEmpty()) {
      throw new IllegalStateException("No internal standard compounds matched the feature list.");
    }

    return new StandardCompoundSelection(standardMatches);
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
      @NotNull final List<@NotNull RawDataFile> otherFiles,
      @NotNull final AbundanceMeasure abundanceMeasure) {
    final List<StandardCompoundMatch> standardMatches = new ArrayList<>(standardAnnotations.size());
    final Map<Integer, List<String>> standardsByRowId = new LinkedHashMap<>();

    for (final CompoundDBAnnotation standardAnnotation : standardAnnotations) {
      final StandardCompoundMatch bestMatch = findBestStandardMatch(summary, featureList,
          standardAnnotation, mzTolerance, rtTolerance, mobilityTolerance, referenceFiles,
          otherFiles, abundanceMeasure);
      if (bestMatch == null) {
        summary.messages().add(
            WARNING_PREFIX + "no feature list row matched standard %s.".formatted(
                describe(standardAnnotation)));
        continue;
      }
      bestMatch.row().addCompoundAnnotation(bestMatch.annotation());
      standardMatches.add(bestMatch);
      standardsByRowId.computeIfAbsent(bestMatch.row().getID(), _ -> new ArrayList<>())
          .add(describe(standardAnnotation));

      summary.messages().add(MESSAGE_PREFIX + bestMatch.describeSelection());
      if (bestMatch.detectedInReferences() == 1) {
        summary.messages().add(WARNING_PREFIX
            + "standard %s is detected in only one reference sample and therefore contributes no correction.".formatted(
            describe(standardAnnotation)));
      }
    }

    // two standards resolving to the same row contribute the same correction multiple times.
    // m/z and RT deviations are only used as a filter here, because the values in the standards
    // file may be imprecise, so this is reported instead of resolved automatically.
    standardsByRowId.forEach((rowId, standards) -> {
      if (standards.size() > 1) {
        summary.messages().add(WARNING_PREFIX
            + "standards %s all matched the same row %d and contribute the same correction multiple times.".formatted(
            String.join(", ", standards), rowId));
      }
    });

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
      @NotNull final List<@NotNull RawDataFile> otherFiles,
      @NotNull final AbundanceMeasure abundanceMeasure) {
    StandardCompoundMatch bestMatch = null;

    for (final FeatureListRow row : featureList.getRows()) {
      final CompoundDBAnnotation matchedAnnotation = standardAnnotation.checkMatchAndCalculateDeviation(
          row, mzTolerance, rtTolerance, mobilityTolerance, null, null);
      if (matchedAnnotation == null) {
        continue;
      }

      final StandardCompoundMatch candidate = createMatch(summary, row, matchedAnnotation,
          referenceFiles, otherFiles, abundanceMeasure);
      if (bestMatch == null || candidate.isBetterThan(bestMatch)) {
        bestMatch = candidate;
      }
    }

    return bestMatch;
  }

  /**
   * Collects the abundance of the row in every reference sample and counts the detections in the
   * remaining files. Already applied normalization functions are taken into account, just like in
   * {@link #createReferencePoints}.
   */
  private @NotNull StandardCompoundMatch createMatch(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final FeatureListRow row, @NotNull final CompoundDBAnnotation annotation,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final List<@NotNull RawDataFile> otherFiles,
      @NotNull final AbundanceMeasure abundanceMeasure) {
    final Map<RawDataFile, Double> referenceAbundances = new LinkedHashMap<>();
    for (final RawDataFile referenceFile : referenceFiles) {
      final double abundance = getUsableAbundance(summary, row, referenceFile, abundanceMeasure);
      if (!Double.isNaN(abundance)) {
        referenceAbundances.put(referenceFile, abundance);
      }
    }

    int detectedInOtherFiles = 0;
    for (final RawDataFile otherFile : otherFiles) {
      if (!Double.isNaN(getUsableAbundance(summary, row, otherFile, abundanceMeasure))) {
        detectedInOtherFiles++;
      }
    }

    return new StandardCompoundMatch(row, annotation, referenceAbundances, referenceFiles.size(),
        detectedInOtherFiles, otherFiles.size());
  }

  /**
   * @return the abundance of the row in this file after applying the already computed normalization
   * steps, or NaN if the feature is missing or the abundance cannot be used for normalization.
   */
  private static double getUsableAbundance(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final FeatureListRow row, @NotNull final RawDataFile file,
      @NotNull final AbundanceMeasure abundanceMeasure) {
    if (!(row.getFeature(file) instanceof ModularFeature feature)) {
      return Double.NaN;
    }
    final float abundance = abundanceMeasure.getOrNaN(feature, summary.functions().get(file));
    return Float.isFinite(abundance) && abundance > 0f ? abundance : Double.NaN;
  }

  /**
   * @param skippedStandardMessages collects the standards that had to be skipped for this file, so
   *                                that the caller can aggregate them into a few messages
   * @return the reference points of this file. May be empty in
   * {@link StandardCompoundNormalizationMode#SKIP_FILES_WITHOUT_STANDARD} mode, then the caller
   * skips the file. The other modes throw instead.
   */
  private @NotNull List<StandardCompoundReferencePoint> createReferencePoints(
      @NotNull IntensityNormalizationSearchableSummary summary, @NotNull final RawDataFile rawFile,
      @NotNull final List<StandardCompoundMatch> standardMatches,
      @NotNull final AbundanceMeasure abundanceMeasure,
      @NotNull final StandardCompoundNormalizationMode mode,
      @NotNull final List<String> skippedStandardMessages) {
    final boolean requireAllStandards =
        mode == StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES;
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
        skippedStandardMessages.add(
            "standard %s not detected in reference sample %s".formatted(standardMatch.describe(),
                rawFile.getName()));
        continue;
      }

      // apply existing function to abundance to normalize on already normalized values
      final @Nullable RawFileNormalizationFunction existingFunction = summary.functions()
          .get(rawFile);

      final float standardAbundance = abundanceMeasure.getOrNaN(standardFeature, existingFunction);
      // same usability rule as getUsableAbundance, otherwise a standard could contribute a
      // reference point that was not part of its own reference level
      if (!Float.isFinite(standardAbundance) || standardAbundance <= 0f) {
        if (!requireAllStandards) {
          skippedStandardMessages.add(
              "standard %s has an unusable abundance in reference sample %s".formatted(
                  standardMatch.describe(), rawFile.getName()));
          continue; // skip standard
        }
        throw new IllegalStateException(
            "Invalid standard abundance found for row %s in file %s: %.2E".formatted(
                standardRow.toString(), rawFile.getName(), standardAbundance));
      }

      // the reference level makes the factor a relative correction around 1, so that different
      // standards stay comparable even if a sample only contains a subset of them
      final double referenceAbundance = standardMatch.referenceAbundance();
      if (!Double.isFinite(referenceAbundance) || referenceAbundance <= 0d) {
        if (!requireAllStandards) {
          skippedStandardMessages.add(
              "standard %s was not detected in any reference sample".formatted(
                  standardMatch.describe()));
          continue; // skip standard
        }
        throw new IllegalStateException(
            "Standard %s was not detected in any reference sample.".formatted(
                standardMatch.describe()));
      }

      referencePoints.add(
          new StandardCompoundReferencePoint(standardMz, standardRt, standardAbundance,
              referenceAbundance));
    }
    if (referencePoints.isEmpty()
        && mode != StandardCompoundNormalizationMode.SKIP_FILES_WITHOUT_STANDARD) {
      throw new IllegalStateException(
          "No intensity normalization standards found for file: " + rawFile.getName());
    }
    return referencePoints;
  }

  /**
   * Adds up to {@link #MAX_DETAIL_MESSAGES} detail lines and replaces the rest by a single summary
   * line, so that the report stays readable for hundreds of files.
   */
  private static void addAggregatedMessages(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final List<String> details, @NotNull final String summaryLine) {
    if (details.isEmpty()) {
      return;
    }
    details.stream().limit(MAX_DETAIL_MESSAGES)
        .forEach(msg -> summary.messages().add(WARNING_PREFIX + msg));
    if (details.size() > MAX_DETAIL_MESSAGES) {
      summary.messages().add(
          WARNING_PREFIX + "%s, only the first %d are listed.".formatted(summaryLine,
              MAX_DETAIL_MESSAGES));
    }
  }

  private static @NotNull String describe(@NotNull final CompoundDBAnnotation annotation) {
    final String name = annotation.getCompoundName();
    final Double mz = annotation.getPrecursorMZ();
    final Float rt = annotation.getRT();
    return "'%s' (m/z %s, RT %s)".formatted(name == null || name.isBlank() ? "unnamed" : name,
        mz == null ? "?" : "%.4f".formatted(mz), rt == null ? "?" : "%.2f".formatted(rt));
  }

  /**
   * The standard rows that were resolved for a whole feature list. Computed once by
   * {@link #selectStandards} and then reused for every samples batch. Public so that
   * {@link IntensityNormalizerTask} can hold it between the steps, but the content is package
   * private on purpose: this is an opaque handle and not an API.
   */
  public record StandardCompoundSelection(@NotNull List<StandardCompoundMatch> matches) {

    public StandardCompoundSelection {
      matches = List.copyOf(matches);
    }
  }

  /**
   * A standard annotation matched to a feature list row, together with the abundances of that row
   * in the reference samples. See {@link #findBestStandardMatch}.
   *
   * @param referenceAbundances  usable abundances per reference sample, missing and unusable files
   *                             are not contained
   * @param detectedInOtherFiles number of non reference files with a usable abundance, only
   *                             reported and never used for normalization
   */
  record StandardCompoundMatch(@NotNull FeatureListRow row,
                               @NotNull CompoundDBAnnotation annotation,
                               @NotNull Map<RawDataFile, Double> referenceAbundances,
                               int totalReferenceFiles, int detectedInOtherFiles,
                               int totalOtherFiles) {

    StandardCompoundMatch {
      referenceAbundances = Map.copyOf(referenceAbundances);
    }

    /**
     * Highest detection rate wins, ties are broken by the highest summed abundance, then by the
     * annotation score, and finally by the lowest row ID to stay deterministic.
     */
    boolean isBetterThan(@NotNull final StandardCompoundMatch other) {
      if (detectedInReferences() != other.detectedInReferences()) {
        return detectedInReferences() > other.detectedInReferences();
      }
      final double summed = summedAbundance();
      final double otherSummed = other.summedAbundance();
      if (Double.compare(summed, otherSummed) != 0) {
        return summed > otherSummed;
      }
      final int scoreCompare = Float.compare(score(), other.score());
      if (scoreCompare != 0) {
        return scoreCompare > 0;
      }
      return row.getID() < other.row.getID();
    }

    /**
     * @return number of reference samples with a usable abundance
     */
    int detectedInReferences() {
      return referenceAbundances.size();
    }

    double summedAbundance() {
      return referenceAbundances.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * @return the level this standard is normalized to, the median over all reference samples that
     * detected it. NaN if the standard was not detected in any reference sample.
     */
    double referenceAbundance() {
      if (referenceAbundances.isEmpty()) {
        // MathUtils.calcMedian returns 0 for empty input, which would be a silently wrong level
        return Double.NaN;
      }
      return MathUtils.calcMedian(
          referenceAbundances.values().stream().mapToDouble(Double::doubleValue).toArray());
    }

    /**
     * @return the relative standard deviation of this standard over the reference samples in
     * percent, or NaN if fewer than two reference samples detected it
     */
    double cvInReferencesPercent() {
      if (referenceAbundances.size() < 2) {
        return Double.NaN;
      }
      final double[] values = referenceAbundances.values().stream().mapToDouble(Double::doubleValue)
          .toArray();
      final double avg = MathUtils.calcAvg(values);
      return Double.compare(avg, 0d) == 0 ? Double.NaN : MathUtils.calcStd(values) / avg * 100d;
    }

    @NotNull String describe() {
      return StandardCompoundNormalizationTypeModule.describe(annotation);
    }

    @NotNull String describeSelection() {
      final double cv = cvInReferencesPercent();
      final String otherFiles = totalOtherFiles == 0 ? ""
          : ", %d/%d other files".formatted(detectedInOtherFiles, totalOtherFiles);
      return "standard %s matched row %d, detected in %d/%d reference samples (CV %s)%s, reference level %.2E".formatted(
          describe(), row.getID(), detectedInReferences(), totalReferenceFiles,
          Double.isNaN(cv) ? "n/a" : "%.1f%%".formatted(cv), otherFiles, referenceAbundance());
    }

    private float score() {
      final Float score = annotation.getScore();
      return score == null ? Float.NEGATIVE_INFINITY : score;
    }
  }

}
