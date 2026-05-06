package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Synthetic compound feature with per-{@link DataType} fallback to the representative source
 * feature. Stored in {@link CompoundList#getCompoundFeaturesSchema()} via the inherited
 * {@link ModularFeature#ModularFeature(CompoundList)} constructor. {@link #get(DataType)} returns
 * the compound feature's own value if set, otherwise the representative source row's feature
 * value for the same {@link RawDataFile}.
 */
public class ModularCompoundFeature extends ModularFeature implements CompoundFeature {

  @NotNull
  private final CompoundList compoundList;
  @NotNull
  private final ModularCompoundRow compoundRow;

  public ModularCompoundFeature(@NotNull final CompoundList compoundList,
      @NotNull final ModularCompoundRow compoundRow, RawDataFile raw) {
    super(compoundList);
    this.compoundList = compoundList;
    this.compoundRow = compoundRow;
    set(RawFileType.class, raw);
    set(DetectionType.class, FeatureStatus.COMPOUND_AGGREGATED);
  }

  @Override
  public @NotNull CompoundList getCompoundList() {
    return compoundList;
  }

  @Override
  public @NotNull ModularCompoundRow getCompoundRow() {
    return compoundRow;
  }

  @Override
  public @NotNull ModularFeatureListRow getRepresentativeRow() {
    return compoundRow.getCompoundMembersData().preferredRow();
  }

  /**
   * Compound-feature-level fallback: if the requested key is not set on this compound feature,
   * return the representative source row's feature value for the same {@link RawDataFile}.
   */
  @Override
  public <T> @Nullable T get(final DataType<T> key) {
    // compound feature only has some datatypes overwritten
    final T own = super.get(key);
    if (own != null) {
      return own;
    }
    // assumption: RawFileType is always set on a synthetic compound feature (see
    // CompoundIntensitySumBinding) — if absent, no fallback target can be resolved.
    final RawDataFile raw = super.get(RawFileType.class);
    if (raw == null) {
      return null;
    }
    // get other datatypes from representative row.feature
    final Feature repFeature = getRepresentativeRow().getFeature(raw);
    if (repFeature instanceof ModularFeature mf) {
      return mf.get(key);
    }
    return null;
  }
}
