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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter_gc_ei;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.align_gc.GCConsensusAlignerPostProcessor;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.FeatureSmoothingOptions;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunctions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Removes duplicate rows of the same compound from GC-EI feature lists. Duplicates are rows within
 * the retention time tolerance that pass the quantifier m/z check and have similar pseudo spectra.
 * The best row of each group is kept and may receive the features of its duplicates.
 */
public class GcEiDuplicateFilterTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(GcEiDuplicateFilterTask.class.getName());

  private final @NotNull MZmineProject project;
  private final @NotNull ModularFeatureList originalList;
  private final @NotNull OriginalFeatureListOption handleOriginal;
  private final @NotNull String suffix;
  private final @NotNull RTTolerance rtTolerance;
  private final @NotNull MZTolerance mzTolerance;
  private final @NotNull GcEiDuplicateHandling handling;
  private final @NotNull GcEiDuplicateMatcher matcher;
  private @Nullable ModularFeatureList processedList;

  public GcEiDuplicateFilterTask(@NotNull final MZmineProject project,
      @NotNull final FeatureList featureList, @Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    originalList = (ModularFeatureList) featureList;
    handleOriginal = parameters.getValue(GcEiDuplicateFilterParameters.HANDLE_ORIGINAL);
    suffix = parameters.getValue(GcEiDuplicateFilterParameters.SUFFIX);
    rtTolerance = parameters.getValue(GcEiDuplicateFilterParameters.RT_TOLERANCE);
    mzTolerance = parameters.getValue(GcEiDuplicateFilterParameters.MZ_TOLERANCE);
    handling = parameters.getValue(GcEiDuplicateFilterParameters.HANDLING);
    matcher = new GcEiDuplicateMatcher(mzTolerance,
        parameters.getValue(GcEiDuplicateFilterParameters.MZ_CHECK),
        SpectralSimilarityFunctions.createOption(
            parameters.getParameter(GcEiDuplicateFilterParameters.SIMILARITY_FUNCTION)
                .getValueWithParameters()));
  }

  @Override
  protected void process() {
    final ModularFeatureList targetList = switch (handleOriginal) {
      case PROCESS_IN_PLACE -> originalList;
      case KEEP, REMOVE ->
          FeatureListUtils.createCopy(originalList, suffix, getMemoryMapStorage(), true);
    };
    processedList = targetList;

    final List<GcEiDuplicateCandidate> candidates = new ArrayList<>();
    for (final FeatureListRow row : targetList.getRows()) {
      final GcEiDuplicateCandidate candidate = GcEiDuplicateCandidate.fromRow(row, mzTolerance);
      if (candidate != null) {
        candidates.add(candidate);
      }
    }
    final int skipped = targetList.getNumberOfRows() - candidates.size();
    if (skipped > 0) {
      logger.warning(() -> """
          %d rows of %s have no pseudo spectrum and were not checked for duplicates. \
          Run the GC-EI spectral deconvolution before alignment.""".formatted(skipped,
          originalList.getName()));
    }

    final List<GcEiDuplicateCandidate> byRt = candidates.stream()
        .sorted(Comparator.comparingDouble(GcEiDuplicateCandidate::rt)).toList();
    final List<GcEiDuplicateCandidate> bestFirst = candidates.stream()
        .sorted(GcEiDuplicateCandidate.BEST_FIRST).toList();
    totalItems = bestFirst.size();

    // rows are identified by identity, as rows of different lists may share IDs
    final Set<FeatureListRow> duplicates = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<FeatureListRow> processed = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<FeatureListRow> mergedRows = Collections.newSetFromMap(new IdentityHashMap<>());
    int mergedFeatures = 0;

    // assumption: GC-EI workflows smooth at most once, usually before resolving
    final FeatureListAppliedMethod smoothing = ParameterUtils.getLatestModuleCall(
        originalList.getAppliedMethods(), SmoothingModule.class);
    final SmoothingAlgorithm smoother =
        smoothing != null ? FeatureSmoothingOptions.createSmoother(smoothing.getParameters())
            : null;

    // decision: greedy from the best row, so that duplicates are never chained over several rows
    for (final GcEiDuplicateCandidate best : bestFirst) {
      if (isCanceled()) {
        return;
      }
      incrementFinishedItems();
      if (duplicates.contains(best.row())) {
        continue;
      }
      processed.add(best.row());

      final List<GcEiDuplicateCandidate> withinRt = BinarySearch.indexRange(
          rtTolerance.getToleranceRange(best.rt()), byRt, GcEiDuplicateCandidate::rt).sublist(byRt);
      for (final GcEiDuplicateCandidate other : withinRt) {
        // better rows were already compared to this row
        if (processed.contains(other.row()) || duplicates.contains(other.row())
            || !matcher.isDuplicate(best, other)) {
          continue;
        }
        duplicates.add(other.row());
        final int merged = switch (handling) {
          case MERGE -> mergeInto(targetList, best, other, smoother);
          case REMOVE -> 0;
        };
        if (merged > 0) {
          mergedFeatures += merged;
          mergedRows.add(best.row());
        }
      }
    }

    // row bindings were not updated when features were merged
    for (final FeatureListRow row : mergedRows) {
      targetList.applyRowBindings(row);
    }
    targetList.removeRows(new ArrayList<>(duplicates));
    handleOriginal.reflectNewFeatureListToProject(suffix, project, targetList, originalList);

    final int mergedTotal = mergedFeatures;
    logger.info(
        () -> "GC-EI duplicate filter on %s removed %d duplicate rows and merged %d features".formatted(
            originalList.getName(), duplicates.size(), mergedTotal));
  }

  /**
   * Transfers features of the duplicate to the best row if the best row has no feature or only an
   * estimated (gap-filled) feature in this sample. Features with a different m/z are re-extracted
   * at the quantifier m/z of the best row, so that all features of a row are quantified on the
   * same m/z. Re-extracted features are smoothed like the other features of the row.
   *
   * @param smoother the smoother of the processing history or null if there was no smoothing
   * @return the number of transferred features
   */
  private int mergeInto(@NotNull final ModularFeatureList flist,
      @NotNull final GcEiDuplicateCandidate best, @NotNull final GcEiDuplicateCandidate duplicate,
      @Nullable final SmoothingAlgorithm smoother) {
    int merged = 0;
    for (final ModularFeature duplicateFeature : duplicate.row().getFeatures()) {
      final RawDataFile file = duplicateFeature.getRawDataFile();
      final Feature bestFeature = best.row().getFeature(file);
      if (statusRank(duplicateFeature.getFeatureStatus()) <= statusRank(
          bestFeature != null ? bestFeature.getFeatureStatus() : FeatureStatus.UNKNOWN)) {
        continue;
      }

      final ModularFeature newFeature;
      if (mzTolerance.checkWithinTolerance(duplicateFeature.getMZ(), best.quantifierMz())) {
        newFeature = new ModularFeature(flist, duplicateFeature);
      } else {
        newFeature = GCConsensusAlignerPostProcessor.extractNewFeature(flist, duplicateFeature,
            mzTolerance.getToleranceRange(best.quantifierMz()));
        // no signal at the quantifier m/z of the best row
        if (newFeature == null) {
          continue;
        }
        smooth(flist, newFeature, smoother);
      }
      best.row().addFeature(file, newFeature, false);
      merged++;
    }
    return merged;
  }

  /**
   * The re-extraction of the GC aligner returns raw intensities, while detected features were
   * smoothed before resolving. Applies the smoothing of the processing history to the feature
   * data.
   *
   * @param flist    the feature list that stores the smoothed data
   * @param feature  the re-extracted feature, including zero intensities
   * @param smoother the smoother or null if there was no smoothing
   */
  private static void smooth(@NotNull final ModularFeatureList flist,
      @NotNull final ModularFeature feature, @Nullable final SmoothingAlgorithm smoother) {
    if (smoother == null) {
      return;
    }
    final IonTimeSeries<? extends Scan> data = feature.getFeatureData();
    // null if retention time smoothing is disabled
    final double[] smoothed = smoother.smoothRt(data);
    if (smoothed == null) {
      return;
    }
    feature.set(FeatureDataType.class, data.copyAndReplace(flist.getMemoryMapStorage(), smoothed));
    FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);
  }

  /**
   * @return higher values for more reliable features
   */
  private static int statusRank(@NotNull final FeatureStatus status) {
    return switch (status) {
      case DETECTED, MANUAL -> 2;
      case ESTIMATED -> 1;
      case UNKNOWN, COMPOUND_AGGREGATED -> 0;
    };
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return processedList == null ? List.of() : List.of(processedList);
  }

  @Override
  public String getTaskDescription() {
    return "GC-EI duplicate filter on " + originalList.getName();
  }
}
