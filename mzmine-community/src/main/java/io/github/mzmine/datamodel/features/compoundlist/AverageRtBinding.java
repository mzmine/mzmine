package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Aggregates {@link RTType} as the arithmetic mean across all member rows of a
 * {@link ModularCompoundRow}. Member rows with a null RT are skipped. If no member has a non-null
 * RT, the compound row's RT is cleared (set to null).
 */
public final class AverageRtBinding implements CompoundRowBinding {

  private static final @NotNull RTType RT_TYPE = DataTypes.get(RTType.class);

  @Override
  public @NotNull DataType<?> getMemberRowType() {
    return RT_TYPE;
  }

  @Override
  public @NotNull DataType<?> getCompoundRowType() {
    return RT_TYPE;
  }

  @Override
  public void apply(@NotNull final ModularCompoundRow compoundRow) {
    final List<FeatureListRow> members = compoundRow.getMemberRows();
    double sum = 0.0;
    int count = 0;
    for (final FeatureListRow member : members) {
      final Float rt = member.get(RT_TYPE);
      if (rt != null) {
        sum += rt;
        count++;
      }
    }
    final Float average = count == 0 ? null : (float) (sum / count);
    compoundRow.set(RT_TYPE, average);
  }
}
