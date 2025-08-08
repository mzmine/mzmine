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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public enum RowTypeFilterOption implements UniqueIdSupplier {
  ION_IDENTITY_ID, ION_TYPE, COMPOUND_NAME, IUPAC_NAME, FORMULA, FORMULA_RANGE, SMILES, INCHI, SMARTS, LIPID, FRAGMENT_SCANS;


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
      case LIPID -> List.of(MatchingMode.CONTAINS, MatchingMode.EQUAL, MatchingMode.NOT_EQUAL);
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
      default -> "";
    };
  }
}
