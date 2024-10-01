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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to store lists of arrays into a single DoubleBuffer to safe memory.
 *
 * @author https://github.com/SteffenHeu
 */
public class StorageUtils {

  public static final MemorySegment EMPTY_DOUBLE_SEGMENT = MemorySegment.ofArray(new double[0]);
  public static final MemorySegment EMPTY_FLOAT_SEGMENT = MemorySegment.ofArray(new float[0]);

  public static <T> List<double[][]> mapTo2dDoubleArrayList(List<T> objects,
      Function<T, double[]> firstDimension, Function<T, double[]> secondDimension) {
    return objects.stream().<double[][]>mapMulti((scan, c) -> {
      c.accept(new double[][]{firstDimension.apply(scan), secondDimension.apply(scan)});
    }).toList();
  }

  /**
   * @param storage    The storage the m/z and intensity values shall be saved to. If null, the
   *                   values will be wrapped in a double buffer and stored in ram.
   * @param seriesList A list of the series that shall be combined into a single buffer. It is
   *                   assumed that the number of m/z and intensity values for a single series are
   *                   equal.
   * @param offsets    (out) An array the series' offsets in the returned storage buffer will be
   *                   written into. Must have the same length as seriesList.
   * @param <T>        A class extending {@link IonSeries}.
   * @return Two double buffers. [0] containing m/z values, [1] containing intensity values.
   */
  public static <T extends IonSeries> MemorySegment[] storeIonSeriesToSingleBuffer(
      @Nullable final MemoryMapStorage storage, final List<T> seriesList, int[] offsets) {
    assert offsets.length == seriesList.size();

    final List<double[][]> mzIntensities = new ArrayList<>();

    for (final T series : seriesList) {
      double[][] mzIntensity = DataPointUtils.getDataPointsAsDoubleArray(series.getMZValueBuffer(),
          series.getIntensityValueBuffer());
      mzIntensities.add(mzIntensity);
    }

    // generate and copy the offsets to the array passed as an argument.
    final int[] generatedOffsets = generateOffsets(mzIntensities, new AtomicInteger(0));
    System.arraycopy(generatedOffsets, 0, offsets, 0, generatedOffsets.length);

    final int numDp =
        offsets[offsets.length - 1] + mzIntensities.get(mzIntensities.size() - 1)[0].length;
    final MemorySegment[] storedValues = new MemorySegment[2];
    double[] storageBuffer = new double[numDp];

    for (int i = 0; i < 2; i++) {
      putAllValuesIntoOneArray(mzIntensities, i, storageBuffer);
      storedValues[i] = storeValuesToDoubleBuffer(storage, storageBuffer);
      if (storage == null) {
        storageBuffer = new double[numDp];
      }
    }

    return storedValues;
  }

  /**
   * Generates offsets for a list of 2D-double-arrays. it is assumed, that the first dimension [x][]
   * specifies the dimension of the value, and the second dimension [][x] contains the values. For
   * example, [0][0] contains the first, [0][1] the second m/z value and [1][0] contains the first
   * and [1][1] contains the second intensity value.
   *
   * @param dataArrayList A list of all the values offsets shall be generated for.
   * @return An array of integer offsets. The indices in the array correspond to the indices in the
   * given list.
   */
  public static int[] generateOffsets(List<double[][]> dataArrayList,
      @NotNull AtomicInteger biggestOffset) {
    final int[] offsets = new int[dataArrayList.size()];
    offsets[0] = 0;
    int biggest = 0;
    for (int i = 1; i < offsets.length; i++) {
      final int numPoints = dataArrayList.get(i - 1)[0].length;
      offsets[i] = offsets[i - 1] + numPoints;
      if (numPoints > biggest) {
        biggest = numPoints;
      }
    }
    biggestOffset.set(biggest);
    return offsets;
  }

  /**
   * @param values     List of values. it will be iterated over values[arrayIndex][i]
   * @param arrayIndex the index of the first dimension of the input array.
   * @param dst        the destination array of an appropriate size.
   * @return An array of base peak indices if arrayIndex == 1.
   */
  public static int[] putAllValuesIntoOneArray(final List<double[][]> values, final int arrayIndex,
      double[] dst) {
    int[] basePeakIndices = null;
    if (arrayIndex == 1) {
      basePeakIndices = new int[values.size()];
      Arrays.fill(basePeakIndices, -1);
    }

    int dpCounter = 0;
    for (int scanNum = 0, numScans = values.size(); scanNum < numScans; scanNum++) {
      double[][] mzIntensity = values.get(scanNum);
      double maxIntensity = -1d;

      double[] doubles = mzIntensity[arrayIndex];
      for (int peakNum = 0; peakNum < doubles.length; peakNum++) {
        double thisMzOrIntensity = doubles[peakNum];
        dst[dpCounter] = thisMzOrIntensity;
        dpCounter++;
        if (arrayIndex == 1 && thisMzOrIntensity > maxIntensity) {
          maxIntensity = thisMzOrIntensity;
          basePeakIndices[scanNum] = peakNum;
        }
      }
    }

    return basePeakIndices;
  }

  /**
   * Stores the given array into a double buffer.
   *
   * @param storage The storage to be used. If null, the values will be wrapped using
   *                {@link MemorySegment#ofArray(double[])}.
   * @param values  The values to be stored. If storage is null, a double buffer will be wrapped
   *                around this array. Changes in the array will therefore be reflected in the
   *                DoubleBuffer.
   * @return The double buffer the values were stored in.
   */
  @NotNull
  public static MemorySegment storeValuesToDoubleBuffer(@Nullable final MemoryMapStorage storage,
      @NotNull final double[] values) {
    if (values.length == 0) {
      return EMPTY_DOUBLE_SEGMENT;
    }

    MemorySegment buffer;
    if (storage != null) {
      buffer = storage.storeData(values);
    } else {
      buffer = MemorySegment.ofArray(values);
    }
    return buffer;
  }

  /**
   * Stores the given array into a double buffer.
   *
   * @param storage The storage to be used. If null, the values will be wrapped using
   *                {@link MemorySegment#ofArray(float[])}.
   * @param values  The values to be stored. If storage is null, a double buffer will be wrapped
   *                around this array. Changes in the array will therefore be reflected in the
   *                DoubleBuffer.
   * @return The double buffer the values were stored in.
   */
  @NotNull
  public static MemorySegment storeValuesToFloatBuffer(@Nullable final MemoryMapStorage storage,
      @NotNull final float[] values) {
    if (values.length == 0) {
      return EMPTY_FLOAT_SEGMENT;
    }

    MemorySegment buffer;
    if (storage != null) {
      buffer = storage.storeData(values);
    } else {
      buffer = MemorySegment.ofArray(values);
    }
    return buffer;
  }

  /**
   * Stores the given array into an int buffer.
   *
   * @param storage The storage to be used. If null, the values will be wrapped using
   *                {@link IntBuffer#wrap(int[])}.
   * @param values  The values to be stored. If storage is null, an int buffer will be wrapped
   *                around this array. Changes in the array will therefore be reflected in the
   *                DoubleBuffer.
   * @return The int buffer the values were stored in.
   */
  @NotNull
  public static MemorySegment storeValuesToIntBuffer(@Nullable final MemoryMapStorage storage,
      @NotNull final int[] values) {

    MemorySegment buffer;
    if (storage != null) {
      buffer = storage.storeData(values);
    } else {
      buffer = MemorySegment.ofArray(values);
    }

    return buffer;
  }

  public static long numFloats(MemorySegment segment) {
    return segment.byteSize() / ValueLayout.JAVA_FLOAT.byteSize();
  }

  public static long numDoubles(MemorySegment segment) {
    return segment.byteSize() / ValueLayout.JAVA_DOUBLE.byteSize();
  }

  public static long numInts(MemorySegment segment) {
    return segment.byteSize() / ValueLayout.JAVA_INT.byteSize();
  }

  public static MemorySegment sliceDoubles(MemorySegment segment, long startIndex,
      long endIndexExclusive) {
    return segment.asSlice(startIndex * ValueLayout.JAVA_DOUBLE.byteSize(),
        (endIndexExclusive - startIndex) * ValueLayout.JAVA_DOUBLE.byteSize());
  }

  public static MemorySegment sliceFloats(MemorySegment segment, long startIndex,
      long endIndexExclusive) {
    return segment.asSlice(startIndex * ValueLayout.JAVA_FLOAT.byteSize(),
        (endIndexExclusive - startIndex) * ValueLayout.JAVA_FLOAT.byteSize());
  }

  public static MemorySegment sliceInts(MemorySegment segment, long startIndex,
      long endIndexExclusive) {
    return segment.asSlice(startIndex * ValueLayout.JAVA_INT.byteSize(),
        (endIndexExclusive - startIndex) * ValueLayout.JAVA_INT.byteSize());
  }

  public static double[] copyOfRangeDouble(MemorySegment segment, long startIndex,
      long endIndexExclusive) {
    return sliceDoubles(segment, startIndex, endIndexExclusive).toArray(ValueLayout.JAVA_DOUBLE);
  }

  public static void copyToBuffer(double[] dst, MemorySegment src, long fromIndex,
      long endIndexExclusive) {
    if (dst.length < endIndexExclusive - fromIndex
        || src.byteSize() / ValueLayout.JAVA_DOUBLE.byteSize() < endIndexExclusive - fromIndex) {
      throw new IndexOutOfBoundsException();
    }

    MemorySegment.copy(src, ValueLayout.JAVA_DOUBLE, fromIndex * ValueLayout.JAVA_DOUBLE.byteSize(),
        dst, 0, (int) (endIndexExclusive - fromIndex));
  }

  public static void copyToBuffer(float[] dst, MemorySegment src, long fromIndex,
      long endIndexExclusive) {
    if (dst.length < endIndexExclusive - fromIndex
        || src.byteSize() / ValueLayout.JAVA_FLOAT.byteSize() < endIndexExclusive - fromIndex) {
      throw new IndexOutOfBoundsException();
    }

    MemorySegment.copy(src, ValueLayout.JAVA_FLOAT, fromIndex * ValueLayout.JAVA_FLOAT.byteSize(),
        dst, 0, (int) (endIndexExclusive - fromIndex));
  }

  public static float[] copyOfRangeFloat(MemorySegment segment, long startIndex,
      long endIndexExclusive) {
    return sliceFloats(segment, startIndex, endIndexExclusive).toArray(ValueLayout.JAVA_FLOAT);
  }

  public static boolean contentEquals(MemorySegment s1, MemorySegment s2) {
    if(s1.byteSize() != s2.byteSize()) {
      return false;
    }
    return MemorySegment.mismatch(s1, 0, s1.byteSize(), s2, 0, s2.byteSize()) == -1;
  }
}
