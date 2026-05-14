package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMemberListType;
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
      DataTypes.get(CompoundMemberListType.class) //
  );

  public static boolean isCompoundOwned(@NotNull final DataType<?> type) {
    return COMPOUND_OWNED.contains(type);
  }
}
