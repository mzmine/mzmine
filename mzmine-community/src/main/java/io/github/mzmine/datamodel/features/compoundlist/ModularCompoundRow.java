package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundIdType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A compound row in a {@link CompoundList}. Extends {@link ModularFeatureListRow} so compound rows
 * can be passed wherever feature list rows are expected. Row data is stored in
 * {@link CompoundList#getCompoundRowSchema()} — not in the underlying feature list's row schema.
 * <p>
 * Membership state (preferred row, member list, confidence) lives inside a single
 * {@link CompoundMembers} carrier under {@link CompoundMembersType}. {@link #get(DataType)} returns
 * the compound row's own value first; if null it dispatches sub-column reads to the carrier (e.g.
 * {@link io.github.mzmine.datamodel.features.types.compoundlist.CompoundConfidenceType}); otherwise
 * it falls back to the preferred row's value.
 * <p>
 * {@code IDType} is intentionally never set on a compound row — the canonical compound identifier
 * is {@link CompoundIdType}, which lives in a separate id space from source-row {@code IDType}.
 */
public class ModularCompoundRow extends ModularFeatureListRow implements CompoundRow {

  private static final Logger logger = Logger.getLogger(ModularCompoundRow.class.getName());
  private static final CompoundMembersType COMPOUND_MEMBERS_TYPE = DataTypes.get(
      CompoundMembersType.class);

  @NotNull
  private final CompoundList compoundList;

  public ModularCompoundRow(@NotNull final CompoundList compoundList, final int compoundId,
      @NotNull final FeatureListRow preferredRow,
      @NotNull final List<CompoundFeatureMember> members, final float confidence,
      @Nullable final Double neutralMass) {
    // ModularFeatureListRow(flist, schema): schema = compound row schema; IDType is intentionally
    // not set so compound ids do not collide with source-row ids IdType is only for FeatureListRow IDs.
    super(compoundList.getFeatureList(), compoundList.getCompoundRowSchema());
    if (!(preferredRow instanceof ModularFeatureListRow mflr)) {
      throw new IllegalArgumentException(
          "preferredRow must be a ModularFeatureListRow — got " + preferredRow.getClass()
              .getName());
    }
    this.compoundList = compoundList;
    set(CompoundIdType.class, compoundId);
    set(CompoundMembersType.class, new CompoundMembers(mflr, List.copyOf(members), confidence));
    if (neutralMass != null) {
      set(NeutralMassType.class, neutralMass);
    }
  }

  /**
   * Returns the compound row's own value first; for sub-columns of {@link CompoundMembersType}
   * (preferred row id, confidence, members list) dispatches to the carrier; otherwise falls back to
   * the preferred row.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T get(final DataType<T> key) {
    final T own = super.get(key);
    if (own != null) {
      return own;
    }
    return getPreferredRow().get(key);
  }

  // -- Feature access via compoundRowSchema (not flist.getRowsSchema()) --
  // Per-RawDataFile fallback: if the compound row has no own feature for a raw file (or it is
  // UNKNOWN), the preferred row's feature for that raw file is returned instead.

  @Override
  public Stream<ModularFeature> streamFeatures() {
    final List<ModularFeature> own = compoundList.getCompoundRowSchema()
        .streamFeatures(modelRowIndex)
        .filter(f -> f.get(DetectionType.class) != FeatureStatus.UNKNOWN).toList();
    final Set<RawDataFile> covered = new HashSet<>(own.size());
    for (final ModularFeature f : own) {
      covered.add(f.getRawDataFile());
    }
    final Stream<ModularFeature> fallback = getPreferredRow().streamFeatures()
        .filter(f -> !covered.contains(f.getRawDataFile()));
    return Stream.concat(own.stream(), fallback);
  }

  @Override
  public @Nullable ModularFeature getFeature(final RawDataFile raw) {
    final ModularFeature own = compoundList.getCompoundRowSchema().getFeature(modelRowIndex, raw);
    if (own != null && own.getFeatureStatus() != FeatureStatus.UNKNOWN) {
      return own;
    }
    final Feature pref = getPreferredRow().getFeature(raw);
    return pref instanceof ModularFeature mf ? mf : null;
  }

  @Override
  public synchronized void addFeature(final RawDataFile raw, final Feature feature,
      final boolean updateByRowBindings) {
    if (feature == null) {
      compoundList.getCompoundRowSchema().setFeature(modelRowIndex, raw, null);
      return;
    }
    if (!(feature instanceof CompoundFeature)) {
      throw new IllegalArgumentException(
          "Compound rows accept CompoundFeature instances only — got " + feature.getClass()
              .getName());
    }
    if (!(feature instanceof ModularFeature mf)) {
      throw new IllegalArgumentException("Cannot add non-modular feature to compound row.");
    }
    if (!compoundList.getFeatureList().equals(feature.getFeatureList())) {
      throw new IllegalArgumentException(
          "Cannot add feature from a different feature list to this compound row.");
    }
    compoundList.getCompoundRowSchema().setFeature(modelRowIndex, raw, mf);
    mf.setRow(this);
    // assumption: compound rows are not part of the feature list event system — no event fired
  }

  @Override
  public void clearFeatures(final boolean updateByRowBindings) {
    compoundList.getCompoundRowSchema().clearFeatures(modelRowIndex);
  }

  // -- Type and listener resolution via compoundRowSchema --

  @Override
  @SuppressWarnings("rawtypes")
  public Set<DataType> getTypes() {
    return compoundList.getCompoundRowSchema().getTypes();
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return compoundList.getCompoundRowSchema().getValueChangeListeners();
  }

  // -- CompoundRow accessors --

  /**
   * @return the carrier holding preferred row + members + confidence. Always non-null for a
   * properly-constructed compound row.
   */
  public @NotNull CompoundMembers getCompoundMembersData() {
    return Objects.requireNonNull(super.get(COMPOUND_MEMBERS_TYPE),
        "compound row has no CompoundMembers carrier — invalid construction");
  }

  @Override
  public @NotNull FeatureListRow getPreferredRow() {
    return getCompoundMembersData().preferredRow();
  }

  @Override
  public int getCompoundId() {
    return getOrDefault(CompoundIdType.class, -1);
  }

  @Override
  public @NotNull List<CompoundFeatureMember> getCompoundMembers() {
    return getCompoundMembersData().members();
  }

  @Override
  public float getCompoundConfidenceScore() {
    return getCompoundMembersData().confidence();
  }

  @Override
  public @Nullable Double getCompoundNeutralMass() {
    return get(NeutralMassType.class);
  }

  @Override
  public @NotNull CompoundList getCompoundList() {
    return compoundList;
  }
}
