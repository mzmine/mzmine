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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
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
    SimpleDoubleRange, SimpleFloatRange {

  @NotNull
  public Range<T> guava();

  boolean contains(@NotNull T value);

  boolean isConnected(@NotNull SimpleRange<T> other);

  boolean isConnected(@NotNull Range<T> other);

  @NotNull T lowerBound();

  @NotNull T upperBound();

  /**
   * @return upper - lower, saturated at the MAX_VALUE of the type instead of overflowing. This
   * matters for the {@link Range#all()} substitute, which spans the whole domain of the type.
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

  /**
   * Open bounds are substituted by the MAX_VALUE of the respective type, see
   * {@link SimpleDoubleRange#of(Range)}.
   */
  @Nullable
  @Contract("null -> null")
  static SimpleDoubleRange ofDouble(@Nullable Range<Double> range) {
    return range == null ? null : SimpleDoubleRange.of(range);
  }

  @NotNull
  static SimpleDoubleRange ofDouble(double lower, double upper) {
    return of(lower, upper);
  }

  /**
   * Open bounds are substituted by the MAX_VALUE of the respective type, see
   * {@link SimpleFloatRange#of(Range)}.
   */
  @Nullable
  @Contract("null -> null")
  static SimpleFloatRange ofFloat(@Nullable Range<Float> range) {
    return range == null ? null : SimpleFloatRange.of(range);
  }

  @NotNull
  static SimpleFloatRange ofFloat(float lower, float upper) {
    return of(lower, upper);
  }

  /**
   * Open bounds are substituted by {@link Integer#MIN_VALUE}/{@link Integer#MAX_VALUE}, see
   * {@link SimpleIntegerRange#of(Range)}.
   */
  @Nullable
  @Contract("null -> null")
  static SimpleIntegerRange ofInteger(@Nullable Range<Integer> range) {
    return range == null ? null : SimpleIntegerRange.of(range);
  }

  @NotNull
  static SimpleIntegerRange ofInteger(int lower, int upper) {
    return of(lower, upper);
  }

  @NotNull
  static SimpleIntegerRange of(int lower, int upper) {
    return new SimpleIntegerRange(lower, upper);
  }

  @NotNull
  static SimpleFloatRange of(float lower, float upper) {
    return new SimpleFloatRange(lower, upper);
  }

  @NotNull
  static SimpleDoubleRange of(double lower, double upper) {
    return new SimpleDoubleRange(lower, upper);
  }

  record SimpleIntegerRange(int lower, int upper) implements SimpleRange<Integer> {

    /**
     * Open bounds are substituted by {@link Integer#MIN_VALUE}/{@link Integer#MAX_VALUE}.
     */
    @NotNull
    public static SimpleIntegerRange of(@NotNull Range<Integer> r) {
      return new SimpleIntegerRange(
          r.hasLowerBound() ? r.lowerEndpoint() : Integer.MIN_VALUE,
          r.hasUpperBound() ? r.upperEndpoint() : Integer.MAX_VALUE);
    }

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
      // the substitute for Range.all() spans the whole int domain and would wrap around,
      // saturate at MAX_VALUE instead of reporting a negative length
      return Math.clamp((long) upper - (long) lower, 0, Integer.MAX_VALUE);
    }

    @Override
    public boolean contains(@NotNull Integer value) {
      return lower <= value && value <= upper;
    }

    @Override
    public boolean isConnected(@NotNull SimpleRange<Integer> other) {
      if (contains(other.lowerBound()) || contains(other.upperBound())) {
        // simple overlap
        return true;
      }
      if (lower < other.lowerBound() && upper > other.upperBound()) {
        // this range encloses the other range
        return true;
      }
      if (other.lowerBound() < lower && other.upperBound() > upper) {
        // other range encloses this range
        return true;
      }
      return false;
    }

    @Override
    public boolean isConnected(@NotNull Range<Integer> other) {
      return SimpleIntegerRange.of(other).isConnected(this);
    }

    public boolean contains(int value) {
      return lower <= value && value <= upper;
    }
  }

  record SimpleDoubleRange(double lower, double upper) implements SimpleRange<Double> {

    /**
     * Open bounds are substituted by -{@link Double#MAX_VALUE}/{@link Double#MAX_VALUE}. Note that
     * the substitute does not contain the infinities or NaN, unlike {@link Range#all()}.
     */
    @NotNull
    public static SimpleDoubleRange of(@NotNull Range<Double> r) {
      return new SimpleDoubleRange(r.hasLowerBound() ? r.lowerEndpoint() : -Double.MAX_VALUE,
          r.hasUpperBound() ? r.upperEndpoint() : Double.MAX_VALUE);
    }

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
      final double length = upper - lower;
      // the substitute for Range.all() spans -MAX_VALUE to MAX_VALUE and overflows to infinity,
      // saturate at MAX_VALUE instead. an actually infinite bound keeps its infinite length.
      if (Double.isInfinite(length) && Double.isFinite(lower) && Double.isFinite(upper)) {
        return Double.MAX_VALUE;
      }
      return length;
    }

    @Override
    public boolean contains(@NotNull Double value) {
      return lower <= value && value <= upper;
    }

    @Override
    public boolean isConnected(@NotNull SimpleRange<Double> other) {
      if (contains(other.lowerBound()) || contains(other.upperBound())) {
        // simple overlap
        return true;
      }
      if (lower < other.lowerBound() && upper > other.upperBound()) {
        // this range encloses the other range
        return true;
      }
      if (other.lowerBound() < lower && other.upperBound() > upper) {
        // other range encloses this range
        return true;
      }
      return false;
    }

    @Override
    public boolean isConnected(@NotNull Range<Double> other) {
      return SimpleDoubleRange.of(other).isConnected(this);
    }

    public boolean contains(double value) {
      return lower <= value && value <= upper;
    }
  }

  record SimpleFloatRange(float lower, float upper) implements SimpleRange<Float> {

    /**
     * Open bounds are substituted by -{@link Float#MAX_VALUE}/{@link Float#MAX_VALUE}. Note that
     * the substitute does not contain the infinities or NaN, unlike {@link Range#all()}.
     */
    @NotNull
    public static SimpleFloatRange of(@NotNull Range<Float> r) {
      return new SimpleFloatRange(r.hasLowerBound() ? r.lowerEndpoint() : -Float.MAX_VALUE,
          r.hasUpperBound() ? r.upperEndpoint() : Float.MAX_VALUE);
    }

    @Override
    public @NotNull Range<Float> guava() {
      return Range.closed(lower, upper);
    }

    @Override
    public @NotNull Float lowerBound() {
      return lower;
    }

    @Override
    public @NotNull Float upperBound() {
      return upper;
    }

    @Override
    public @NotNull Float length() {
      final float length = upper - lower;
      // the substitute for Range.all() spans -MAX_VALUE to MAX_VALUE and overflows to infinity,
      // saturate at MAX_VALUE instead. an actually infinite bound keeps its infinite length.
      if (Float.isInfinite(length) && Float.isFinite(lower) && Float.isFinite(upper)) {
        return Float.MAX_VALUE;
      }
      return length;
    }

    @Override
    public boolean contains(@NotNull Float value) {
      return lower <= value && value <= upper;
    }

    @Override
    public boolean isConnected(@NotNull SimpleRange<Float> other) {
      if (contains(other.lowerBound()) || contains(other.upperBound())) {
        // simple overlap
        return true;
      }
      if (lower < other.lowerBound() && upper > other.upperBound()) {
        // this range encloses the other range
        return true;
      }
      if (other.lowerBound() < lower && other.upperBound() > upper) {
        // other range encloses this range
        return true;
      }
      return false;
    }

    @Override
    public boolean isConnected(@NotNull Range<Float> other) {
      return SimpleFloatRange.of(other).isConnected(this);
    }

    public boolean contains(float value) {
      return lower <= value && value <= upper;
    }
  }
}
