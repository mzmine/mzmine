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

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.SmartsEductStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmartsStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesIsomericStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public enum RowTypeFilterOption implements UniqueIdSupplier {
  ION_IDENTITY_ID, ION_TYPE, COMPOUND_NAME, FORMULA, SMILES, INCHI, SMARTS, LIPID, FRAGMENT_SCANS;


  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case FORMULA -> "formula";
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
      case FORMULA, SMILES, INCHI, SMARTS, LIPID, ION_TYPE, COMPOUND_NAME -> false;
    };
  }

  public List<MatchingMode> getMatchingModes() {
    return switch (this) {
      case ION_IDENTITY_ID, FRAGMENT_SCANS ->
          List.of(MatchingMode.EQUAL, MatchingMode.GREATER_EQUAL, MatchingMode.LESSER_EQUAL,
              MatchingMode.NOT_EQUAL);
      case FORMULA, SMILES, INCHI, SMARTS, LIPID ->
          List.of(MatchingMode.EQUAL, MatchingMode.GREATER_EQUAL, MatchingMode.NOT_EQUAL);
      case ION_TYPE, COMPOUND_NAME -> List.of(MatchingMode.EQUAL, MatchingMode.CONTAINS);
    };
  }


  /**
   * DataTypes that may contain the asked for information
   *
   * @return
   */
  public @NotNull List<DataType> getDataTypes() {
    return switch (this) {
      case FORMULA -> DataTypes.getAll(FormulaType.class, FormulaListType.class);
      case SMILES -> DataTypes.getAll(SmilesStructureType.class, SmilesIsomericStructureType.class);
      case INCHI -> DataTypes.getAll(InChIStructureType.class);
      case SMARTS -> DataTypes.getAll(SmartsStructureType.class, SmartsEductStructureType.class);
      case FRAGMENT_SCANS -> DataTypes.getAll(FragmentScanNumbersType.class);
      case LIPID -> DataTypes.getAll(LipidMatchListType.class);
      case ION_IDENTITY_ID -> DataTypes.getAll(IonIdentityListType.class);
      case ION_TYPE ->
          DataTypes.getAll(IonIdentityListType.class, IonTypeType.class, IonAdductType.class);
      case COMPOUND_NAME -> DataTypes.getAll(CompoundNameType.class);
    };
  }
}
