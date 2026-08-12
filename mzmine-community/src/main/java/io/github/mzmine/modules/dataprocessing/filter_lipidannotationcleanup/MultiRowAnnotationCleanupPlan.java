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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable plan record describing which lipid annotations will be removed per row and which
 * annotation will remain selected after the cleanup is applied.
 */
public record MultiRowAnnotationCleanupPlan(
    @NotNull Map<FeatureListRow, Set<MatchedLipid>> annotationsToRemoveByRow,
    @NotNull Map<FeatureListRow, MatchedLipid> selectedRemainingAnnotationByRow) {

  public MultiRowAnnotationCleanupPlan {
    final Map<FeatureListRow, Set<MatchedLipid>> copiedRemovals = new LinkedHashMap<>();
    for (final Map.Entry<FeatureListRow, Set<MatchedLipid>> entry : annotationsToRemoveByRow.entrySet()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      copiedRemovals.put(entry.getKey(), Set.copyOf(entry.getValue()));
    }
    annotationsToRemoveByRow = Map.copyOf(copiedRemovals);
    selectedRemainingAnnotationByRow = Map.copyOf(
        new LinkedHashMap<>(selectedRemainingAnnotationByRow));
  }

  public int removedAnnotationCount() {
    return annotationsToRemoveByRow.values().stream().mapToInt(Set::size).sum();
  }

  public int affectedRowCount() {
    return annotationsToRemoveByRow.size();
  }

  public boolean hasRemovals() {
    return removedAnnotationCount() > 0;
  }
}
