package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
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
