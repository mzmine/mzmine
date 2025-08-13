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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public enum RowTypeFilterOption implements UniqueIdSupplier {
  ION_IDENTITY_ID, ION_TYPE, COMPOUND_NAME, IUPAC_NAME, FORMULA, FORMULA_RANGE, SMILES, INCHI, SMARTS, LIPID, FRAGMENT_SCANS;

  @JsonCreator
  @Nullable
  public static RowTypeFilterOption fromUniqueID(String uniqueId) {
    return UniqueIdSupplier.parseOrElse(uniqueId, values(), null);
  }

  @Override
  public String toString() {
    return switch (this) {
      case ION_IDENTITY_ID -> "Ion identity ID";
      case ION_TYPE -> "Ion type";
      case COMPOUND_NAME -> "Compound name";
      case IUPAC_NAME -> "IUPAC name";
      case FORMULA -> "Formula";
      case FORMULA_RANGE -> "Formula range";
      case SMILES -> "SMILES";
      case INCHI -> "InChI";
      case SMARTS -> "SMARTS";
      case LIPID -> "Lipid";
      case FRAGMENT_SCANS -> "Fragment scans";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case IUPAC_NAME -> "iupac";
      case FORMULA -> "formula";
      case FORMULA_RANGE -> "formula_range";
      case SMILES -> "smiles";
      case INCHI -> "inchi";
      case SMARTS -> "smarts";
      case FRAGMENT_SCANS -> "fragment_scans";
      case LIPID -> "lipid";
      case ION_IDENTITY_ID -> "ion_identity_id";
      case ION_TYPE -> "ion_type";
      case COMPOUND_NAME -> "compound_name";
    };
  }

  public @NotNull String getDescription() {
    return switch (this) {
      case ION_IDENTITY_ID -> "Filter for the ion identity network ID";
      case ION_TYPE -> "Filter for the ion type (adduct, in-source, and clusters) like [M+H]+";
      case COMPOUND_NAME -> "Filter for the compound name in any annotation";
      case IUPAC_NAME -> "Filter for the IUPAC name in any annotation";
      case FORMULA ->
          "Filter for the molecular formula like a direct match or minimum match like >C10Br2.";
      case FORMULA_RANGE ->
          "Filter for the molecular formula ranges by defining the minimum - maximum formula, e.g., C9Br - Br3 (only defines 9C and 1Br as minimum and 3Br as upper bound).";
      case SMILES ->
          "Filter for the SMILES structure or substructure matches, e.g., CNC=O for amides";
      case INCHI -> "Filter for the InChI structure or substructure matches.";
      case SMARTS -> "Filter for the SMARTS substructure matches, e.g., [#6]NC=O for amides";
      case LIPID -> """
          Filter for the lipid matches from rule-based annotation, e.g., C10:1 or PC18:2_18:0 or define thresholds like: PC>9:>2;>=1 for  >9C, >2 double bonds, and >=1 oxygen.
          'C' matches all lipids and is used as no class definition.
          'PC' matches the PC class without other constraints.
          'PC36:4' matches PC class with a chain total of 36 carbons and 4 double bonds.
          'PC>20:>=2' matches PC class with a chain total of >20 carbons and at least 2 double bonds.
          'PC>9:>2;>=1' also requires at least 1 oxygen.
          'C20:0 - C40:6' matches all lipids within range. Class is matched only for first entry.
          'C18:2_18:0;1O' matches lipid with specific chains separated by _ or /.""";
      case FRAGMENT_SCANS -> "Filter for the number of fragment scans";
    };
  }

  public boolean isNumeric() {
    return switch (this) {
      case FRAGMENT_SCANS, ION_IDENTITY_ID -> true;
      case FORMULA, SMILES, INCHI, SMARTS, LIPID, ION_TYPE, COMPOUND_NAME, FORMULA_RANGE,
           IUPAC_NAME -> false;
    };
  }

  /**
   * The first element of the list is the preferred {@link MatchingMode}
   *
   * @return list of matching modes applicable for option
   */
  public List<MatchingMode> getMatchingModes() {
    return switch (this) {
      case ION_IDENTITY_ID ->
          List.of(MatchingMode.EQUAL, MatchingMode.GREATER_EQUAL, MatchingMode.LESSER_EQUAL,
              MatchingMode.NOT_EQUAL);
      case FRAGMENT_SCANS ->
          List.of(MatchingMode.GREATER_EQUAL, MatchingMode.EQUAL, MatchingMode.LESSER_EQUAL,
              MatchingMode.NOT_EQUAL);
      case SMILES, INCHI ->
          List.of(MatchingMode.CONTAINS, MatchingMode.EQUAL, MatchingMode.NOT_EQUAL);
      case LIPID -> List.of(MatchingMode.CONTAINS, MatchingMode.EQUAL);
      case SMARTS -> List.of(MatchingMode.CONTAINS);
      case ION_TYPE, COMPOUND_NAME, IUPAC_NAME ->
          List.of(MatchingMode.EQUAL, MatchingMode.CONTAINS);
      case FORMULA ->
          List.of(MatchingMode.GREATER_EQUAL, MatchingMode.EQUAL, MatchingMode.LESSER_EQUAL);
      case FORMULA_RANGE -> List.of(MatchingMode.CONTAINS);
    };
  }

  /**
   * Prompt text shown in query text field
   */
  public String getQueryPromptText() {
    return switch (this) {
      case FORMULA_RANGE -> "C5Br - C9Br3";
      case LIPID -> "C10:1 or PC>9:>2;>1";
      default -> "";
    };
  }
}
