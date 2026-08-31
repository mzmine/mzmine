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

package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy for picking the representative {@link FeatureListRow} of a compound component (a list of
 * feature list rows that belong to the same compound). Implementations encode different preferences
 * (e.g. annotation-based, preferred adduct tier, intensity).
 */
public interface CompoundRepresentativeSelector {

  static @Nullable FeatureListRow highestMatching(@NotNull final List<FeatureListRow> members,
      @NotNull final Predicate<FeatureListRow> predicate) {
    FeatureListRow best = null;
    float bestHeight = Float.NEGATIVE_INFINITY;
    for (final FeatureListRow row : members) {
      if (!predicate.test(row)) {
        continue;
      }
      final float h = CompoundRepresentativeSelector.heightOrZero(row);
      if (best == null || h > bestHeight) {
        best = row;
        bestHeight = h;
      }
    }
    return best;
  }

  /**
   * @return the row with the best preferred annotation or null if no member is annotated
   */
  static @Nullable FeatureListRow pickBestAnnotated(@NotNull final List<FeatureListRow> members) {
    // sort config is shared by the whole feature list, so any member defines the ranking
    final Comparator<@Nullable AnnotationSummary> bestFirst = members.getFirst().getFeatureList()
        .getAnnotationSortConfig().sortOrder().getComparatorHighFirst();
    // equally confident annotations are decided by intensity
    final Comparator<@NotNull AnnotationSummary> annotationThenIntensity = bestFirst.thenComparing(
        summary -> CompoundRepresentativeSelector.heightOrZero(summary.row()),
        Comparator.reverseOrder());

    // AnnotationSummary.of uses the preferred annotation of the row, which may be user defined
    return members.stream().map(AnnotationSummary::of).filter(s -> s.annotation() != null)
        // best first comparator so the minimum is the best annotation
        .min(annotationThenIntensity).map(AnnotationSummary::row).orElse(null);
  }

  /**
   * Pick one row to act as the representative of the given component. {@code members} is never
   * empty.
   */
  @NotNull FeatureListRow pickRepresentative(@NotNull List<FeatureListRow> members);

  /**
   * Shared fallback: row with the highest {@link FeatureListRow#getMaxHeight()}. Used by all
   * selectors when their primary criterion finds no candidate.
   */
  static @NotNull FeatureListRow pickHighestIntensity(@NotNull final List<FeatureListRow> members) {
    FeatureListRow best = members.getFirst();
    float bestHeight = heightOrZero(best);
    for (int i = 1; i < members.size(); i++) {
      final FeatureListRow row = members.get(i);
      final float h = heightOrZero(row);
      if (h > bestHeight) {
        best = row;
        bestHeight = h;
      }
    }
    return best;
  }

  static float heightOrZero(@NotNull final FeatureListRow row) {
    final Float h = row.getMaxHeight();
    return h == null ? 0f : h;
  }
}
