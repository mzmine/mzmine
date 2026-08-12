package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * How a {@link io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow}'s per-raw-file
 * intensity (height, area, normalized variants) is computed from its member rows.
 */
public enum CompoundIntensityRepresentation implements UniqueIdSupplier {

  /**
   * No compound-level features are stored. Per-raw-file intensity is read from the preferred
   * (representative) row's feature via the existing fallback in
   * {@link io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow#getFeature}.
   */
  REPRESENTATIVE,

  /**
   * For each raw file, sum the area, height, normalized area and normalized height across all
   * member rows' features and store the result as a synthesized compound feature.
   */
  SUM_ALL_MEMBERS,

  /**
   * Same as {@link #SUM_ALL_MEMBERS} but only contributions from members where
   * {@link io.github.mzmine.datamodel.features.FeatureListRow#hasIonIdentity()} is true are
   * included.
   */
  SUM_ION_IDENTITY_MEMBERS;

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case REPRESENTATIVE -> "representative";
      case SUM_ALL_MEMBERS -> "sum_all_members";
      case SUM_ION_IDENTITY_MEMBERS -> "sum_ion_identity_members";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case REPRESENTATIVE -> "Representative row";
      case SUM_ALL_MEMBERS -> "Sum of all members";
      case SUM_ION_IDENTITY_MEMBERS -> "Sum of members with ion identity";
    };
  }
}
