package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundPreferredRowIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundSizeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A compound row in a {@link CompoundList}. Extends {@link ModularFeatureListRow} so compound rows
 * can be passed wherever feature list rows are expected. Row data is stored in
 * {@link CompoundList#getCompoundRowSchema()} — not in the underlying feature list's row schema.
 * Calling {@link #get(DataType)} returns the compound row's own value first; if null it falls back
 * to the preferred row's value.
 */
public class ModularCompoundRow extends ModularFeatureListRow implements CompoundRow {

  private static final Logger logger = Logger.getLogger(ModularCompoundRow.class.getName());

  @NotNull private final CompoundList owner;
  @NotNull private final FeatureListRow preferredRow;

  public ModularCompoundRow(@NotNull final CompoundList owner,
      final int compoundId,
      @NotNull final FeatureListRow preferredRow,
      @NotNull final List<CompoundFeatureMember> members,
      final float confidence,
      @Nullable final Double neutralMass) {
    // Protected ModularFeatureListRow constructor: flist = source feature list, schema = compound row schema
    super(owner.getFeatureList(), owner.getCompoundRowSchema(), compoundId);
    this.owner = owner;
    this.preferredRow = preferredRow;
    set(CompoundIdType.class, compoundId);
    set(CompoundPreferredRowIdType.class, preferredRow.getID());
    set(CompoundMembersType.class, List.copyOf(members));
    set(CompoundSizeType.class, members.size());
    set(CompoundConfidenceType.class, confidence);
    if (neutralMass != null) {
      set(NeutralMassType.class, neutralMass);
    }
  }

  /**
   * Returns the compound row's own value first; falls back to the preferred row for any type not
   * explicitly set on this compound row.
   */
  @Override
  public <T> @Nullable T get(DataType<T> key) {
    T ownValue = super.get(key);
    if (ownValue != null) {
      return ownValue;
    }
    return preferredRow.get(key);
  }

  // -- Feature access via compoundRowSchema (not flist.getRowsSchema()) --
  // Per-RawDataFile fallback: if the compound row has no own feature for a raw file (or it is
  // UNKNOWN), the preferred row's feature for that raw file is returned instead.

  @Override
  public Stream<ModularFeature> streamFeatures() {
    final List<ModularFeature> own = owner.getCompoundRowSchema().streamFeatures(modelRowIndex)
        .filter(f -> f.get(DetectionType.class) != FeatureStatus.UNKNOWN).toList();
    final Set<RawDataFile> covered = new HashSet<>(own.size());
    for (final ModularFeature f : own) {
      covered.add(f.getRawDataFile());
    }
    final Stream<ModularFeature> fallback = preferredRow.streamFeatures()
        .filter(f -> !covered.contains(f.getRawDataFile()));
    return Stream.concat(own.stream(), fallback);
  }

  @Override
  public @Nullable ModularFeature getFeature(RawDataFile raw) {
    final ModularFeature own = owner.getCompoundRowSchema().getFeature(modelRowIndex, raw);
    if (own != null && own.getFeatureStatus() != FeatureStatus.UNKNOWN) {
      return own;
    }
    final Feature pref = preferredRow.getFeature(raw);
    return pref instanceof ModularFeature mf ? mf : null;
  }

  @Override
  public synchronized void addFeature(RawDataFile raw, Feature feature,
      boolean updateByRowBindings) {
    if (feature == null) {
      owner.getCompoundRowSchema().setFeature(modelRowIndex, raw, null);
      return;
    }
    if (!(feature instanceof ModularFeature mf)) {
      throw new IllegalArgumentException(
          "Cannot add non-modular feature to compound row.");
    }
    if (!owner.getFeatureList().equals(feature.getFeatureList())) {
      throw new IllegalArgumentException(
          "Cannot add feature from a different feature list to this compound row.");
    }
    owner.getCompoundRowSchema().setFeature(modelRowIndex, raw, mf);
    mf.setRow(this);
    // assumption: compound rows are not part of the feature list event system — no event fired
  }

  @Override
  public void clearFeatures(boolean updateByRowBindings) {
    owner.getCompoundRowSchema().clearFeatures(modelRowIndex);
  }

  // -- Type and listener resolution via compoundRowSchema --

  @Override
  public Set<DataType> getTypes() {
    return owner.getCompoundRowSchema().getTypes();
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return owner.getCompoundRowSchema().getValueChangeListeners();
  }

  // -- CompoundRow accessors --

  @Override
  public @NotNull FeatureListRow getPreferredRow() {
    return preferredRow;
  }

  @Override
  public int getCompoundId() {
    return getOrDefault(CompoundIdType.class, -1);
  }

  @Override
  public @NotNull List<CompoundFeatureMember> getCompoundMembers() {
    return getNonNullElse(CompoundMembersType.class, List.of());
  }

  @Override
  public float getCompoundConfidenceScore() {
    return getOrDefault(CompoundConfidenceType.class, 0f);
  }

  @Override
  public @Nullable Double getCompoundNeutralMass() {
    return get(NeutralMassType.class);
  }
}
