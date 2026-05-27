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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeCandidate;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeDetector;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for resolving the preferred {@link MatchedLipid} for a feature list row in the
 * lipid QC dashboard, and for extracting structural properties from lipid annotations.
 */
public final class LipidQcAnnotationSelectionUtils {

  private LipidQcAnnotationSelectionUtils() {
  }

  /**
   * Returns the preferred lipid match for the given row: the user-preferred annotation if it is a
   * {@link MatchedLipid}, otherwise the first match in the list, or {@code null} if the list is
   * empty.
   */
  public static @Nullable MatchedLipid getPreferredLipidMatch(final @NotNull FeatureListRow row) {
    final List<MatchedLipid> matches = row.getLipidMatches();
    if (matches.isEmpty()) {
      return null;
    }
    final @Nullable FeatureAnnotation preferredAnnotation = row.getPreferredAnnotation();
    if (preferredAnnotation instanceof MatchedLipid preferredLipid) {
      return preferredLipid;
    }
    return matches.getFirst();
  }

  /**
   * Returns the preferred lipid match for the row, falling back to a
   * {@link KendrickFalseNegativeDetector} candidate when the row has no annotation. Returns
   * {@code null} if neither source yields a match.
   */
  public static @Nullable MatchedLipid getPreferredOrPotentialLipidMatch(
      final @NotNull FeatureListRow row) {
    final @Nullable MatchedLipid preferred = getPreferredLipidMatch(row);
    if (preferred != null) {
      return preferred;
    }
    if (row.getFeatureList() instanceof ModularFeatureList featureList) {
      final @Nullable KendrickFalseNegativeCandidate potential =
          new KendrickFalseNegativeDetector(featureList).detectCandidate(row);
      return potential == null ? null : potential.match();
    }
    return null;
  }

  /**
   * Extracts the number of double bond equivalents (DBE) from a lipid annotation at species level.
   * Works for both {@link SpeciesLevelAnnotation}
   * and {@link MolecularSpeciesLevelAnnotation}
   * (sums chains).
   */
  public static int extractDbe(final @NotNull ILipidAnnotation annotation) {
    return annotation.getChainsDoubleBondCount();
  }

  /**
   * Extracts the carbon chain length from a lipid annotation at species level. Works for both
   * {@link SpeciesLevelAnnotation}
   * and {@link MolecularSpeciesLevelAnnotation}
   * (sums chains).
   */
  public static int extractCarbons(final @NotNull ILipidAnnotation annotation) {
    return annotation.getChainsCarbonCount();
  }
}
