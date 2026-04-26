package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelRow;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundPreferredRowIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundSizeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A compound row in a {@link CompoundList}. Extends {@link ColumnarModularDataModelRow} directly —
 * not a subtype of {@link io.github.mzmine.datamodel.features.ModularFeatureListRow}. Has no
 * features, no raw data files, and does not participate in the feature list row-change event system.
 */
public class ModularCompoundRow extends ColumnarModularDataModelRow implements CompoundRow {

  private static final Logger logger = Logger.getLogger(ModularCompoundRow.class.getName());

  @NotNull private final FeatureListRow preferredRow;
  @NotNull private final CompoundList owner;

  public ModularCompoundRow(@NotNull final CompoundList owner,
      final int compoundId,
      @NotNull final FeatureListRow preferredRow,
      @NotNull final List<CompoundFeatureMember> members,
      final float confidence,
      @Nullable final Double neutralMass) {
    super(owner.getSchema());
    this.owner = owner;
    this.preferredRow = preferredRow;
    set(CompoundIdType.class, compoundId);
    set(CompoundPreferredRowIdType.class, preferredRow.getID());
    set(CompoundMembersType.class, List.copyOf(members));
    set(CompoundSizeType.class, members.size());
    set(CompoundConfidenceType.class, confidence);
    if (neutralMass != null) {
      set(NeutralMassType.class, neutralMass);
    }
  }

  @Override
  public @NotNull FeatureListRow getPreferredRow() {
    return preferredRow;
  }

  @Override
  public int getCompoundId() {
    return getOrDefault(CompoundIdType.class, -1);
  }

  @Override
  public @NotNull List<CompoundFeatureMember> getCompoundMembers() {
    return getNonNullElse(CompoundMembersType.class, List.of());
  }

  @Override
  public float getCompoundConfidenceScore() {
    return getOrDefault(CompoundConfidenceType.class, 0f);
  }

  @Override
  public @Nullable Double getCompoundNeutralMass() {
    return get(NeutralMassType.class);
  }
}
