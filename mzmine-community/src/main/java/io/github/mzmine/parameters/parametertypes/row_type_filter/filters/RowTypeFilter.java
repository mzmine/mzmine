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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption;
import org.jetbrains.annotations.Nullable;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public interface RowTypeFilter {


  /**
   * @param selectedType
   * @param matchingMode
   * @param query        if null then no filter
   * @return null if query is empty
   */
  @JsonCreator
  @Nullable
  static RowTypeFilter create(@JsonProperty("selectedType") RowTypeFilterOption selectedType,
      @JsonProperty("matchingMode") MatchingMode matchingMode,
      @JsonProperty("query") @Nullable String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    return switch (selectedType) {
      // numerics
      case ION_IDENTITY_ID -> new NumericRowTypeFilter(selectedType, matchingMode, query, row -> {
        final IonIdentity iin = row.getBestIonIdentity();
        return iin == null ? null : iin.getNetID();
      });
      case FRAGMENT_SCANS -> new NumericRowTypeFilter(selectedType, matchingMode, query,
          row -> row.getAllFragmentScans().size());
      // simple string matching
      case ION_TYPE -> new IonTypeRowTypeFilter(selectedType, matchingMode, query);
      case COMPOUND_NAME ->
          new AnnotationAsStringRowTypeFilter(selectedType, matchingMode, query, false,
              FeatureAnnotation::getCompoundName);
      case IUPAC_NAME ->
          new AnnotationAsStringRowTypeFilter(selectedType, matchingMode, query, false,
              FeatureAnnotation::getIupacName);
      case FORMULA -> new FormulaRowTypeFilter(selectedType, matchingMode, query);
      case FORMULA_RANGE -> new FormulaRangeRowTypeFilter(selectedType, matchingMode, query);
      // special cases
      // allow substructure matching
      case SMILES, INCHI, SMARTS -> new StructureRowTypeFilter(selectedType, matchingMode, query);
      // allow matching to special cases like
      // >C30:>2 or :>2 for more than two double bonds
      // direct matching to names, classes
      case LIPID -> new LipidRowTypeFilter(query, matchingMode);
    };
  }

  @JsonProperty("selectedType")
  RowTypeFilterOption selectedType();

  @JsonProperty("matchingMode")
  MatchingMode matchingMode();

  @JsonProperty("query")
  String query();

  boolean matches(FeatureListRow row);


}
