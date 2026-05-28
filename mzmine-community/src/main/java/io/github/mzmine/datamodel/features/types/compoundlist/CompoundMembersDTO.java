package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRowID;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * JSON DTO for {@link CompoundMembersType#getFormattedString}: a flat reference to the preferred
 * row, the full member list, and the compound confidence score.
 */
record CompoundMembersDTO(@NotNull FeatureListRowID preferredRow,
                          @NotNull List<CompoundMemberDTO> members
// for now do not export confidence. Only once we can explain them better as this might confuse users
//                          ,float confidence
) {

}
