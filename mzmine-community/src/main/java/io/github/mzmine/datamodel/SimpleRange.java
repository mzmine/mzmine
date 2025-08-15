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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleIntegerRange;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight alternative to {@link Range} without cut values, which bloat RAM usage drastically.
 * Guava Range is a good option for computation tasks but never to keep in memory.
 * {@link SimpleRange} is always a closed range.
 *
 * @param <T>
 */
public sealed interface SimpleRange<T extends Comparable<?>> permits SimpleIntegerRange,
    SimpleDoubleRange {

  @NotNull
  public Range<T> guava();

  boolean contains(@NotNull T value);

  @NotNull T lowerBound();

  @NotNull T upperBound();

  /**
   * @return upper - lower
   */
  @NotNull T length();

  /**
   * Convenience method to convert a simple range to a guava range. Equivalent to
   * {@code simpleRange != null ? simpleRange.guava() : null}
   *
   * @param range The range to convert.
   * @param <T>   The type of the range (e.g. {@link Double} or {@link Integer}
   * @param <V>   The type of the {@link SimpleRange}.
   * @return The closed {@link Range} equivalent of the given range or null if the range is null.
   */
  static <T extends Comparable<?>, V extends SimpleRange<T>> Range<T> guavaOrNull(V range) {
    return range != null ? range.guava() : null;
  }

  @Nullable
  @Contract("null -> null")
  public static SimpleDoubleRange ofDouble(@Nullable Range<Double> range) {
    if (range == null) {
      return null;
    }

    if (Range.all().equals(range)) {
      return new SimpleDoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE);
    }

    return new SimpleDoubleRange(range.lowerEndpoint(), range.upperEndpoint());
  }

  @NotNull
  static SimpleDoubleRange ofDouble(double upper, double lower) {
    return of(upper, lower);
  }

  @NotNull
  static SimpleIntegerRange of(int lower, int upper) {
    return new SimpleIntegerRange(lower, upper);
  }

  @NotNull
  static SimpleDoubleRange of(double lower, double upper) {
    return new SimpleDoubleRange(lower, upper);
  }

  @Nullable
  @Contract("null -> null")
  public static SimpleIntegerRange ofInteger(@Nullable Range<Integer> range) {
    if (range == null) {
      return null;
    }

    if (Range.all().equals(range)) {
      return new SimpleIntegerRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    return new SimpleIntegerRange(range.lowerEndpoint(), range.upperEndpoint());
  }

  @NotNull
  static SimpleIntegerRange ofInteger(int lower, int upper) {
    return of(lower, upper);
  }

  record SimpleIntegerRange(int lower, int upper) implements SimpleRange<Integer> {

    @Override
    public @NotNull Range<Integer> guava() {
      return Range.closed(lower, upper);
    }

    @Override
    public @NotNull Integer lowerBound() {
      return lower;
    }

    @Override
    public @NotNull Integer upperBound() {
      return upper;
    }

    @Override
    public @NotNull Integer length() {
      return upper - lower;
    }

    @Override
    public boolean contains(@NotNull Integer value) {
      return lower <= value && value <= upper;
    }

    public boolean contains(int value) {
      return lower <= value && value <= upper;
    }
  }

  record SimpleDoubleRange(double lower, double upper) implements SimpleRange<Double> {

    @Override
    public @NotNull Range<Double> guava() {
      return Range.closed(lower, upper);
    }

    @Override
    public @NotNull Double lowerBound() {
      return lower;
    }

    @Override
    public @NotNull Double upperBound() {
      return upper;
    }

    @Override
    public @NotNull Double length() {
      return upper - lower;
    }

    @Override
    public boolean contains(@NotNull Double value) {
      return lower <= value && value <= upper;
    }

    public boolean contains(double value) {
      return lower <= value && value <= upper;
    }
  }
}
