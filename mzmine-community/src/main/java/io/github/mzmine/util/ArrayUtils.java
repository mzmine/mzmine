/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import javax.validation.constraints.Null;
import org.jetbrains.annotations.Nullable;

public class ArrayUtils {

  /**
   * Throws out of range exceptions
   *
   * @param arrayLength
   * @param fromIndex
   * @param toIndex
   */
  public static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
    }
    if (fromIndex < 0) {
      throw new ArrayIndexOutOfBoundsException(fromIndex);
    }
    if (toIndex > arrayLength) {
      throw new ArrayIndexOutOfBoundsException(toIndex);
    }
  }

  public static boolean contains(Object needle, Object[] haystack) {
    for (final Object o : haystack) {
      if (Objects.equals(o, needle)) {
        return true;
      }
    }
    return false;
  }

  public static <T> int indexOf(T needle, T[] haystack) {
    for (int i = 0; i < haystack.length; i++) {
      if (Objects.equals(haystack[i], needle)) {
        return i;
      }
    }
    return -1;
  }

  public static int indexOf(double needle, double[] haystack) {
    for (int i = 0; i < haystack.length; i++) {
      if (Double.compare(haystack[i], needle) == 0) {
        return i;
      }
    }
    return -1;
  }

  public static double sum(double[] array, int startIndex, int endIndex) {
    assert endIndex < array.length;
    assert startIndex >= 0;

    double sum = 0d;
    for (int i = startIndex; i < endIndex; i++) {
      sum += array[i];
    }
    return sum;
  }

  public static double sum(double[] array) {
    if (array == null) {
      return 0d;
    }
    double sum = 0d;
    for (double v : array) {
      sum += v;
    }
    return sum;
  }

  public static float sum(float[] array) {
    if (array == null) {
      return 0f;
    }
    float sum = 0f;
    for (float v : array) {
      sum += v;
    }
    return sum;
  }

  /**
   * @see #smallestDelta(double[], int)
   */
  public static double smallestDelta(double[] values) {
    return smallestDelta(values, values.length);
  }

  /**
   * @param values    the values. sorted ascending/descending
   * @param numValues
   * @return The smallest delta between two neighbouring values. 0 if values contains 0 or 1 value.
   */
  public static double smallestDelta(double[] values, int numValues) {
    if (values.length <= 1) {
      return 0d;
    }

    double prevValue = values[0];
    double smallestDelta = Double.POSITIVE_INFINITY;
    for (int i = 1; i < numValues; i++) {
      final double delta = Math.abs(values[i] - prevValue);
      if (Double.compare(0d, delta) != 0 && delta < smallestDelta) {
        smallestDelta = delta;
      }
      prevValue = values[i];
    }

    return smallestDelta;
  }

  public static void fill2D(double[][] array, double value) {
    for (double[] doubles : array) {
      Arrays.fill(doubles, value);
    }
  }

  /**
   * Reverses the given array.
   *
   * @param input The array.
   */
  public static void reverse(int[] input) {
    int last = input.length - 1;
    int middle = input.length / 2;
    for (int i = 0; i <= middle; i++) {
      int temp = input[i];
      input[i] = input[last - i];
      input[last - i] = temp;
    }
  }

  public static void reverse(double[] input) {
    int last = input.length - 1;
    int middle = input.length / 2;
    for (int i = 0; i <= middle; i++) {
      double temp = input[i];
      input[i] = input[last - i];
      input[last - i] = temp;
    }
  }

  public static double lastElement(double[] array) {
    assert array.length > 0;
    return array[array.length - 1];
  }

  /**
   * Concatenate multiple arrays into one long array
   *
   * @param source       list of data sources of double[]
   * @param dataSupplier function to extract the double[] from source
   * @param <T>
   * @return a long double array
   */
  public static <T> double[] concat(final List<T> source,
      final Function<T, double[]> dataSupplier) {
    List<double[]> sourceData = new ArrayList<>(source.size());
    for (final T s : source) {
      sourceData.add(dataSupplier.apply(s));
    }
    return concat(sourceData);
  }

  /**
   * Concatenate multiple arrays into one long array
   *
   * @param sourceData list of data, order is preserved
   * @return one long array
   */
  public static double[] concat(final List<double[]> sourceData) {
    int numDp = sourceData.stream().mapToInt(a -> a.length).sum();
    final double[] result = new double[numDp];
    int offset = 0;
    for (final double[] data : sourceData) {
      System.arraycopy(data, 0, result, offset, data.length);
    }
    return result;
  }

  public static float[] doubleToFloat(double[] values) {
    float[] converted = new float[values.length];
    for (int i = 0; i < values.length; i++) {
      converted[i] = (float) values[i];
    }
    return converted;
  }

  public static double[] floatToDouble(float[] values) {
    double[] converted = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      converted[i] = (double) values[i];
    }
    return converted;
  }

  /**
   * Max of any value extracted from values
   *
   * @return Optional maximum value
   */
  public static <T> OptionalDouble max(final T[] values, final ToDoubleFunction<T> extractor) {
    if (values == null || values.length == 0) {
      return OptionalDouble.empty();
    }
    if (values.length == 1) {
      return OptionalDouble.of(extractor.applyAsDouble(values[0]));
    }
    double max = Double.NEGATIVE_INFINITY;
    for (T value : values) {
      final double v = extractor.applyAsDouble(value);
      if (v > max) {
        max = v;
      }
    }
    return OptionalDouble.of(max);
  }

  /**
   * min of any value extracted from values
   *
   * @return Optional minimum value
   */
  public static <T> OptionalDouble min(final T[] values, final ToDoubleFunction<T> extractor) {
    if (values == null || values.length == 0) {
      return OptionalDouble.empty();
    }
    if (values.length == 1) {
      return OptionalDouble.of(extractor.applyAsDouble(values[0]));
    }
    double max = Double.POSITIVE_INFINITY;
    for (T value : values) {
      final double v = extractor.applyAsDouble(value);
      if (v < max) {
        max = v;
      }
    }
    return OptionalDouble.of(max);
  }

  public static <T> double sum(final T[] values, final ToDoubleFunction<T> extractor) {
    if (values == null) {
      return 0d;
    }
    double sum = 0;
    for (T value : values) {
      final double v = extractor.applyAsDouble(value);
      sum += v;
    }
    return sum;
  }

  public static OptionalDouble max(final double[] values) {
    if (values == null || values.length == 0) {
      return OptionalDouble.empty();
    }
    if (values.length == 1) {
      return OptionalDouble.of(values[0]);
    }
    double max = Double.NEGATIVE_INFINITY;
    for (double v : values) {
      if (v > max) {
        max = v;
      }
    }
    return OptionalDouble.of(max);
  }

  public static Optional<Float> max(final float[] values) {
    if (values == null || values.length == 0) {
      return Optional.empty();
    }
    if (values.length == 1) {
      return Optional.of(values[0]);
    }
    float max = Float.NEGATIVE_INFINITY;
    for (float v : values) {
      if (v > max) {
        max = v;
      }
    }
    return Optional.of(max);
  }

  public static OptionalDouble min(final double[] values) {
    if (values == null || values.length == 0) {
      return OptionalDouble.empty();
    }
    if (values.length == 1) {
      return OptionalDouble.of(values[0]);
    }
    double max = Double.POSITIVE_INFINITY;
    for (double v : values) {
      if (v < max) {
        max = v;
      }
    }
    return OptionalDouble.of(max);
  }

  public static Optional<Float> min(final float[] values) {
    if (values == null || values.length == 0) {
      return Optional.empty();
    }
    if (values.length == 1) {
      return Optional.of(values[0]);
    }
    float max = Float.POSITIVE_INFINITY;
    for (float v : values) {
      if (v < max) {
        max = v;
      }
    }
    return Optional.of(max);
  }

  /**
   * Combines multiple arrays into a single array, filtering nulls that were passed as array and
   * null array elements.
   */
  public static File[] combine(final @Nullable File[]... arrays) {
    return Arrays.stream(arrays).filter(Objects::nonNull).flatMap(Arrays::stream)
        .filter(Objects::nonNull).toArray(File[]::new);
  }
}
