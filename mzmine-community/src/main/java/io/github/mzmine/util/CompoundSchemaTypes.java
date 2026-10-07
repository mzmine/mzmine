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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMemberListType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersJsonType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundPreferredRowType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Canonical lists of DataTypes related to a
 * {@link io.github.mzmine.datamodel.features.compoundlist.CompoundList}.
 *
 * <ul>
 *   <li>{@link #REGISTERED}: top-level columns pre-allocated on the compound row schema.</li>
 *   <li>{@link #COMPOUND_OWNED}: full set of compound-only DataTypes (top-level + sub-columns
 *       under {@link CompoundMembersType}). Used to gate compound-aware UI logic.</li>
 * </ul>
 * <p>
 * {@link CompoundMembersType} is included in {@link #REGISTERED} so the column is pre-allocated in
 * the schema.
 */
public final class CompoundSchemaTypes {

  private CompoundSchemaTypes() {
  }

  public static final List<DataType<?>> REGISTERED = List.of( //
      DataTypes.get(CompoundIdType.class), //
      DataTypes.get(CompoundMembersType.class), //
      DataTypes.get(CompoundMembersJsonType.class), //
      DataTypes.get(NeutralMassType.class) //
  );

  /**
   * Compound-only DataTypes — top-level columns plus sub-columns carried inside
   * {@link CompoundMembersType}. Use {@link #isCompoundOwned(DataType)} for membership checks.
   */
  private static final Set<DataType<?>> COMPOUND_OWNED = Set.of( //
      DataTypes.get(CompoundIdType.class), //
      DataTypes.get(CompoundMembersType.class), //
      DataTypes.get(NeutralMassType.class), //
      DataTypes.get(CompoundPreferredRowType.class), //
      DataTypes.get(CompoundConfidenceType.class), //
      DataTypes.get(CompoundMemberListType.class), //
      DataTypes.get(CompoundMembersJsonType.class) //
  );

  public static boolean isCompoundOwned(@NotNull final DataType<?> type) {
    return COMPOUND_OWNED.contains(type);
  }
}
