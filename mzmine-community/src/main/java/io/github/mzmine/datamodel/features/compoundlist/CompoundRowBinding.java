package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.types.DataType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Aggregates a {@link DataType} value across a {@link ModularCompoundRow}'s member rows and writes
 * the result to the compound row. Analogous to
 * {@link io.github.mzmine.datamodel.features.RowBinding} but operating member rows -> compound row
 * instead of features -> row.
 */
public interface CompoundRowBinding {

  /**
   * The {@link DataType} watched on member
   * {@link io.github.mzmine.datamodel.features.FeatureListRow}s. When a member row's value of this
   * type changes, the compound row is recomputed.
   */
  @NotNull DataType<?> getMemberRowType();

  /**
   * The {@link DataType} written to the compound row. Often equal to {@link #getMemberRowType()}.
   */
  @NotNull DataType<?> getCompoundRowType();

  /**
   * Additional row-level {@link DataType}s whose changes on member rows should also trigger
   * {@link #apply(ModularCompoundRow)}. Use this for bindings whose result depends on multiple
   * row-level inputs (e.g. an intensity sum that filters by ion identity).
   */
  default @NotNull List<DataType<?>> getAdditionalMemberRowTypes() {
    return List.of();
  }

  /**
   * Member-feature {@link DataType}s whose changes should trigger
   * {@link #apply(ModularCompoundRow)}. Use this for bindings that aggregate feature-level values
   * (e.g. {@code AreaType}, {@code HeightType}). Listeners are wired on the source feature list's
   * features schema; the listener resolves the changed feature's row to the owning compound.
   */
  default @NotNull List<DataType<?>> getMemberFeatureTypes() {
    return List.of();
  }

  /**
   * Compound-feature {@link DataType}s that this binding writes to the compound feature schema.
   * Listeners are wired on the compound features schema so that when an inner compound row's
   * feature changes (after re-aggregation), the outer compound that owns it is recomputed.
   */
  default @NotNull List<DataType<?>> getCompoundFeatureTypes() {
    return List.of();
  }

  /**
   * Recompute the aggregate from {@link ModularCompoundRow#getMemberRows()} and store the result
   * on the compound row via {@link ModularCompoundRow#set(DataType, Object)}.
   */
  void apply(@NotNull ModularCompoundRow compoundRow);
}
