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

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.identifiers.CASType;
import io.github.mzmine.datamodel.features.types.identifiers.InternalIdType;
import io.github.mzmine.datamodel.features.types.identifiers.IupacNameType;

/**
 * Describes the hierarchy of compound <b>name</b> identifiers in order of usable they are in
 * day-to-day use.
 * <p>
 * May add a "uniqueness" identifier in the future, that ranks by uniqueness.
 */
public enum CompoundNameIdentifier {
  COMPOUND_NAME, IUPAC_NAME, INTERNAL_ID, SMILES, INCHI, INCHI_KEY, FORMULA, CAS;

  public Class<? extends DataType<?>> getDataTypeClass() {
    return switch (this) {
      case COMPOUND_NAME -> CompoundNameType.class;
      case IUPAC_NAME -> IupacNameType.class;
      case INTERNAL_ID -> InternalIdType.class;
      case FORMULA -> FormulaType.class;
      case SMILES -> SmilesStructureType.class;
      case INCHI_KEY -> InChIKeyStructureType.class;
      case INCHI -> InChIStructureType.class;
      case CAS -> CASType.class;
    };
  }

  public DataType<?> getDataType() {
    return DataTypes.get(getDataTypeClass());
  }
}
