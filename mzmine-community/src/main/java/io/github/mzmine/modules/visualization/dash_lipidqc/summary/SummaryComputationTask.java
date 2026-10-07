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

package io.github.mzmine.modules.visualization.dash_lipidqc.summary;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Background task that groups lipid annotations by the selected {@link SummaryGroup} hierarchy and
 * computes annotation counts for the {@link LipidSummaryPane} bar chart.
 */
final class SummaryComputationTask extends FxUpdateTask<LipidSummaryPane> {

  private static final Pattern SUBCLASS_TOKEN_PATTERN = Pattern.compile(
      "^([A-Za-z][A-Za-z0-9-]*(?:\\s+[OP]-)?)");

  private final @Nullable ModularFeatureList featureList;
  private final @Nullable SummaryGroup grouping;
  private final @Nullable SummaryCountMode countMode;
  private final @Nullable String selectedGroup;
  private @NotNull SummaryComputationResult result;

  SummaryComputationTask(final @NotNull LipidSummaryPane pane,
      final @Nullable ModularFeatureList featureList, final @Nullable SummaryGroup grouping,
      final @Nullable SummaryCountMode countMode, final @Nullable String selectedGroup) {
    super("lipidqc-summary-update", pane);
    this.featureList = featureList;
    this.grouping = grouping;
    this.countMode = countMode;
    this.selectedGroup = selectedGroup;
    final SummaryGroup fallbackGrouping = grouping != null ? grouping : SummaryGroup.LIPID_SUBCLASS;
    final SummaryCountMode fallbackCountMode =
        countMode != null ? countMode : SummaryCountMode.ROW_COUNT;
    result = new SummaryComputationResult("Select a feature list with lipid annotations.",
        fallbackGrouping, fallbackCountMode, Map.of(), Map.of(), Map.of(), 0, null);
  }

  @Override
  protected void process() {
    if (featureList == null) {
      result = new SummaryComputationResult("Select a feature list with lipid annotations.",
          fallbackGrouping(), fallbackCountMode(), Map.of(), Map.of(), Map.of(), 0, null);
      return;
    }
    final SummaryGroup localGrouping = fallbackGrouping();
    final SummaryCountMode localCountMode = fallbackCountMode();
    final List<MatchedLipid> bestMatches = featureList.getRows().stream()
        .map(FeatureListRow::getLipidMatches).filter(matches -> !matches.isEmpty())
        .map(List::getFirst).toList();
    if (bestMatches.isEmpty()) {
      result = new SummaryComputationResult("No lipid annotations available in this feature list.",
          localGrouping, localCountMode, Map.of(), Map.of(), Map.of(), 0, null);
      return;
    }

    final Map<String, Integer> groupToCount = new TreeMap<>();
    final Map<String, Set<String>> groupToUniqueAnnotations = new TreeMap<>();
    final Map<String, String> groupTooltip = new TreeMap<>();
    final Map<String, Set<Integer>> groupToRowIds = new TreeMap<>();
    int totalLipidRows = 0;
    for (final FeatureListRow row : featureList.getRows()) {
      final List<MatchedLipid> matches = row.get(LipidMatchListType.class);
      if (matches == null || matches.isEmpty()) {
        continue;
      }
      totalLipidRows++;
      if (localCountMode == SummaryCountMode.ROW_COUNT) {
        final @Nullable MatchedLipid lipid = LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(
            row);
        if (lipid == null) {
          continue;
        }
        final String groupName = localGrouping.extractGroupLabel(lipid,
            extractSubclassToken(lipid));
        groupTooltip.putIfAbsent(groupName,
            localGrouping.extractTooltip(lipid, extractSubclassToken(lipid)));
        groupToCount.merge(groupName, 1, Integer::sum);
        groupToRowIds.computeIfAbsent(groupName, _ -> new HashSet<>()).add(row.getID());
        continue;
      }

      // collect species-level keys from molecular species annotations to avoid double-counting
      // when both species-level and molecular species-level annotations exist for the same species
      final Set<String> rowMolecularAggregateKeys = new HashSet<>();
      for (final MatchedLipid lipid : matches) {
        if (lipid.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation) {
          rowMolecularAggregateKeys.add(getSpeciesLevelKey(lipid.getLipidAnnotation()));
        }
      }
      final Set<String> rowUniqueAnnotationKeys = new HashSet<>();
      for (final MatchedLipid lipid : matches) {
        final String groupName = localGrouping.extractGroupLabel(lipid,
            extractSubclassToken(lipid));
        groupTooltip.putIfAbsent(groupName,
            localGrouping.extractTooltip(lipid, extractSubclassToken(lipid)));
        groupToRowIds.computeIfAbsent(groupName, _ -> new HashSet<>()).add(row.getID());

        final @Nullable String uniqueAnnotationKey = uniqueAnnotationKey(lipid,
            rowMolecularAggregateKeys);
        if (uniqueAnnotationKey == null || !rowUniqueAnnotationKeys.add(uniqueAnnotationKey)) {
          continue;
        }
        groupToUniqueAnnotations.computeIfAbsent(groupName, _ -> new HashSet<>())
            .add(uniqueAnnotationKey);
      }
    }
    if (localCountMode == SummaryCountMode.UNIQUE_ANNOTATIONS) {
      groupToUniqueAnnotations.forEach(
          (group, annotations) -> groupToCount.put(group, annotations.size()));
    }
    final int totalCount =
        localCountMode == SummaryCountMode.UNIQUE_ANNOTATIONS ? groupToUniqueAnnotations.values()
            .stream().flatMap(Set::stream).collect(Collectors.toSet()).size() : totalLipidRows;

    final @Nullable String effectiveSelectedGroup =
        selectedGroup != null && groupToCount.containsKey(selectedGroup) ? selectedGroup : null;
    result = new SummaryComputationResult(null, localGrouping, localCountMode, groupToCount,
        groupToRowIds, groupTooltip, totalCount, effectiveSelectedGroup);
  }

  @Override
  protected void updateGuiModel() {
    model.applySummaryResult(result);
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Calculating lipid summary plot";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }

  private @NotNull SummaryGroup fallbackGrouping() {
    return grouping != null ? grouping : SummaryGroup.LIPID_SUBCLASS;
  }

  private @NotNull SummaryCountMode fallbackCountMode() {
    return countMode != null ? countMode : SummaryCountMode.ROW_COUNT;
  }

  private static @NotNull String extractSubclassToken(final @NotNull MatchedLipid lipid) {
    final String annotation = lipid.getLipidAnnotation().getAnnotation();
    if (annotation == null || annotation.isBlank()) {
      return lipid.getLipidAnnotation().getLipidClass().getAbbr();
    }
    final Matcher matcher = SUBCLASS_TOKEN_PATTERN.matcher(annotation);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    final String[] parts = annotation.trim().split("\\s+", 2);
    if (parts.length > 0 && !parts[0].isBlank()) {
      return parts[0];
    }
    return lipid.getLipidAnnotation().getLipidClass().getAbbr();
  }

  private static @Nullable String uniqueAnnotationKey(final @NotNull MatchedLipid lipid,
      final @NotNull Set<String> rowMolecularAggregateKeys) {
    final String speciesKey = getSpeciesLevelKey(lipid.getLipidAnnotation());
    switch (lipid.getLipidAnnotation()) {
      case SpeciesLevelAnnotation _ -> {
        // skip species-level annotation if the same species is already covered by a molecular annotation
        if (rowMolecularAggregateKeys.contains(speciesKey)) {
          return null;
        }
        return "S|" + speciesKey;
      }
      case MolecularSpeciesLevelAnnotation _ -> {
        return "M|" + lipid.getLipidAnnotation().getAnnotation();
      }
    }
  }

  private static @NotNull String getSpeciesLevelKey(@NotNull ILipidAnnotation annotation) {
    // todo: can this be replaced by getSpeciesLevelAnnotation? Is the abbreviation unique enough?
    return annotation.getLipidClass().getName() + "|" + annotation.getChainsCarbonCount() + ":"
        + annotation.getChainsDoubleBondCount();
  }
}
