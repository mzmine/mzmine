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

import io.github.mzmine.datamodel.features.types.DataType;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link CompoundRowBinding} that aggregates a single row-level {@link DataType} across a
 * {@link ModularCompoundRow}'s member rows and writes the result back to a single compound row type
 * (e.g. average RT). This is the common, constrained case: one primary input type, one output type,
 * optionally a few additional row-level inputs that should also re-trigger the binding.
 * <p>
 * For bindings that watch feature-level types or write multiple output types, implement
 * {@link ComplexCompoundRowBinding} instead.
 */
public non-sealed interface CompoundRowMemberRowsBinding extends CompoundRowBinding {

  /**
   * The primary {@link DataType} watched on member
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
   * row-level inputs. Watched on both the source rows and compound rows schemas.
   */
  default @NotNull List<DataType<?>> getAdditionalMemberRowTypes() {
    return List.of();
  }

  /**
   * Row types watched on the source feature list's rows: the primary {@link #getMemberRowType()}
   * plus any {@link #getAdditionalMemberRowTypes()}.
   */
  default @NotNull List<DataType<?>> getMemberRowTypes() {
    return combine(getMemberRowType(), getAdditionalMemberRowTypes());
  }

  /**
   * Row types watched on the compound rows schema (nested compound propagation): the primary
   * {@link #getCompoundRowType()} plus any {@link #getAdditionalMemberRowTypes()}.
   */
  default @NotNull List<DataType<?>> getCompoundRowTypes() {
    return combine(getCompoundRowType(), getAdditionalMemberRowTypes());
  }

  private static @NotNull List<DataType<?>> combine(@NotNull final DataType<?> primary,
      @NotNull final List<DataType<?>> additional) {
    if (additional.isEmpty()) {
      return List.of(primary);
    }
    final List<DataType<?>> all = new ArrayList<>(additional.size() + 1);
    all.add(primary);
    all.addAll(additional);
    return all;
  }
}
