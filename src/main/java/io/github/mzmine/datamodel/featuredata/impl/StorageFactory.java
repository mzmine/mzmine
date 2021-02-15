package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StorageFactory {

  public static <T extends IonSeries> DoubleBuffer[] storeIonSeriesToSingleBuffer(
      @Nullable final MemoryMapStorage storage, List<T> seriesList) {

    final List<double[][]> mzIntensities = new ArrayList<>();

    for (final T series : seriesList) {
      double[][] mzIntensity = DataPointUtils
          .getDataPointsAsDoubleArray(series.getMZValues(), series.getIntensityValues());
      mzIntensities.add(mzIntensity);
    }

    final int[] offsets = generateOffsets(mzIntensities);
    final int numDp =
        offsets[offsets.length - 1] + mzIntensities.get(mzIntensities.size() - 1)[0].length;

    final DoubleBuffer[] storedValues = new DoubleBuffer[2];
    double[] storageBuffer = new double[numDp];
    for (int i = 0; i < 2; i++) {
      putAllValuesIntoOneArray(mzIntensities, i, storageBuffer);
      storedValues[i] = storeValuesToDoubleBuffer(storage, storageBuffer);
      if(storage == null) {
        storageBuffer = new double[numDp];
      }
    }
    return storedValues;
  }

  public static int[] generateOffsets(List<double[][]> mzsIntensities) {
    final int[] offsets = new int[mzsIntensities.size()];
    offsets[0] = 0;
    for (int i = 1; i < offsets.length; i++) {
      offsets[i] = offsets[i - 1] + mzsIntensities.get(i - 1)[0].length;
    }
    return offsets;
  }

  /**
   * @param peaks      List of values. it will be iterated over peaks[arrayIndex][i]
   * @param arrayIndex the index of the first dimension of the input array.
   * @param dst        the destination array of an appropriate size.
   * @return An array of base peak indices if arrayIndex == 1.
   */
  public static int[] putAllValuesIntoOneArray(final List<double[][]> peaks,
      final int arrayIndex, double[] dst) {
    int[] basePeakIndices = null;
    if (arrayIndex == 1) {
      basePeakIndices = new int[peaks.size()];
      Arrays.fill(basePeakIndices, -1);
    }

    int dpCounter = 0;
    for (int scanNum = 0, numScans = peaks.size(); scanNum < numScans; scanNum++) {
      double[][] mzIntensity = peaks.get(scanNum);
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
   * @param values  The values to be stored.
   * @return The double buffer the values were stored in.
   */
  @Nonnull
  public static DoubleBuffer storeValuesToDoubleBuffer(@Nullable final MemoryMapStorage storage,
      @Nonnull final double[] values) {

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
}
