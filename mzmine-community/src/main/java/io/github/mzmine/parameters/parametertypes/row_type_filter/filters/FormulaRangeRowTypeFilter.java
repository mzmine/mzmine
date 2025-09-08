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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

class FormulaRangeRowTypeFilter extends AbstractRowTypeFilter {

  private final @NotNull IMolecularFormula minFormula;
  private final @NotNull IMolecularFormula maxFormula;
  private final List<IsotopeCount> minIsotopes;
  private final List<IsotopeCount> maxIsotopes;

  /**
   * @param query format is min formula - max formula like: C10H20Br2 - C20Br4
   */
  FormulaRangeRowTypeFilter(@NotNull RowTypeFilterOption selectedType,
      @NotNull MatchingMode matchingMode, @NotNull String query) {
    super(selectedType, matchingMode, query);

    final String[] split = query.split("-");
    if (split.length != 2) {
      throw new QueryFormatException(
          query + " is not a valid formula range, e.g., C10H20Br2 - C20Br4");
    }

    final String min = split[0].trim();
    final IMolecularFormula minInternal = FormulaUtils.createMajorIsotopeMolFormula(min);
    if (minInternal == null) {
      throw new QueryFormatException(min + " is not a valid formula");
    }
    final String max = split[1].trim();
    final IMolecularFormula maxInternal = FormulaUtils.createMajorIsotopeMolFormula(max);
    if (maxInternal == null) {
      throw new QueryFormatException(max + " is not a valid formula");
    }

    minFormula = minInternal;
    maxFormula = maxInternal;

    minIsotopes = new ArrayList<>();
    for (IIsotope isotope : minFormula.isotopes()) {
      minIsotopes.add(new IsotopeCount(minFormula.getIsotopeCount(isotope), isotope));
    }

    maxIsotopes = new ArrayList<>();
    for (IIsotope isotope : maxFormula.isotopes()) {
      final int count = Math.max(minFormula.getIsotopeCount(isotope),
          maxFormula.getIsotopeCount(isotope));

      maxIsotopes.add(new IsotopeCount(count, isotope));
    }
  }

  @Override
  public boolean matches(FeatureListRow row) {
    return CompoundAnnotationUtils.streamFeatureAnnotations(row).map(FeatureAnnotation::getFormula)
        .anyMatch(this::matchesFormula);
  }

  private boolean matchesFormula(String formulaStr) {
    final IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(formulaStr);
    if (formula == null) {
      return false;
    }

    // always the same matching within range
    // all atoms in query need to be at least in formula
    for (IsotopeCount queryIso : minIsotopes) {
      if (formula.getIsotopeCount(queryIso.isotope()) < queryIso.count) {
        return false;
      }
    }
    for (IsotopeCount queryIso : maxIsotopes) {
      if (formula.getIsotopeCount(queryIso.isotope()) > queryIso.count) {
        return false;
      }
    }
    return true;
  }

  private record IsotopeCount(int count, IIsotope isotope) {

  }
}
