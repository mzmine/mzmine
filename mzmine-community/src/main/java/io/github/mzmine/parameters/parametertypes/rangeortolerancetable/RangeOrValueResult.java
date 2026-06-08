/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.rangeortolerancetable;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.parametertypes.tolerances.NanometerTolerance;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RangeOrValueResult<T extends Number & Comparable<T>>(
    @NotNull List<RangeOrValue<T>> ranges, @Nullable Tolerance<T> tolerance) {

  public static RangeOrValueResult<Double> emptyNanometer() {
    return new RangeOrValueResult<>(List.of(), new NanometerTolerance(1d));
  }

  public RangeOrValueResult<T> copy() {
    final List<RangeOrValue<T>> copy = ranges.stream()
        .map(r -> new RangeOrValue<>(r.getLower(), r.getUpper())).toList();
    return new RangeOrValueResult<>(copy, tolerance);
  }

  public List<@NotNull Range<T>> toGuava() {
    return ranges.stream().map(r -> r.getRange(tolerance)).filter(Objects::nonNull).toList();
  }

  boolean valid() {
    return tolerance != null && ranges.stream().allMatch(RangeOrValue::isValid);
  }
}
