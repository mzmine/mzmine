package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundPreferredRowIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundSizeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import java.util.List;

/**
 * Canonical list of DataTypes registered as columnar schema columns in a {@link
 * io.github.mzmine.datamodel.features.compoundlist.CompoundList}.
 *
 * <p>{@link CompoundMembersType} is included so the column is pre-allocated in the schema, but it
 * is excluded from auto-column rendering by the {@link
 * io.github.mzmine.datamodel.features.types.modifiers.IgnoreAutoColumn} check in
 * {@code ModularDataModelColumnFactory}.
 */
public final class CompoundSchemaTypes {

  private CompoundSchemaTypes() {
  }

  public static final List<DataType<?>> REGISTERED = List.of(
      DataTypes.get(CompoundIdType.class),
      DataTypes.get(CompoundSizeType.class),
      DataTypes.get(CompoundPreferredRowIdType.class),
      DataTypes.get(CompoundConfidenceType.class),
      DataTypes.get(NeutralMassType.class),
      DataTypes.get(CompoundMembersType.class)
  );
}
