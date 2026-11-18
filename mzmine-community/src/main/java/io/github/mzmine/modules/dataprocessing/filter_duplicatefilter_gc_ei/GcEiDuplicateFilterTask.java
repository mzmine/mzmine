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
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowUtils;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
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
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Removes duplicate rows of the same compound from GC-EI feature lists. Duplicates are rows within
 * the retention time tolerance that pass the quantifier m/z check and have similar pseudo spectra.
 * The best row of each group is kept and may receive the features of its duplicates. With a
 * compound list, the representative rows of the compounds are compared and duplicate compounds are
 * merged into or removed with all their member rows.
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
    if (targetList != originalList) {
      // removed rows are detached from the compound list, so it stays valid
      FeatureListUtils.copyCompoundListByRowId(originalList, targetList, getMemoryMapStorage());
    }
    final CompoundList compoundList = getUpToDateCompoundList(targetList);

    // with a compound list, only the representative rows hold pseudo spectra and stand for their
    // compound. Rows are identified by identity, as rows of different lists may share IDs
    final Map<FeatureListRow, ModularCompoundRow> compoundOfRepresentative = new IdentityHashMap<>();
    final List<FeatureListRow> rowsToCheck;
    if (compoundList != null) {
      rowsToCheck = new ArrayList<>(compoundList.getRows().size());
      for (final ModularCompoundRow compound : compoundList.getRows()) {
        rowsToCheck.add(compound.getPreferredRow());
        compoundOfRepresentative.put(compound.getPreferredRow(), compound);
      }
    } else {
      rowsToCheck = targetList.getRows();
    }

    final List<GcEiDuplicateCandidate> candidates = new ArrayList<>();
    for (final FeatureListRow row : rowsToCheck) {
      final GcEiDuplicateCandidate candidate = GcEiDuplicateCandidate.fromRow(row, mzTolerance);
      if (candidate != null) {
        candidates.add(candidate);
      }
    }
    final int skipped = rowsToCheck.size() - candidates.size();
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

    final Set<FeatureListRow> duplicates = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<FeatureListRow> processed = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<FeatureListRow> rowsToRemove = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<FeatureListRow> mergedRows = Collections.newSetFromMap(new IdentityHashMap<>());
    // kept compound -> duplicate compounds, merged after the loop
    final Map<ModularCompoundRow, List<ModularCompoundRow>> compoundMerges = new IdentityHashMap<>();
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
        rowsToRemove.add(other.row());
        final ModularCompoundRow bestCompound = compoundOfRepresentative.get(best.row());
        final ModularCompoundRow otherCompound = compoundOfRepresentative.get(other.row());
        switch (handling) {
          case MERGE -> {
            if (bestCompound != null && otherCompound != null) {
              // members first, so that ions measured in both compounds are copied, not re-extracted
              mergedFeatures += mergeMembers(targetList, bestCompound, otherCompound, other.row(),
                  smoother, mergedRows, rowsToRemove);
              compoundMerges.computeIfAbsent(bestCompound, _ -> new ArrayList<>())
                  .add(otherCompound);
            }
            final int merged = mergeInto(targetList, best.row(), best.quantifierMz(), other.row(),
                smoother);
            if (merged > 0) {
              mergedFeatures += merged;
              mergedRows.add(best.row());
            }
          }
          case REMOVE -> {
            if (bestCompound != null && otherCompound != null) {
              final List<FeatureListRow> bestMembers = bestCompound.getMemberRows();
              // rows shared with the kept compound stay
              otherCompound.getMemberRows().stream().filter(row -> !bestMembers.contains(row))
                  .forEach(rowsToRemove::add);
            }
          }
        }
      }
    }

    // row bindings were not updated when features were merged
    for (final FeatureListRow row : mergedRows) {
      targetList.applyRowBindings(row);
    }
    if (compoundList != null) {
      // members without a counterpart in the kept compound move into the kept compound
      for (final Map.Entry<ModularCompoundRow, List<ModularCompoundRow>> merge : compoundMerges.entrySet()) {
        CompoundRowUtils.mergeCompoundRows(compoundList, merge.getKey(), merge.getValue(),
            List.of());
      }
    }
    // also detaches the removed rows from the compound list
    targetList.removeRows(new ArrayList<>(rowsToRemove));
    handleOriginal.reflectNewFeatureListToProject(suffix, project, targetList, originalList);

    final int mergedTotal = mergedFeatures;
    logger.info(() -> """
        GC-EI duplicate filter on %s found %d duplicates, removed %d rows and merged %d features""".formatted(
        originalList.getName(), duplicates.size(), rowsToRemove.size(), mergedTotal));
  }

  /**
   * @return the compound list or null if there is none or it is outdated
   */
  private static @Nullable CompoundList getUpToDateCompoundList(
      @NotNull final ModularFeatureList flist) {
    final CompoundList compoundList = flist.getCompoundList();
    if (compoundList == null) {
      return null;
    }
    if (compoundList.isStale()) {
      // decision: a stale compound list may miss rows, so all rows are checked
      logger.warning(() -> """
          The compound list of %s is outdated, rows were added or removed after compound grouping. \
          Checking all rows for duplicates instead of the compounds.""".formatted(flist.getName()));
      return null;
    }
    return compoundList;
  }

  /**
   * Merges each member row of the duplicate compound into the member row of the kept compound with
   * the closest m/z. Merged member rows are removed. Members without a counterpart are kept and
   * later moved into the kept compound.
   *
   * @param duplicateRepresentative merged separately on the quantifier m/z of the kept compound
   * @param mergedRows              receives the member rows that received features
   * @param rowsToRemove            receives the merged member rows of the duplicate compound
   * @return the number of transferred features
   */
  private int mergeMembers(@NotNull final ModularFeatureList flist,
      @NotNull final ModularCompoundRow bestCompound,
      @NotNull final ModularCompoundRow duplicateCompound,
      @NotNull final FeatureListRow duplicateRepresentative,
      @Nullable final SmoothingAlgorithm smoother, @NotNull final Set<FeatureListRow> mergedRows,
      @NotNull final Set<FeatureListRow> rowsToRemove) {
    final List<FeatureListRow> bestMembers = bestCompound.getMemberRows();
    int merged = 0;
    for (final FeatureListRow member : duplicateCompound.getMemberRows()) {
      // rows shared with the kept compound stay as they are
      if (member == duplicateRepresentative || bestMembers.contains(member)) {
        continue;
      }
      final double mz = member.getAverageMZ();
      final FeatureListRow target = bestMembers.stream()
          .filter(row -> mzTolerance.checkWithinTolerance(row.getAverageMZ(), mz))
          .min(Comparator.comparingDouble(row -> Math.abs(row.getAverageMZ() - mz))).orElse(null);
      if (target == null) {
        continue;
      }
      final int transferred = mergeInto(flist, target, target.getAverageMZ(), member, smoother);
      if (transferred > 0) {
        merged += transferred;
        mergedRows.add(target);
      }
      rowsToRemove.add(member);
    }
    return merged;
  }

  /**
   * Transfers features of the duplicate to the target row if the target row has no feature or only
   * an estimated (gap-filled) feature in this sample. Features with a different m/z are re-extracted
   * at the m/z of the target row, so that all features of a row are quantified on the same m/z.
   * Re-extracted features are smoothed like the other features of the row.
   *
   * @param targetMz the quantifier m/z of the target row
   * @param smoother the smoother of the processing history or null if there was no smoothing
   * @return the number of transferred features
   */
  private int mergeInto(@NotNull final ModularFeatureList flist,
      @NotNull final FeatureListRow target, final double targetMz,
      @NotNull final FeatureListRow duplicate, @Nullable final SmoothingAlgorithm smoother) {
    int merged = 0;
    for (final ModularFeature duplicateFeature : duplicate.getFeatures()) {
      final RawDataFile file = duplicateFeature.getRawDataFile();
      final Feature targetFeature = target.getFeature(file);
      if (statusRank(duplicateFeature.getFeatureStatus()) <= statusRank(
          targetFeature != null ? targetFeature.getFeatureStatus() : FeatureStatus.UNKNOWN)) {
        continue;
      }

      final ModularFeature newFeature;
      if (mzTolerance.checkWithinTolerance(duplicateFeature.getMZ(), targetMz)) {
        newFeature = new ModularFeature(flist, duplicateFeature);
      } else {
        newFeature = GCConsensusAlignerPostProcessor.extractNewFeature(flist, duplicateFeature,
            mzTolerance.getToleranceRange(targetMz));
        // no signal at the m/z of the target row
        if (newFeature == null) {
          continue;
        }
        smooth(flist, newFeature, smoother);
      }
      target.addFeature(file, newFeature, false);
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
