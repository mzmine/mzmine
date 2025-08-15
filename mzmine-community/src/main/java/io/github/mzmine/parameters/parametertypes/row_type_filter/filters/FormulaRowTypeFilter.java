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

class FormulaRowTypeFilter extends AbstractRowTypeFilter {

  private final @NotNull IMolecularFormula queryFormula;
  private final List<IsotopeCount> isotopeCounts;

  FormulaRowTypeFilter(@NotNull RowTypeFilterOption selectedType,
      @NotNull MatchingMode matchingMode, @NotNull String query) {
    super(selectedType, matchingMode, query);

    final IMolecularFormula internal = FormulaUtils.createMajorIsotopeMolFormula(query);
    if (internal == null) {
      throw new QueryFormatException(query + " is not a valid formula");
    }
    queryFormula = internal;

    isotopeCounts = new ArrayList<>();
    for (IIsotope isotope : queryFormula.isotopes()) {
      isotopeCounts.add(new IsotopeCount(queryFormula.getIsotopeCount(isotope), isotope));
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

    return switch (matchingMode) {
      case EQUAL -> queryFormula.equals(formula);
      case CONTAINS, GREATER_EQUAL -> {
        // all atoms in query need to be at least in formula
        for (IsotopeCount queryIso : isotopeCounts) {
          if (formula.getIsotopeCount(queryIso.isotope()) < queryIso.count) {
            yield false;
          }
        }
        yield true;
      }
      case LESSER_EQUAL -> {
        for (IsotopeCount queryIso : isotopeCounts) {
          if (formula.getIsotopeCount(queryIso.isotope()) > queryIso.count) {
            yield false;
          }
        }
        yield true;
      }
      case NOT_EQUAL -> !queryFormula.equals(formula);
    };
  }

  private record IsotopeCount(int count, IIsotope isotope) {

  }
}
