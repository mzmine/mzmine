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

package io.github.mzmine.modules.dataprocessing.id_lipidid.utils;

import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.clampToUnit;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeCombinedAnnotationScore;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeMs1Score;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.ComponentWeights;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The LipidAnnotationResolver class is responsible for resolving and processing matched lipid
 * annotations associated with a feature list row. It provides methods to handle duplicate entries
 * and limit the maximum number of matched lipids to be retained. This class is designed to enhance
 * the accuracy and usefulness of lipid annotations for a given feature.
 * <p>
 * The class resolves lipids from multiple annotation runs resulting from the same or different
 * annotation parameters.
 */
public class LipidAnnotationResolver {

  private final boolean keepIsobars;
  private final boolean keepIsomers;
  private final boolean includeMs2Score;
  private final boolean includeElutionOrderScore;
  private final double minimumOverallQualityScore;
  private final @Nullable LipidAnalysisType lipidAnalysisType;
  private final @Nullable ComponentWeights customQcWeights;
  private final @Nullable MZTolerance mzToleranceMS1;

  private int maximumIdNumber;

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers) {
    this(keepIsobars, keepIsomers, true, true);
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final boolean includeMs2Score, final boolean includeElutionOrderScore) {
    this(keepIsobars, keepIsomers, includeMs2Score, includeElutionOrderScore, 0d, null);
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final boolean includeMs2Score, final boolean includeElutionOrderScore,
      final double minimumOverallQualityScore) {
    this(keepIsobars, keepIsomers, includeMs2Score, includeElutionOrderScore,
        minimumOverallQualityScore, null);
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final boolean includeMs2Score, final boolean includeElutionOrderScore,
      final double minimumOverallQualityScore,
      final @Nullable LipidAnalysisType lipidAnalysisType) {
    this(keepIsobars, keepIsomers, includeMs2Score, includeElutionOrderScore,
        minimumOverallQualityScore, lipidAnalysisType, null);
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final boolean includeMs2Score, final boolean includeElutionOrderScore,
      final double minimumOverallQualityScore, final @Nullable LipidAnalysisType lipidAnalysisType,
      final @Nullable ComponentWeights customQcWeights) {
    this(keepIsobars, keepIsomers, includeMs2Score, includeElutionOrderScore,
        minimumOverallQualityScore, lipidAnalysisType, customQcWeights, null);
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final boolean includeMs2Score, final boolean includeElutionOrderScore,
      final double minimumOverallQualityScore, final @Nullable LipidAnalysisType lipidAnalysisType,
      final @Nullable ComponentWeights customQcWeights,
      final @Nullable MZTolerance mzToleranceMS1) {
    this.keepIsobars = keepIsobars;
    this.keepIsomers = keepIsomers;
    this.includeMs2Score = includeMs2Score;
    this.includeElutionOrderScore = includeElutionOrderScore;
    this.minimumOverallQualityScore = clampToUnit(minimumOverallQualityScore);
    this.lipidAnalysisType = lipidAnalysisType;
    this.customQcWeights = customQcWeights;
    this.mzToleranceMS1 = mzToleranceMS1;
    this.maximumIdNumber = -1;
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final int maximumIdNumber) {
    this(keepIsobars, keepIsomers, maximumIdNumber, true, true);
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final int maximumIdNumber, final boolean includeMs2Score,
      final boolean includeElutionOrderScore) {
    this(keepIsobars, keepIsomers, includeMs2Score, includeElutionOrderScore, 0d, null);
    this.maximumIdNumber = maximumIdNumber;
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final int maximumIdNumber, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final double minimumOverallQualityScore) {
    this(keepIsobars, keepIsomers, includeMs2Score, includeElutionOrderScore,
        minimumOverallQualityScore, null);
    this.maximumIdNumber = maximumIdNumber;
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final int maximumIdNumber, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final double minimumOverallQualityScore,
      final @Nullable LipidAnalysisType lipidAnalysisType) {
    this(keepIsobars, keepIsomers, maximumIdNumber, includeMs2Score, includeElutionOrderScore,
        minimumOverallQualityScore, lipidAnalysisType, null);
  }

  public LipidAnnotationResolver(final boolean keepIsobars, final boolean keepIsomers,
      final int maximumIdNumber, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final double minimumOverallQualityScore,
      final @Nullable LipidAnalysisType lipidAnalysisType,
      final @Nullable ComponentWeights customQcWeights) {
    this(keepIsobars, keepIsomers, includeMs2Score, includeElutionOrderScore,
        minimumOverallQualityScore, lipidAnalysisType, customQcWeights);
    this.maximumIdNumber = maximumIdNumber;
  }

  public @NotNull List<MatchedLipid> resolveFeatureListRowMatchedLipids(
      final @NotNull FeatureListRow featureListRow,
      final @NotNull Set<MatchedLipid> matchedLipids) {
    final List<MatchedLipid> resolvedMatchedLipidsList = removeDuplicates(matchedLipids);
    final Map<MatchedLipid, Double> preFilterScoreCache = new IdentityHashMap<>();
    final Map<MatchedLipid, Double> qualityScoreCache = new IdentityHashMap<>();
    sortByRankingScore(featureListRow, resolvedMatchedLipidsList, preFilterScoreCache);
    filterByMinimumQuality(featureListRow, resolvedMatchedLipidsList, qualityScoreCache);
    //TODO: Add Keep isobars functionality

    //TODO: Add keep isomers functionality

    //add to resolved list
    if (maximumIdNumber != -1 && resolvedMatchedLipidsList.size() > maximumIdNumber) {
      filterMaximumNumberOfId(resolvedMatchedLipidsList);
    }
    return resolvedMatchedLipidsList;
  }

  private @NotNull List<MatchedLipid> removeDuplicates(
      final @NotNull Set<MatchedLipid> resolvedMatchedLipids) {
    return resolvedMatchedLipids.stream().collect(Collectors.collectingAndThen(
        Collectors.toCollection(() -> new TreeSet<>(comparatorMatchedLipids())), ArrayList::new));
  }

  private void filterMaximumNumberOfId(final @NotNull List<MatchedLipid> resolvedMatchedLipids) {
    final Iterator<MatchedLipid> iterator = resolvedMatchedLipids.iterator();
    while (iterator.hasNext()) {
      final MatchedLipid lipid = iterator.next();
      if (resolvedMatchedLipids.indexOf(lipid) > maximumIdNumber) {
        iterator.remove();
      }
    }
  }

  private void filterByMinimumQuality(final @NotNull FeatureListRow featureListRow,
      final @NotNull List<MatchedLipid> resolvedMatchedLipids,
      final @NotNull Map<MatchedLipid, Double> qualityScoreCache) {
    if (minimumOverallQualityScore <= 0d) {
      return;
    }
    resolvedMatchedLipids.removeIf(matchedLipid -> qualityScoreCache.computeIfAbsent(matchedLipid,
        lipid -> computeQualityScore(featureListRow, lipid)) < minimumOverallQualityScore);
  }

  private static @NotNull Comparator<MatchedLipid> comparatorMatchedLipids() {
    return Comparator.comparing(
            (MatchedLipid lipid) -> Objects.toString(lipid.getLipidAnnotation().getAnnotation(), ""))
        .thenComparingDouble(LipidAnnotationResolver::safeMs2Score)
        .thenComparing(MatchedLipid::getAccurateMz, Comparator.nullsLast(Double::compareTo));
  }

  private void sortByRankingScore(final @NotNull FeatureListRow featureListRow,
      final @NotNull List<MatchedLipid> matchedLipids,
      final @NotNull Map<MatchedLipid, Double> scoreCache) {
    matchedLipids.sort((left, right) -> {
      final double leftScore = scoreCache.computeIfAbsent(left,
          lipid -> computePreFilterRankingScore(featureListRow, lipid));
      final double rightScore = scoreCache.computeIfAbsent(right,
          lipid -> computePreFilterRankingScore(featureListRow, lipid));
      final int byScore = Double.compare(rightScore, leftScore);
      if (byScore != 0) {
        return byScore;
      }
      final int byMs2 = Double.compare(safeMs2Score(right), safeMs2Score(left));
      if (byMs2 != 0) {
        return byMs2;
      }
      return Objects.toString(left.getLipidAnnotation().getAnnotation(), "")
          .compareTo(Objects.toString(right.getLipidAnnotation().getAnnotation(), ""));
    });
  }

  private double computePreFilterRankingScore(final @NotNull FeatureListRow featureListRow,
      final @NotNull MatchedLipid matchedLipid) {
    final double ms1Score = computeMs1Score(featureListRow, matchedLipid, mzToleranceMS1);
    final Double preferenceScore = computeRelativeRuleIntensityPreferenceScore(matchedLipid);
    final double prePreferenceScore;
    if (includeMs2Score) {
      prePreferenceScore = (ms1Score + safeMs2Score(matchedLipid)) / 2d;
    } else {
      prePreferenceScore = ms1Score;
    }
    if (preferenceScore == null) {
      return prePreferenceScore;
    }
    return (prePreferenceScore + preferenceScore) / 2d;
  }

  private double computeQualityScore(final @NotNull FeatureListRow featureListRow,
      final @NotNull MatchedLipid matchedLipid) {
    if (featureListRow.getFeatureList() instanceof ModularFeatureList modularFeatureList) {
      return computeCombinedAnnotationScore(modularFeatureList, featureListRow, matchedLipid,
          includeMs2Score, includeElutionOrderScore, lipidAnalysisType, customQcWeights,
          mzToleranceMS1);
    }
    return computePreFilterRankingScore(featureListRow, matchedLipid);
  }

  private static double safeMs2Score(final @Nullable MatchedLipid matchedLipid) {
    if (matchedLipid == null || matchedLipid.getMsMsScore() == null) {
      return 0d;
    }
    return clampToUnit(matchedLipid.getMsMsScore());
  }

  static @Nullable Double computeRelativeRuleIntensityPreferenceScore(
      final @NotNull MatchedLipid matchedLipid) {
    final Map<RuleKey, Integer> expectedRelativeIntensityWeights = collectExpectedRelativeIntensityWeights(
        matchedLipid);
    if (expectedRelativeIntensityWeights.size() < 2) {
      return null;
    }

    final Map<RuleKey, Double> maxObservedIntensityByRule = new HashMap<>();
    for (final LipidFragment fragment : matchedLipid.getMatchedFragments()) {
      final LipidFragmentationRuleType ruleType = fragment.getRuleType();
      final double intensity = fragment.getDataPoint().getIntensity();
      if (ruleType == null || !Double.isFinite(intensity) || intensity <= 0d) {
        continue;
      }
      final RuleKey ruleKey = new RuleKey(ruleType,
          Objects.toString(fragment.getOriginatingRuleFormula(), ""));
      if (!expectedRelativeIntensityWeights.containsKey(ruleKey)) {
        continue;
      }
      maxObservedIntensityByRule.merge(ruleKey, intensity, Math::max);
    }

    if (maxObservedIntensityByRule.isEmpty()) {
      return null;
    }
    if (maxObservedIntensityByRule.size() == 1) {
      final RuleKey observedRule = maxObservedIntensityByRule.keySet().iterator().next();
      final Integer expectedRelativeIntensity = expectedRelativeIntensityWeights.get(observedRule);
      if (expectedRelativeIntensity == null || expectedRelativeIntensity <= 0) {
        return null;
      }
      return clampToUnit(expectedRelativeIntensity / 100d);
    }
    final double sumExpected = expectedRelativeIntensityWeights.values().stream()
        .mapToDouble(Integer::doubleValue).sum();
    final double sumObserved = expectedRelativeIntensityWeights.keySet().stream().mapToDouble(
        key -> maxObservedIntensityByRule.getOrDefault(key, 0d)).sum();
    if (sumExpected <= 0d || sumObserved <= 0d) {
      return null;
    }

    final double l1Distance = expectedRelativeIntensityWeights.entrySet().stream().mapToDouble(
        entry -> {
      final double expected = entry.getValue() / sumExpected;
      final double observed = maxObservedIntensityByRule.getOrDefault(entry.getKey(), 0d)
          / sumObserved;
      return Math.abs(expected - observed);
    }).sum();
    final double normalized = 1d - Math.min(l1Distance / 2d, 1d);
    return clampToUnit(normalized);
  }

  private static @NotNull Map<RuleKey, Integer> collectExpectedRelativeIntensityWeights(
      final @NotNull MatchedLipid matchedLipid) {
    final Map<RuleKey, Integer> expectedRelativeIntensityWeights = new HashMap<>();
    final LipidFragmentationRule[] fragmentationRules = matchedLipid.getLipidAnnotation()
        .getLipidClass().getFragmentationRules();
    final var ionizationType = matchedLipid.getIonizationType();
    for (final LipidFragmentationRule rule : fragmentationRules) {
      final LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
      final int relativeIntensityWeight = rule.getRelativeIntensityWeight();
      if (ruleType == null || relativeIntensityWeight <= 0
          || rule.getIonizationType() != ionizationType) {
        continue;
      }
      final RuleKey ruleKey = new RuleKey(ruleType,
          Objects.toString(rule.getMolecularFormula(), ""));
      expectedRelativeIntensityWeights.merge(ruleKey, relativeIntensityWeight, Math::max);
    }
    return expectedRelativeIntensityWeights;
  }

  private record RuleKey(@NotNull LipidFragmentationRuleType ruleType,
                         @NotNull String ruleFormula) {
  }

}
