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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class BaselineDataBuffer {

  private static final Logger logger = Logger.getLogger(BaselineDataBuffer.class.getName());

  private final IntList indicesOfInterest = new IntArrayList();
  private int numValues = 0;
  private double[] xBuffer = new double[0];
  private double[] yBuffer = new double[0];
  private double[] xBufferRemovedPeaks = new double[0];
  private double[] yBufferRemovedPeaks = new double[0];
  private int remaining;

  public void ensureCapacity(final int capacity) {
    if (xBuffer.length < capacity) {
      xBuffer = new double[capacity];
    }
    if (yBuffer.length < capacity) {
      yBuffer = new double[capacity];
    }
    if (xBufferRemovedPeaks.length < capacity) {
      xBufferRemovedPeaks = new double[capacity];
    }
    if (yBufferRemovedPeaks.length < capacity) {
      yBufferRemovedPeaks = new double[capacity];
    }
  }

  public <T extends IntensityTimeSeries> void extractDataIntoBuffer(final T timeSeries) {
    numValues = timeSeries.getNumberOfValues();
    remaining = numValues;

    boolean yAlreadySet = false;
    if (timeSeries instanceof FeatureDataAccess access) {
      // use the existing backing data array for y
      // this needs to happen before ensureCapacity
      // therefore mark if data is already set to avoid copying data later
      yBuffer = access.getIntensityValues();
      yAlreadySet = true;
    }

    ensureCapacity(numValues);
    clearRemovedPeaksBuffers();
    extractRtValues(timeSeries); // needs to happen after ensureCapacity

    indicesOfInterest.clear();
    indicesOfInterest.add(0);
    indicesOfInterest.add(numValues - 1);

    if (!yAlreadySet) {
      // data was not set from a feature data access - is regular IntensityTimeSeries
      // fill data in regular way copying over data array
      // needs to happen after ensureCapacity
      Arrays.fill(yBuffer, numValues, yBuffer.length, 0d);
      yBuffer = timeSeries.getIntensityValues(yBuffer);
    }
  }


  private void clearRemovedPeaksBuffers() {
    Arrays.fill(xBufferRemovedPeaks, 0d);
    Arrays.fill(yBufferRemovedPeaks, 0d);
  }

  private <T extends IntensityTimeSeries> void extractRtValues(final T timeSeries) {
    int numValues = timeSeries.getNumberOfValues();
    Arrays.fill(xBuffer, numValues, xBuffer.length, 0d);
    for (int i = 0; i < numValues; i++) {
      xBuffer[i] = timeSeries.getRetentionTime(i);
      if (i > 0 && xBuffer[i] < xBuffer[i - 1]) {
        throw new IllegalStateException();
      }
    }
  }

  /**
   * Indices of interest are landmark indices in the data arrays that should be included during
   * subsampling. This always includes the first and last data point and the index left and right of
   * detected features that may be excluded during baseline detection
   *
   * @return list of indices for sub sampling
   */
  public IntList indicesOfInterest() {
    return indicesOfInterest;
  }

  public int numValues() {
    return numValues;
  }

  public double[] xBuffer() {
    return xBuffer;
  }

  public double[] yBuffer() {
    return yBuffer;
  }

  public double[] xBufferRemovedPeaks() {
    return xBufferRemovedPeaks;
  }

  public double[] yBufferRemovedPeaks() {
    return yBufferRemovedPeaks;
  }

  /**
   * @return true if ranges were removed and the xBufferRemovedPeaks and yBufferRemovedPeaks should
   * be used
   */
  public boolean hasRemovedRanges() {
    return numValues != remaining;
  }

  /**
   * remaining values after removing ranges
   *
   * @return
   */
  public int remaining() {
    return remaining;
  }

  public int removeRangesFromArrays(final List<Range<Double>> rtRanges) {
    final List<IndexRange> indices = rtRanges.stream()
        .map(range -> BinarySearch.indexRange(xBuffer, range, 0, numValues)).toList();

    return removeRangesFromArray(indices);
  }

  /**
   * Removes the given list of index ranges from the array, always keeping the first and last value
   * even if they are contained in one of the ranges. This may be needed for
   * {@link org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction}, because it will
   * not extrapolate beyond the sides.
   *
   * @param indices The list of index ranges. May be empty.
   * @return The number of values written to the array.
   */
  public int removeRangesFromArray(List<IndexRange> indices) {
    int startInRemovedArray = 0;
    int lastEndPointInOriginalArray = 0;

    // keep track of indices right next to the removed areas + first and last point
    indicesOfInterest.clear();
    indicesOfInterest.add(0);

    if (indices.isEmpty()) {
      // no need to copy data if none is removed
      xBufferRemovedPeaks = xBuffer;
      yBufferRemovedPeaks = yBuffer;

      // only first and last index
      indicesOfInterest.add(numValues - 1);

      remaining = numValues;
      return numValues;
    } else {
      if (indices.getFirst().min() == 0) {
        xBufferRemovedPeaks[0] = xBuffer[0];
        yBufferRemovedPeaks[0] = yBuffer[0];
        startInRemovedArray++;
        lastEndPointInOriginalArray++;
        // add right bound of first range (peak) that was removed
        indicesOfInterest.add(1);
      }

      for (final IndexRange range : indices) {
        final int numPoints = range.min() - lastEndPointInOriginalArray;

        // in case the first range starts at 0 and the first point was copied manually, this condition is not met.
        if (numPoints > 0) {
          System.arraycopy(xBuffer, lastEndPointInOriginalArray, xBufferRemovedPeaks,
              startInRemovedArray, numPoints);
          System.arraycopy(yBuffer, lastEndPointInOriginalArray, yBufferRemovedPeaks,
              startInRemovedArray, numPoints);
          startInRemovedArray += numPoints;
          // add left bound of removed range (peak)
          if (numPoints > 1) {
            indicesOfInterest.add(startInRemovedArray - 1);
          }
          // add right bound of removed range (peak) - which is now just the next point
          indicesOfInterest.add(startInRemovedArray); // this might be the last data point
        }
        lastEndPointInOriginalArray = range.maxExclusive();
      }
    }

    // last range removed the last value -> add last value back
    if (lastEndPointInOriginalArray >= numValues) {
      // indicesOfInterest already include this data point
      xBufferRemovedPeaks[startInRemovedArray] = xBuffer[numValues - 1];
      yBufferRemovedPeaks[startInRemovedArray] = yBuffer[numValues - 1];
      startInRemovedArray++;
    } else {
      // add values until the end
      int numPoints = numValues - lastEndPointInOriginalArray;
      System.arraycopy(xBuffer, lastEndPointInOriginalArray, xBufferRemovedPeaks,
          startInRemovedArray, numPoints);
      System.arraycopy(yBuffer, lastEndPointInOriginalArray, yBufferRemovedPeaks,
          startInRemovedArray, numPoints);
      startInRemovedArray += numPoints;
      // add last data point to index of interest
      indicesOfInterest.add(startInRemovedArray - 1);
    }
    remaining = startInRemovedArray;
    return startInRemovedArray;
  }


  /**
   * Create list of indices from landmark indices of interest. step size is used to fill in
   * additional steps
   *
   * @param stepSize
   * @return
   */
  public @NotNull IntList createSubSampleIndicesFromLandmarks(int stepSize) {
    if (stepSize < 4) {
      stepSize = 4; // minimum required distance between samples
    }
    int lastIndex = indicesOfInterest.getInt(0);
    IntList subsampleIndices = new IntArrayList();
    subsampleIndices.add(lastIndex);
    int nextIndexOfInterest;
    for (int i = 1; i < indicesOfInterest.size(); i++) {
      nextIndexOfInterest = indicesOfInterest.getInt(i);
      // require distance of 2x stepSize to add step in between to landmark indices
      while (lastIndex + stepSize * 2 < nextIndexOfInterest) {
        lastIndex = lastIndex + stepSize;
        subsampleIndices.add(lastIndex);
      }
      // if step size is more than X x stepsize -> add data point in between
      if (nextIndexOfInterest - lastIndex > stepSize * 1.5) {
        lastIndex = (nextIndexOfInterest + lastIndex) / 2;
        subsampleIndices.add(lastIndex);
      }

      // only add landmark index of interest if not to close to another index
      if (nextIndexOfInterest - lastIndex > stepSize * 0.25) {
        subsampleIndices.add(nextIndexOfInterest);
        lastIndex = nextIndexOfInterest;
      }
    }
    // make sure last index is added
    int finalIndex = indicesOfInterest.getLast();
    if (lastIndex != finalIndex) {
      subsampleIndices.add(finalIndex);
    }

//    logger.finer("Subsampling indices: " + StringUtils.join(subsampleIndices.toIntArray(), ", "));
    return subsampleIndices;
  }
}

