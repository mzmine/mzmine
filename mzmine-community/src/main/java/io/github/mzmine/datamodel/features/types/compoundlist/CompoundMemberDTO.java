package io.github.mzmine.datamodel.features.types.compoundlist;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.mzmine.datamodel.features.FeatureListRowID;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import org.jetbrains.annotations.NotNull;

/**
 * JSON DTO for a single compound member: a flat row reference, its role inside the compound, and
 * the member score. Used by {@link CompoundMembersType#getFormattedString} for export.
 * <p>
 * {@code row} is unwrapped, so the {@link FeatureListRowID} components ({@code mode}, {@code id})
 * are emitted directly next to {@code role} and {@code score}.
 */
record CompoundMemberDTO(@JsonUnwrapped @NotNull FeatureListRowID row,
                         @NotNull CompoundMemberRole role, float score) {

}
