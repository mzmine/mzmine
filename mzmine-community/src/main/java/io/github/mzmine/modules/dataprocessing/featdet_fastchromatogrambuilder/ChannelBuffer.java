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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Growable primitive storage of the data points of one channel in ascending scan order, at most
 * one data point per scan. The second pass appends, {@link ChannelFinalization} inserts and merges.
 */
final class ChannelBuffer {

  private int[] scanIndices;
  private double[] mzs;
  private double[] intensities;
  private int size = 0;

  ChannelBuffer(int initialCapacity) {
    final int capacity = Math.max(4, initialCapacity);
    scanIndices = new int[capacity];
    mzs = new double[capacity];
    intensities = new double[capacity];
  }

  /**
   * @param scanIndex larger than the scan index of all data points
   */
  void add(int scanIndex, double mz, double intensity) {
    ensureCapacity(size + 1);
    scanIndices[size] = scanIndex;
    mzs[size] = mz;
    intensities[size] = intensity;
    size++;
  }

  /**
   * @param scanIndex a scan without data point in this buffer
   */
  void insert(int scanIndex, double mz, double intensity) {
    final int found = indexOfScan(scanIndex);
    if (found >= 0) {
      throw new IllegalArgumentException("Scan " + scanIndex + " already has a data point");
    }
    final int position = -found - 1;
    ensureCapacity(size + 1);
    System.arraycopy(scanIndices, position, scanIndices, position + 1, size - position);
    System.arraycopy(mzs, position, mzs, position + 1, size - position);
    System.arraycopy(intensities, position, intensities, position + 1, size - position);
    scanIndices[position] = scanIndex;
    mzs[position] = mz;
    intensities[position] = intensity;
    size++;
  }

  /**
   * Adds all data points of the other buffer.
   *
   * @param other a buffer without data point in the scans of this buffer
   */
  void addAll(@NotNull ChannelBuffer other) {
    final int total = size + other.size;
    final int[] mergedScans = new int[Math.max(4, total)];
    final double[] mergedMzs = new double[mergedScans.length];
    final double[] mergedIntensities = new double[mergedScans.length];
    int i = 0;
    int j = 0;
    for (int k = 0; k < total; k++) {
      final boolean fromThis;
      if (j == other.size) {
        fromThis = true;
      } else if (i == size) {
        fromThis = false;
      } else if (scanIndices[i] == other.scanIndices[j]) {
        throw new IllegalArgumentException(
            "Both buffers have a data point in scan " + scanIndices[i]);
      } else {
        fromThis = scanIndices[i] < other.scanIndices[j];
      }
      if (fromThis) {
        mergedScans[k] = scanIndices[i];
        mergedMzs[k] = mzs[i];
        mergedIntensities[k] = intensities[i++];
      } else {
        mergedScans[k] = other.scanIndices[j];
        mergedMzs[k] = other.mzs[j];
        mergedIntensities[k] = other.intensities[j++];
      }
    }
    scanIndices = mergedScans;
    mzs = mergedMzs;
    intensities = mergedIntensities;
    size = total;
  }

  /**
   * Adds the data points [from, to) of the other buffer. In scans with a data point in both
   * buffers, the data point of the other buffer replaces the one of this buffer, which is passed to
   * the consumer.
   */
  void addRangeReplacing(@NotNull ChannelBuffer other, int from, int to,
      @NotNull DataPointConsumer replaced) {
    final int[] mergedScans = new int[Math.max(4, size + to - from)];
    final double[] mergedMzs = new double[mergedScans.length];
    final double[] mergedIntensities = new double[mergedScans.length];
    int i = 0;
    int j = from;
    int k = 0;
    while (i < size || j < to) {
      if (j == to || (i < size && scanIndices[i] < other.scanIndices[j])) {
        mergedScans[k] = scanIndices[i];
        mergedMzs[k] = mzs[i];
        mergedIntensities[k++] = intensities[i++];
        continue;
      }
      if (i < size && scanIndices[i] == other.scanIndices[j]) {
        replaced.accept(scanIndices[i], mzs[i], intensities[i]);
        i++;
      }
      mergedScans[k] = other.scanIndices[j];
      mergedMzs[k] = other.mzs[j];
      mergedIntensities[k++] = other.intensities[j++];
    }
    scanIndices = mergedScans;
    mzs = mergedMzs;
    intensities = mergedIntensities;
    size = k;
  }

  /**
   * Removes the data points [from, to).
   */
  void removeRange(int from, int to) {
    System.arraycopy(scanIndices, to, scanIndices, from, size - to);
    System.arraycopy(mzs, to, mzs, from, size - to);
    System.arraycopy(intensities, to, intensities, from, size - to);
    size -= to - from;
  }

  /**
   * Receives data points.
   */
  @FunctionalInterface
  interface DataPointConsumer {

    void accept(int scanIndex, double mz, double intensity);
  }

  /**
   * @return the index of the data point in the scan, otherwise (-(insertion point) - 1)
   */
  int indexOfScan(int scanIndex) {
    return Arrays.binarySearch(scanIndices, 0, size, scanIndex);
  }

  private void ensureCapacity(int capacity) {
    if (capacity <= scanIndices.length) {
      return;
    }
    final int newCapacity = Math.max(capacity, size + (size >> 1) + 1);
    scanIndices = Arrays.copyOf(scanIndices, newCapacity);
    mzs = Arrays.copyOf(mzs, newCapacity);
    intensities = Arrays.copyOf(intensities, newCapacity);
  }

  int size() {
    return size;
  }

  @NotNull int[] scanIndices() {
    return scanIndices;
  }

  @NotNull double[] mzs() {
    return mzs;
  }

  @NotNull double[] intensities() {
    return intensities;
  }

  double maxIntensity() {
    double max = 0d;
    for (int i = 0; i < size; i++) {
      max = Math.max(max, intensities[i]);
    }
    return max;
  }

  /**
   * @return the smallest m/z of the data points, infinity if empty
   */
  double minMz() {
    double min = Double.POSITIVE_INFINITY;
    for (int i = 0; i < size; i++) {
      min = Math.min(min, mzs[i]);
    }
    return min;
  }

  /**
   * @return the largest m/z of the data points, negative infinity if empty
   */
  double maxMz() {
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < size; i++) {
      max = Math.max(max, mzs[i]);
    }
    return max;
  }

  @NotNull BuiltChromatogram toChromatogram(double centerMz) {
    return new BuiltChromatogram(centerMz, Arrays.copyOf(scanIndices, size),
        Arrays.copyOf(mzs, size), Arrays.copyOf(intensities, size));
  }
}
