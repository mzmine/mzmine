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
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link CompoundRowBinding} whose output is bound to feature-level {@link DataType}s rather than
 * a single member-row value. Watches one or more feature types on the source and/or compound
 * feature schemas — and optionally row types as additional re-aggregation triggers — and may write
 * several outputs in {@link #apply(ModularCompoundRow)} (e.g. the max intensity across samples, or
 * summed compound features).
 * <p>
 * This is the flexible, multi-listener counterpart to {@link CompoundRowMemberRowsBinding}: it
 * declares directly which types it watches across the four schemas, each defaulting to empty so an
 * implementation overrides only the ones it needs.
 */
public non-sealed interface ComplexCompoundRowBinding extends CompoundRowBinding {

  /**
   * Feature {@link DataType}s watched on the source feature list's features. When a member
   * feature's value of one of these types changes, the owning compound is recomputed.
   */
  default @NotNull List<DataType<?>> getMemberFeatureTypes() {
    return List.of();
  }

  /**
   * Feature {@link DataType}s watched on the compound features schema. Covers the nested case: when
   * an inner compound row's feature changes (after re-aggregation), the outer compound that owns it
   * is recomputed.
   */
  default @NotNull List<DataType<?>> getCompoundFeatureTypes() {
    return List.of();
  }

  /**
   * Optional row {@link DataType}s watched on the source feature list's rows. Use this for triggers
   * whose change should also re-run the aggregation (e.g. an ion-identity change that affects which
   * members contribute).
   */
  default @NotNull List<DataType<?>> getMemberRowTypes() {
    return List.of();
  }

  /**
   * Optional row {@link DataType}s watched on the compound rows schema (nested compound
   * propagation).
   */
  default @NotNull List<DataType<?>> getCompoundRowTypes() {
    return List.of();
  }
}
