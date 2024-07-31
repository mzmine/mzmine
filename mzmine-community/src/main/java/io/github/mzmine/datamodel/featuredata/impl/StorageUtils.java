/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.datamodel.impl.AbstractStorableSpectrum;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
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
  public static <T extends IonSeries> DoubleBuffer[] storeIonSeriesToSingleBuffer(
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
    final DoubleBuffer[] storedValues = new DoubleBuffer[2];
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
   * @param storage The storage to be used. If null, the values will be wrapped using {@link
   *                DoubleBuffer#wrap(double[])}.
   * @param values  The values to be stored. If storage is null, a double buffer will be wrapped
   *                around this array. Changes in the array will therefore be reflected in the
   *                DoubleBuffer.
   * @return The double buffer the values were stored in.
   */
  @NotNull
  public static DoubleBuffer storeValuesToDoubleBuffer(@Nullable final MemoryMapStorage storage,
      @NotNull final double[] values) {
    if (values.length == 0) {
      return AbstractStorableSpectrum.EMPTY_BUFFER;
    }

    DoubleBuffer buffer;
    if (storage != null) {
      try {
        buffer = storage.storeData(values);
      } catch (IOException e) {
        e.printStackTrace();
        buffer = DoubleBuffer.wrap(values);
      }
    } else {
      buffer = DoubleBuffer.wrap(values);
    }
    return buffer;
  }

  /**
   * Stores the given array into a double buffer.
   *
   * @param storage The storage to be used. If null, the values will be wrapped using
   *                {@link DoubleBuffer#wrap(double[])}.
   * @param values  The values to be stored. If storage is null, a double buffer will be wrapped
   *                around this array. Changes in the array will therefore be reflected in the
   *                DoubleBuffer.
   * @return The double buffer the values were stored in.
   */
  @NotNull
  public static FloatBuffer storeValuesToFloatBuffer(@Nullable final MemoryMapStorage storage,
      @NotNull final float[] values) {
    if (values.length == 0) {
      return AbstractStorableSpectrum.EMPTY_FLOAT_BUFFER;
    }

    FloatBuffer buffer;
    if (storage != null) {
      try {
        buffer = storage.storeData(values);
      } catch (IOException e) {
        e.printStackTrace();
        buffer = FloatBuffer.wrap(values);
      }
    } else {
      buffer = FloatBuffer.wrap(values);
    }
    return buffer;
  }

  /**
   * Stores the given array into an int buffer.
   *
   * @param storage The storage to be used. If null, the values will be wrapped using {@link
   *                IntBuffer#wrap(int[])}.
   * @param values  The values to be stored. If storage is null, an int buffer will be wrapped
   *                around this array. Changes in the array will therefore be reflected in the
   *                DoubleBuffer.
   * @return The int buffer the values were stored in.
   */
  @NotNull
  public static IntBuffer storeValuesToIntBuffer(@Nullable final MemoryMapStorage storage,
      @NotNull final int[] values) {

    IntBuffer buffer;
    if (storage != null) {
      try {
        buffer = storage.storeData(values);
      } catch (IOException e) {
        e.printStackTrace();
        buffer = IntBuffer.wrap(values);
      }
    } else {
      buffer = IntBuffer.wrap(values);
    }
    return buffer;
  }
}
