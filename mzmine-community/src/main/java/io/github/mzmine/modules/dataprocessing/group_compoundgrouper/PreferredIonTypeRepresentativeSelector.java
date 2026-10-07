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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Picks the representative row by ion type first:
 * <ol>
 *   <li>highest {@link IonTypeRanking#score(IonType)} of the row's best {@link IonIdentity}, using
 *       the user definable ranking from the {@link FeatureListPreferences}
 *       of the feature list</li>
 *   <li>ties broken by annotation quality (AQS), see
 *       {@link CompoundRepresentativeSelector#annotationQualityBestFirst(List)}</li>
 *   <li>then by intensity</li>
 * </ol>
 * Rows without an {@link IonIdentity} are not eligible, the fallback is the highest-intensity row.
 * Polarity is ignored.
 * <p>
 * decision: the ranking replaces the former hard coded adduct tier list so that this selector, the
 * ion identity networking and every other consumer of {@link IonTypeRanking} agree on which ion
 * type is the more likely explanation, and so that users can change it in one place.
 */
public final class PreferredIonTypeRepresentativeSelector implements
    CompoundRepresentativeSelector {

  /**
   * A row with its precomputed sort keys. {@link AnnotationSummary} caches its scores internally,
   * so it is built once per row instead of on every comparison.
   */
  private record Candidate(@NotNull FeatureListRow row, double ionScore,
                           @NotNull AnnotationSummary summary) {

  }

  @Override
  public @NotNull FeatureListRow pickRepresentative(@NotNull final List<FeatureListRow> members) {
    // ranking is shared by the whole feature list, so any member defines it
    final IonTypeRanking ranking = members.getFirst().getFeatureList().getPreferences()
        .getIonTypeRanking();

    final Comparator<Candidate> bestFirst = Comparator.<Candidate>comparingDouble(
            Candidate::ionScore).reversed().thenComparing(Candidate::summary,
            CompoundRepresentativeSelector.annotationQualityBestFirst(members))
        .thenComparing(c -> CompoundRepresentativeSelector.heightOrZero(c.row()),
            Comparator.reverseOrder());

    // best first comparator so the minimum is the best candidate
    return members.stream().map(row -> toCandidate(row, ranking)).filter(Objects::nonNull)
        .min(bestFirst).map(Candidate::row)
        .orElseGet(() -> CompoundRepresentativeSelector.pickHighestIntensity(members));
  }

  private static @Nullable Candidate toCandidate(@NotNull final FeatureListRow row,
      @NotNull final IonTypeRanking ranking) {
    final IonIdentity ion = row.getBestIonIdentity();
    if (ion == null) {
      return null;
    }
    // AnnotationSummary.of uses the preferred annotation of the row, which may be user defined
    return new Candidate(row, ranking.score(ion.getIonType()), AnnotationSummary.of(row));
  }
}
