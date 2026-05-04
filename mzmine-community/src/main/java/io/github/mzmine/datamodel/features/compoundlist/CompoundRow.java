package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A compound row grouping multiple {@link FeatureListRow}s under a single compound identity.
 * Extends {@link FeatureListRow} so compound rows can be used wherever feature list rows are
 * expected. Compound-specific data types are stored in the row's own schema; all other
 * {@link FeatureListRow} values fall back to the preferred row via
 * {@link io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow#get}.
 */
public interface CompoundRow extends FeatureListRow {

  @NotNull FeatureListRow getPreferredRow();

  @NotNull List<CompoundFeatureMember> getCompoundMembers();

  float getCompoundConfidenceScore();

  @Nullable Double getCompoundNeutralMass();

  int getCompoundId();

  default int compoundSize() {
    return getCompoundMembers().size();
  }

  default @NotNull List<FeatureListRow> getMemberRows() {
    return getCompoundMembers().stream().map(CompoundFeatureMember::row).toList();
  }

  default @NotNull List<FeatureListRow> getMemberRowsByRole(@NotNull final CompoundMemberRole role) {
    return getCompoundMembers().stream()
        .filter(m -> m.role() == role).map(CompoundFeatureMember::row).toList();
  }
}
