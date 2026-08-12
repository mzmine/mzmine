package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import org.jetbrains.annotations.NotNull;

public record CompoundFeatureMember(
    @NotNull FeatureListRow row,
    @NotNull CompoundMemberRole role,
    float score) {
}
