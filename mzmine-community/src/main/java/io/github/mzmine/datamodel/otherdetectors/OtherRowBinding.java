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

package io.github.mzmine.datamodel.otherdetectors;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Computes a row-level value of an {@link OtherFeatureListRow} from its per-file {@link OtherFeature}s
 * (e.g. the average RT). Bindings are defined once on the {@link OtherFeatureList} and applied
 * explicitly after a row's features are set (during alignment and, later, gap filling) - a
 * lightweight analogue of the feature-list row bindings, without the columnar schema / listener
 * machinery. Each binding declares the row {@link #boundType()} it produces so the list can keep its
 * row-type index in sync automatically.
 */
public interface OtherRowBinding {

  /**
   * @return the row-level {@link DataType} this binding produces (registered as a row type on the
   * {@link OtherFeatureList}).
   */
  @NotNull DataType<?> boundType();

  /**
   * Recomputes and sets the bound row value from the row's per-file features.
   */
  void apply(@NotNull OtherFeatureListRow row);

  /**
   * The default row bindings: average RT and RT range.
   */
  static @NotNull List<OtherRowBinding> createDefault() {
    return List.of(averageRt(), rtRange());
  }

  /**
   * Row RT = mean of the per-file feature apex RTs (unset when the row has no RTs).
   */
  static @NotNull OtherRowBinding averageRt() {
    return new OtherRowBinding() {
      @Override
      public @NotNull DataType<?> boundType() {
        return DataTypes.get(RTType.class);
      }

      @Override
      public void apply(@NotNull final OtherFeatureListRow row) {
        float sum = 0f;
        int n = 0;
        for (final OtherFeature f : row.getFeatures()) {
          final Float rt = f.getRT();
          if (rt != null) {
            sum += rt;
            n++;
          }
        }
        row.set(RTType.class, n > 0 ? sum / n : null);
      }
    };
  }

  /**
   * Row RT range = union of the per-file feature RT ranges (apex point when a range is missing).
   */
  static @NotNull OtherRowBinding rtRange() {
    return new OtherRowBinding() {
      @Override
      public @NotNull DataType<?> boundType() {
        return DataTypes.get(RTRangeType.class);
      }

      @Override
      public void apply(@NotNull final OtherFeatureListRow row) {
        Range<Float> range = null;
        for (final OtherFeature f : row.getFeatures()) {
          final Float rt = f.getRT();
          if (rt == null) {
            continue;
          }
          final Range<Float> featureRange = f.getRtRange();
          final Range<Float> point = featureRange != null ? featureRange : Range.singleton(rt);
          range = range == null ? point : range.span(point);
        }
        row.set(RTRangeType.class, range);
      }
    };
  }
}
