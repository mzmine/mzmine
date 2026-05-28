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

package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRowID;
import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMemberListType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundPreferredRowType;
import io.github.mzmine.util.io.JsonUtils;
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

  @NotNull
  public String toSimpleJson() {
    // serialize as JSON: preferred row (flat id), members (flat id + role + score), confidence
    final List<CompoundMemberDTO> memberJsons = members().stream()
        .map(m -> new CompoundMemberDTO(FeatureListRowID.of(m.row()))).toList();
    return JsonUtils.writeStringOrEmpty(
        new CompoundMembersDTO(FeatureListRowID.of(preferredRow()), memberJsons));
  }
}
