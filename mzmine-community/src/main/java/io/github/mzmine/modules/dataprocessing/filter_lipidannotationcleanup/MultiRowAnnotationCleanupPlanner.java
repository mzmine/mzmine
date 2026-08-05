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

package io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class that analyses a feature list and computes a {@link MultiRowAnnotationCleanupPlan}
 * based on the given {@link MultiRowAnnotationCleanupOptions}, resolving preferred ionisation types
 * and score-based removal rules per lipid class.
 */
public final class MultiRowAnnotationCleanupPlanner {

  private MultiRowAnnotationCleanupPlanner() {
  }

  /**
   * Collects lipid-class-specific available ionizations sorted by prevalence
   */
  public static @NotNull Map<String, List<IonizationType>> collectAvailableIonizationsByLipidClass(
      final @NotNull ModularFeatureList featureList) {
    final Map<String, Map<IonizationType, Integer>> countsByClass = collectClassIonizationCounts(
        featureList);
    final Map<String, List<IonizationType>> availableByClass = new LinkedHashMap<>();
    for (final Entry<String, Map<IonizationType, Integer>> entry : countsByClass.entrySet()) {
      final List<IonizationType> ionizations = entry.getValue().entrySet().stream()
          .sorted((left, right) -> {
            final int countCmp = Integer.compare(right.getValue(), left.getValue());
            if (countCmp != 0) {
              return countCmp;
            }
            return left.getKey().toString().compareTo(right.getKey().toString());
          }).map(Entry::getKey).toList();
      availableByClass.put(entry.getKey(), ionizations);
    }
    return availableByClass;
  }

  /**
   * Determines the default preferred ionization type for each lipid class based on their prevalence
   * in the provided feature list. The method scans the feature list to collect counts of ionization
   * types for each lipid class and selects the most common one as the default. If multiple
   * ionization types have the same count, ties are resolved lexicographically by ionization type
   * name.
   *
   * @param featureList the feature list containing lipid data from which ionization type counts are
   *                    collected, must not be null.
   * @return a map where the keys are lipid class names and values are the most preferred ionization
   * types for each class.
   */
  public static @NotNull Map<String, IonizationType> defaultPreferredIonizationByLipidClass(
      final @NotNull ModularFeatureList featureList) {
    final Map<String, Map<IonizationType, Integer>> countsByClass = collectClassIonizationCounts(
        featureList);
    final Map<String, IonizationType> defaults = new LinkedHashMap<>();
    for (final Entry<String, Map<IonizationType, Integer>> entry : countsByClass.entrySet()) {
      final IonizationType preferred = entry.getValue().entrySet().stream().max((left, right) -> {
        final int countCmp = Integer.compare(left.getValue(), right.getValue());
        if (countCmp != 0) {
          return countCmp;
        }
        return right.getKey().toString().compareTo(left.getKey().toString());
      }).map(Entry::getKey).orElse(null);
      if (preferred != null) {
        defaults.put(entry.getKey(), preferred);
      }
    }
    return defaults;
  }

  public static @NotNull MultiRowAnnotationCleanupPlan buildCleanupPlan(
      final @NotNull ModularFeatureList featureList, LipidAnalysisType analysisType,
      final @NotNull MultiRowAnnotationCleanupOptions options) {
    final Map<String, List<RowAnnotationCandidate>> candidatesByAnnotation = new TreeMap<>();
    final Map<FeatureListRow, Map<MatchedLipid, Double>> scoreCache = new HashMap<>();

    for (final FeatureListRow candidateRow : featureList.getRows()) {
      for (final MatchedLipid match : candidateRow.getLipidMatches()) {
        final String annotation = Objects.toString(match.getLipidAnnotation().getAnnotation(), "");
        if (annotation.isBlank()) {
          continue;
        }
        final double score = computeAnnotationScore(featureList, candidateRow, match, analysisType);
        scoreCache.computeIfAbsent(candidateRow, _ -> new HashMap<>()).put(match, score);
        candidatesByAnnotation.computeIfAbsent(annotation, _ -> new ArrayList<>())
            .add(new RowAnnotationCandidate(candidateRow, match, score));
      }
    }

    final Map<FeatureListRow, Set<MatchedLipid>> removalsByRow = new LinkedHashMap<>();
    for (final Entry<String, List<RowAnnotationCandidate>> entry : candidatesByAnnotation.entrySet()) {
      final List<List<RowAnnotationCandidate>> clusters = partitionByRtScope(entry.getValue(),
          options.duplicateScope());
      for (final List<RowAnnotationCandidate> cluster : clusters) {
        if (cluster.size() <= 1) {
          continue;
        }
        final RowAnnotationCandidate winner = selectWinner(cluster, options);
        if (winner == null) {
          continue;
        }
        for (final RowAnnotationCandidate candidate : cluster) {
          if (candidate == winner) {
            continue;
          }
          removalsByRow.computeIfAbsent(candidate.row(), _ -> new LinkedHashSet<>())
              .add(candidate.match());
        }
      }
    }

    final Map<FeatureListRow, MatchedLipid> selectedRemainingByRow = new LinkedHashMap<>();
    applyRowHandlingOptions(featureList, analysisType, options, removalsByRow,
        selectedRemainingByRow, scoreCache);

    return new MultiRowAnnotationCleanupPlan(removalsByRow, selectedRemainingByRow);
  }

  public static void applyCleanupPlan(final @NotNull MultiRowAnnotationCleanupPlan cleanupPlan) {
    final Set<FeatureListRow> rowsToUpdate = new LinkedHashSet<>();
    rowsToUpdate.addAll(cleanupPlan.annotationsToRemoveByRow().keySet());
    rowsToUpdate.addAll(cleanupPlan.selectedRemainingAnnotationByRow().keySet());

    for (final FeatureListRow targetRow : rowsToUpdate) {
      final Set<MatchedLipid> removals = cleanupPlan.annotationsToRemoveByRow()
          .getOrDefault(targetRow, Set.of());
      final MatchedLipid selected = cleanupPlan.selectedRemainingAnnotationByRow().get(targetRow);

      final List<MatchedLipid> original = targetRow.getLipidMatches();
      final List<MatchedLipid> updated = original.stream()
          .filter(match -> !removals.contains(match))
          .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
      if (selected != null && updated.remove(selected)) {
        updated.addFirst(selected);
      }
      if (!updated.equals(original)) {
        targetRow.setLipidAnnotations(updated);
      }
    }
  }

  private static void applyRowHandlingOptions(final @NotNull ModularFeatureList featureList,
      final @NotNull LipidAnalysisType analysisType,
      final @NotNull MultiRowAnnotationCleanupOptions options,
      final @NotNull Map<FeatureListRow, Set<MatchedLipid>> removalsByRow,
      final @NotNull Map<FeatureListRow, MatchedLipid> selectedRemainingByRow,
      final @NotNull Map<FeatureListRow, Map<MatchedLipid, Double>> scoreCache) {
    final Set<FeatureListRow> affectedRows = Set.copyOf(removalsByRow.keySet());
    for (final FeatureListRow affectedRow : affectedRows) {
      final Set<MatchedLipid> rowRemovals = removalsByRow.computeIfAbsent(affectedRow,
          _ -> new LinkedHashSet<>());
      if (rowRemovals.isEmpty()) {
        continue;
      }
      switch (options.rowHandlingMode()) {
        case DISCARD_LOWER_THAN_REMOVED -> {
          final double threshold = rowRemovals.stream().mapToDouble(
                  removed -> scoreFor(affectedRow, removed, featureList, analysisType, scoreCache))
              .max().orElse(Double.NaN);
          if (!Double.isFinite(threshold)) {
            continue;
          }
          for (final MatchedLipid remaining : affectedRow.getLipidMatches()) {
            final double score = scoreFor(affectedRow, remaining, featureList, analysisType,
                scoreCache);
            if (score < threshold) {
              rowRemovals.add(remaining);
            }
          }
        }
        case DISCARD_ALL_IF_ANY_REMOVED -> rowRemovals.addAll(affectedRow.getLipidMatches());
        case SELECT_REMAINING_HIGHEST_SCORE -> {
          final List<MatchedLipid> remaining = affectedRow.getLipidMatches().stream()
              .filter(match -> !rowRemovals.contains(match)).toList();
          if (remaining.size() <= 1) {
            continue;
          }
          final MatchedLipid bestRemaining = remaining.stream().max(Comparator.comparingDouble(
                  match -> scoreFor(affectedRow, match, featureList, analysisType, scoreCache)))
              .orElse(null);
          selectedRemainingByRow.put(affectedRow, bestRemaining);
        }
      }
    }
  }

  private static @NotNull Map<String, Map<IonizationType, Integer>> collectClassIonizationCounts(
      final @NotNull ModularFeatureList featureList) {
    final Map<String, Map<IonizationType, Integer>> countsByClass = new TreeMap<>();
    for (final FeatureListRow row : featureList.getRows()) {
      for (final MatchedLipid match : row.getLipidMatches()) {
        final String lipidClass = lipidClassLabel(match);
        final Map<IonizationType, Integer> classCounts = countsByClass.computeIfAbsent(lipidClass,
            _ -> new HashMap<>());
        final @Nullable IonizationType ionizationType = match.getIonizationType();
        if (ionizationType == null) {
          continue;
        }
        classCounts.merge(ionizationType, 1, Integer::sum);
      }
    }
    return countsByClass;
  }

  private static @Nullable RowAnnotationCandidate selectWinner(
      final @NotNull List<RowAnnotationCandidate> candidates,
      final @NotNull MultiRowAnnotationCleanupOptions options) {
    return candidates.stream().max((left, right) -> compareCandidates(left, right, options))
        .orElse(null);
  }

  private static int compareCandidates(final @NotNull RowAnnotationCandidate left,
      final @NotNull RowAnnotationCandidate right,
      final @NotNull MultiRowAnnotationCleanupOptions options) {
    final boolean useHighestScoreForClass = useHighestScoreSelectionForClass(left, right, options);
    final int preferredCmp = Boolean.compare(isPreferredIonization(left, options),
        isPreferredIonization(right, options));
    final int scoreCmp = Double.compare(left.combinedScore(), right.combinedScore());
    final int rowCmp = Integer.compare(right.row().getID(), left.row().getID());
    if (useHighestScoreForClass) {
      if (scoreCmp != 0) {
        return scoreCmp;
      }
      if (preferredCmp != 0) {
        return preferredCmp;
      }
      return rowCmp;
    }
    if (preferredCmp != 0) {
      return preferredCmp;
    }
    if (scoreCmp != 0) {
      return scoreCmp;
    }
    return rowCmp;
  }

  private static boolean useHighestScoreSelectionForClass(
      final @NotNull RowAnnotationCandidate left, final @NotNull RowAnnotationCandidate right,
      final @NotNull MultiRowAnnotationCleanupOptions options) {
    if (options.alwaysKeepHighestScore()) {
      return true;
    }
    final String leftClass = lipidClassLabel(left.match());
    final String rightClass = lipidClassLabel(right.match());
    return options.keepHighestScoreByLipidClass().contains(leftClass)
        || options.keepHighestScoreByLipidClass().contains(rightClass);
  }

  private static boolean isPreferredIonization(final @NotNull RowAnnotationCandidate candidate,
      final @NotNull MultiRowAnnotationCleanupOptions options) {
    final IonizationType preferredIonization = options.preferredIonizationByLipidClass()
        .get(lipidClassLabel(candidate.match()));
    return preferredIonization != null && preferredIonization == candidate.match()
        .getIonizationType();
  }

  /**
   * Splits candidates into RT clusters so that only rows within tolerance of each other compete as
   * duplicates. Uses connected-component grouping via union-find: if A–B and B–C are within
   * tolerance, all three end up in the same group even if A–C is not.
   *
   * <p>When scope is {@link DuplicateAnnotationScope#ALL_RTS} or the RT tolerance is null, all
   * candidates form a single group (current behaviour).
   *
   * <p>Rows with a null average RT are treated as always within tolerance (no false exclusions).
   */
  private static List<List<RowAnnotationCandidate>> partitionByRtScope(
      final List<RowAnnotationCandidate> candidates, final DuplicateAnnotationScopeFilter scope) {
    if (scope.scope() == DuplicateAnnotationScope.ALL_RTS || scope.rtTolerance() == null) {
      // single group — preserves current behaviour
      return List.of(candidates);
    }
    final int n = candidates.size();
    // clusterRep[i] = index of the representative candidate for candidate i's cluster
    final int[] clusterRep = IntStream.range(0, n).toArray();
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        final Float rtI = candidates.get(i).row().getAverageRT();
        final Float rtJ = candidates.get(j).row().getAverageRT();
        // null RT treated as always within tolerance (safe fallback — no false exclusions)
        if (rtI == null || rtJ == null || scope.rtTolerance().checkWithinTolerance(rtI, rtJ)) {
          union(clusterRep, i, j);
        }
      }
    }
    final Map<Integer, List<RowAnnotationCandidate>> groups = new LinkedHashMap<>();
    for (int i = 0; i < n; i++) {
      groups.computeIfAbsent(find(clusterRep, i), _ -> new ArrayList<>()).add(candidates.get(i));
    }
    return List.copyOf(groups.values());
  }

  private static int find(final int[] clusterRep, int i) {
    while (clusterRep[i] != i) {
      // path compression: skip one level to shorten future lookups
      clusterRep[i] = clusterRep[clusterRep[i]];
      i = clusterRep[i];
    }
    return i;
  }

  private static void union(final int[] clusterRep, final int i, final int j) {
    final int ri = find(clusterRep, i);
    final int rj = find(clusterRep, j);
    if (ri != rj) {
      clusterRep[ri] = rj;
    }
  }

  public static @NotNull String lipidClassLabel(final @NotNull MatchedLipid match) {
    final ILipidClass lipidClass = match.getLipidAnnotation().getLipidClass();
    if (lipidClass == null) {
      return "Unknown class";
    }
    final String abbr = Objects.toString(lipidClass.getAbbr(), "").trim();
    if (!abbr.isBlank()) {
      return abbr;
    }
    final String name = Objects.toString(lipidClass.getName(), "").trim();
    return name.isBlank() ? "Unknown class" : name;
  }

  private static double scoreFor(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match, final @NotNull ModularFeatureList featureList,
      final LipidAnalysisType analysisType,
      final @NotNull Map<FeatureListRow, Map<MatchedLipid, Double>> scoreCache) {
    return scoreCache.computeIfAbsent(row, _ -> new HashMap<>())
        .computeIfAbsent(match, key -> computeAnnotationScore(featureList, row, key, analysisType));
  }

  private static double computeAnnotationScore(final @NotNull ModularFeatureList featureList,
      final @NotNull FeatureListRow row, final @NotNull MatchedLipid match,
      final @NotNull LipidAnalysisType analysisType) {
    return LipidQcScoringUtils.computeCombinedAnnotationScore(featureList, row, match, true,
        analysisType.hasRetentionTimePattern(), analysisType);
  }
}
