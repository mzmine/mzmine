package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CompoundRow extends ModularDataModel {

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

  // Delegating convenience accessors — not DataType-backed, avoid round-trip through schema
  default @Nullable Double getAverageMZ() {
    return getPreferredRow().getAverageMZ();
  }

  default @Nullable Float getAverageRT() {
    return getPreferredRow().getAverageRT();
  }

  default @Nullable IonIdentity getBestIonIdentity() {
    return getPreferredRow().getBestIonIdentity();
  }
}
