/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.row_type_filter.filters;

import static io.github.mzmine.parameters.parametertypes.row_type_filter.filters.LipidFlexibleNotationParser.parseLipidNotation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.LipidFlexibleNotationParser.LipidFlexibleNotation;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.maths.MathOperator;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Matches lipids. Allowed options are:
 * <pre>
 *   - PC only define class
 *   - C20:2 for 20 C and 2 double bonds
 *   - PC20:2 also specifying the lipid class as PC
 *   - C>20:2 for more than 20 C and exactly 2 double bonds
 *   - C>20:>2 also more than 2 double bonds
 *   - PC18:2_18:0 defining chains
 *   - PC>18:>2_>18:0 defining ranges as well
 *   - Ranges PC20:2 - PC40:6
 * </pre>
 */
class LipidRowTypeFilter extends AbstractRowTypeFilter {

  // always set in ranges and single lipid notation
  private final @NotNull LipidFlexibleNotation rangeStart;
  // only present if actual range
  private final @Nullable LipidFlexibleNotation rangeEnd;

  LipidRowTypeFilter(@NotNull String query) throws QueryFormatException {
    this(query, MatchingMode.EQUAL);
  }

  LipidRowTypeFilter(@NotNull String query, @NotNull MatchingMode mode)
      throws QueryFormatException {
    // so far we only allow equal
    super(RowTypeFilterOption.LIPID, MatchingMode.EQUAL, query);

    if (!(mode == MatchingMode.EQUAL || mode == MatchingMode.CONTAINS)) {
      throw new IllegalArgumentException(
          "Unsupported matching mode, only supports equals and contains: " + mode);
    }

    // - separates ranges
    String[] ranges = query.split("-");
    if (ranges.length == 1) {
      final LipidFlexibleNotation internal = parseLipidNotation(ranges[0].trim(), false, false);
      // apply greater equal if mode is contains
      rangeStart =
          mode == MatchingMode.EQUAL ? internal : internal.withOperator(MathOperator.GREATER_EQ);
      rangeEnd = null;
    } else if (ranges.length == 2) {
      rangeStart = parseLipidNotation(ranges[0].trim(), false, false).withOperator(
          MathOperator.GREATER_EQ);
      rangeEnd = parseLipidNotation(ranges[1].trim(), false, false).withOperator(
          MathOperator.LESS_EQ);
    } else {
      throw new QueryFormatException(
          "Invalid format. Either enter a class PC, range PC20:2 - PC40:6, or special formats C>20:>2");
    }
  }


  @Override
  public boolean matches(FeatureListRow row) {
    for (MatchedLipid lipid : row.getLipidMatches()) {
      if (matchesLipid(lipid)) {
        return true;
      }
    }

    return CompoundAnnotationUtils.streamFeatureAnnotations(row)
        .map(FeatureAnnotation::getCompoundName).anyMatch(this::matchesLipidName);
  }

  private boolean matchesLipid(MatchedLipid lipid) {
    final List<String> classesCandidates = new ArrayList<>();

    final ILipidAnnotation annotation = lipid.getLipidAnnotation();
    try {
      final String mainClassShort = annotation.getLipidClass().getMainClass().getLipidCategory()
          .getAbbreviation();
      classesCandidates.add(mainClassShort.trim());
    } catch (Exception e) {
    }
    final String classShort = annotation.getLipidClass().getAbbr();
    classesCandidates.add(classShort.trim());

    if (!rangeStart.lipidClass().matchesLipidClass(classesCandidates)) {
      return false;
    }

    if (annotation instanceof SpeciesLevelAnnotation species) {
      return matchesSpeciesLevel(species.getNumberOfCarbons(), species.getNumberOfDBEs(),
          species.getNumberOfOxygens());

    } else if (annotation instanceof MolecularSpeciesLevelAnnotation molecularSpecies) {
      // TODO not sure if all chains are defined here for all classes - therefore also try name parsing
      // like spingolipids?
      final LipidFlexibleNotation flexible = LipidFlexibleNotationParser.toFlexible(
          molecularSpecies);

      // class already matched
      return matchesFlexible(flexible, false) || matchesLipidName(annotation.getAnnotation());
    } else {
      throw new IllegalArgumentException("Unsupported lipid annotation type: " + annotation);
    }
  }

  private boolean matchesFlexible(LipidFlexibleNotation flexible, boolean matchClass) {
    if (matchClass && !rangeStart.matchesClass(flexible)) {
      return false;
    }

    return rangeStart.matchesChains(flexible) && (rangeEnd == null || rangeEnd.matchesChains(
        flexible));
  }

  private boolean matchesSpeciesLevel(int c, int db, int oxy) {
    if (rangeStart.isClassOnly()) {
      return true;
    }
    return rangeStart.matchesSpeciesLevel(c, db, oxy) && (rangeEnd == null
        || rangeEnd.matchesSpeciesLevel(c, db, oxy));
  }

  public boolean matchesLipidName(String name) {
    try {
      final LipidFlexibleNotation flexible = parseLipidNotation(name, true, true);
      return matchesFlexible(flexible, true);
    } catch (QueryFormatException e) {
      // might just not be a lipid
    }
    return false;
  }

}
