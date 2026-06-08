/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

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
public final class AverageRtBinding implements CompoundRowMemberRowsBinding {

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
