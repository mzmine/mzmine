package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMemberListType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundPreferredRowType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Carrier record for the membership state of a {@link ModularCompoundRow}: preferred row, full
 * member list (id+role+score per element), and confidence score. Acts as the single value behind
 * {@link CompoundMembersType}; sub-column lookups are dispatched by {@link #getValue(DataType)}.
 */
public record CompoundMembers(@NotNull ModularFeatureListRow preferredRow,
                              @NotNull List<CompoundFeatureMember> members,
                              float confidence) implements ModularDataRecord {

  public int size() {
    return members.size();
  }

  @Override
  public Object getValue(@NotNull final DataType<?> sub) {
    return switch (sub) {
      case CompoundPreferredRowType _ -> preferredRow;
      case CompoundConfidenceType _ -> confidence;
      case CompoundMemberListType _ -> members;
      default -> null;
    };
  }
}
