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
import org.jetbrains.annotations.NotNull;

/**
 * Recomputes derived value(s) on a {@link ModularCompoundRow} from its members. Analogous to
 * {@link io.github.mzmine.datamodel.features.RowBinding} but operating on a compound row instead of
 * a feature list row.
 * <p>
 * Every binding is exactly one of two kinds; {@link CompoundList} inspects the kind and wires the
 * appropriate change listeners so the owning compound is recomputed when an input changes:
 * <ul>
 *   <li>{@link CompoundRowMemberRowsBinding} — all member rows define the compound row value: a
 *   single row-level {@link DataType} is aggregated across
 *   the member rows and written back to the compound row (e.g. average RT).</li>
 *   <li>{@link ComplexCompoundRowBinding} — the compound row (or its compound features) is bound to
 *   feature-level data types: watches one or more feature/row types and may write several outputs
 *   (e.g. max intensity across samples, summed compound features).</li>
 * </ul>
 */
public sealed interface CompoundRowBinding permits CompoundRowMemberRowsBinding,
    ComplexCompoundRowBinding {

  /**
   * Recompute the derived value(s) for {@code compoundRow} from its members and store the result on
   * the compound row (and/or its compound features) via
   * {@link ModularCompoundRow#set(DataType, Object)}.
   */
  void apply(@NotNull ModularCompoundRow compoundRow);
}
