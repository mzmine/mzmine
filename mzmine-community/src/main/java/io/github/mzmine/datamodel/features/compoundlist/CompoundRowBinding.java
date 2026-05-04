package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.types.DataType;
import org.jetbrains.annotations.NotNull;

/**
 * Aggregates a {@link DataType} value across a {@link ModularCompoundRow}'s member rows and writes
 * the result to the compound row. Analogous to
 * {@link io.github.mzmine.datamodel.features.RowBinding} but operating member rows -> compound row
 * instead of features -> row.
 */
public interface CompoundRowBinding {

  /**
   * The {@link DataType} watched on member {@link io.github.mzmine.datamodel.features.FeatureListRow}s.
   * When a member row's value of this type changes, the compound row is recomputed.
   */
  @NotNull DataType<?> getMemberRowType();

  /**
   * The {@link DataType} written to the compound row. Often equal to {@link #getMemberRowType()}.
   */
  @NotNull DataType<?> getCompoundRowType();

  /**
   * Recompute the aggregate from {@link ModularCompoundRow#getMemberRows()} and store the result
   * on the compound row via {@link ModularCompoundRow#set(DataType, Object)}.
   */
  void apply(@NotNull ModularCompoundRow compoundRow);
}
