package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
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

  /**
   * @return the preferred member row which may be a classic FeatureListRow or a {@link CompoundRow}
   * with its isotope rows
   */
  @NotNull FeatureListRow getPreferredRow();

  /**
   * @return all member rows
   */
  @NotNull List<CompoundFeatureMember> getCompoundMembers();

  float getCompoundConfidenceScore();

  @Nullable Double getCompoundNeutralMass();

  /**
   * The compound ID is different from the {@link IDType} id used by regular {@link FeatureListRow}.
   * A compound row will only link to the preferred row.getID in their getID method.
   *
   * @return a compound ID
   */
  int getCompoundId();

  /**
   * @return direct member number (this does not include eventual second level children like isotopes.)
   */
  default int compoundSize() {
    return getCompoundMembers().size();
  }

  default @NotNull List<FeatureListRow> getMemberRows() {
    return getCompoundMembers().stream().map(CompoundFeatureMember::row).toList();
  }

  default @NotNull List<FeatureListRow> getMemberRowsByRole(
      @NotNull final CompoundMemberRole role) {
    return getCompoundMembers().stream().filter(m -> m.role() == role)
        .map(CompoundFeatureMember::row).toList();
  }

  @NotNull CompoundList getCompoundList();
}
