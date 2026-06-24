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

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the per-{@link ModularCompoundRow} row-level {@link AreaType}, {@link HeightType},
 * {@link NormalizedAreaType} and {@link NormalizedHeightType} as the maximum of the corresponding
 * values across the compound row's features (one feature per sample). Mirrors the feature -> row
 * {@code MAX} aggregation that {@link io.github.mzmine.datamodel.features.SimpleRowBinding}
 * performs for a regular {@link io.github.mzmine.datamodel.features.ModularFeatureListRow}, but for
 * the compound row's own (compound) features rather than the source feature list's features.
 * <p>
 * Reads via {@link ModularCompoundRow#streamFeatures()} which returns the compound row's own
 * compound features when set
 * ({@link
 * io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation.CompoundIntensityRepresentation}
 * SUM modes) and falls back to the preferred row's source features otherwise (REPRESENTATIVE
 * mode).
 * <p>
 * Listens to the same feature-level types both on the source feature list (via
 * {@link #getMemberFeatureTypes()}) and on the compound features schema (via
 * {@link #getCompoundFeatureTypes()}) so changes propagate through nested compounds. Listener
 * registration order in {@link CompoundList#wireListeners()} relies on this binding being placed
 * <em>after</em> {@link CompoundIntensitySumBinding} in
 * {@link CompoundRowBindings#defaultBindings()} so that compound features have already been
 * rewritten when this binding reads them.
 */
public final class MaxFeatureIntensityRowBinding implements ComplexCompoundRowBinding {

  private static final @NotNull AreaType AREA_TYPE = DataTypes.get(AreaType.class);
  private static final @NotNull HeightType HEIGHT_TYPE = DataTypes.get(HeightType.class);
  private static final @NotNull NormalizedAreaType NORMALIZED_AREA_TYPE = DataTypes.get(
      NormalizedAreaType.class);
  private static final @NotNull NormalizedHeightType NORMALIZED_HEIGHT_TYPE = DataTypes.get(
      NormalizedHeightType.class);

  private static final List<DataType<?>> INTENSITY_TYPES = List.of(AREA_TYPE, HEIGHT_TYPE,
      NORMALIZED_AREA_TYPE, NORMALIZED_HEIGHT_TYPE);

  @Override
  public @NotNull List<DataType<?>> getMemberFeatureTypes() {
    // source-feature changes need to refresh the row-level max after CompoundIntensitySumBinding
    // has rewritten the compound features (listener order is dispatch order = registration order)
    return INTENSITY_TYPES;
  }

  @Override
  public @NotNull List<DataType<?>> getCompoundFeatureTypes() {
    // nested case: when an inner compound's feature is recomputed, the outer compound that owns it
    // re-aggregates its compound features (CompoundIntensitySumBinding), and then this binding
    // refreshes the outer compound's row-level max.
    return INTENSITY_TYPES;
  }

  @Override
  public void apply(@NotNull final ModularCompoundRow compoundRow) {
    final List<ModularFeature> features = compoundRow.streamFeatures().toList();
    compoundRow.set(AREA_TYPE, maxOf(features, AREA_TYPE));
    compoundRow.set(HEIGHT_TYPE, maxOf(features, HEIGHT_TYPE));
    compoundRow.set(NORMALIZED_AREA_TYPE, maxOf(features, NORMALIZED_AREA_TYPE));
    compoundRow.set(NORMALIZED_HEIGHT_TYPE, maxOf(features, NORMALIZED_HEIGHT_TYPE));
  }

  private static @Nullable Float maxOf(@NotNull final List<ModularFeature> features,
      @NotNull final DataType<Float> type) {
    Float max = null;
    for (final ModularFeature f : features) {
      final Float v = f.get(type);
      if (v != null && (max == null || v > max)) {
        max = v;
      }
    }
    return max;
  }
}
