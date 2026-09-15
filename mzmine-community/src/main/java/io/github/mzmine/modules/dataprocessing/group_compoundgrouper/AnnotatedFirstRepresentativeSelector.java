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

import static io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector.highestMatching;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummaryOrder;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Picks the representative row by annotation quality, then by intensity:
 * <ol>
 *   <li>row with the best {@link FeatureListRow#getPreferredAnnotation()}, ranked by its
 *       {@link AnnotationSummary} using the
 *       {@link AnnotationSummaryOrder}
 *       of the feature list, ties broken by intensity</li>
 *   <li>highest-intensity row carrying any {@link IonIdentity}</li>
 *   <li>highest-intensity row overall</li>
 * </ol>
 * Polarity is ignored.
 */
public final class AnnotatedFirstRepresentativeSelector implements CompoundRepresentativeSelector {

  @Override
  public @NotNull FeatureListRow pickRepresentative(@NotNull final List<FeatureListRow> members) {
    final FeatureListRow annotated = CompoundRepresentativeSelector.pickBestAnnotated(members);
    if (annotated != null) {
      return annotated;
    }
    // not annotated -> use highest ion
    final FeatureListRow withIon = highestMatching(members, r -> r.getBestIonIdentity() != null);
    if (withIon != null) {
      return withIon;
    }
    return CompoundRepresentativeSelector.pickHighestIntensity(members);
  }

}
