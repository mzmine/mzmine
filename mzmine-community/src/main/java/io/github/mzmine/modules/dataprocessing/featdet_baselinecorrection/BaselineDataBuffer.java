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
  // true if peak ranges were bridged by interpolation into the removed-peaks buffers
  private boolean rangesInterpolated = false;

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
    rangesInterpolated = false;

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
   * @return true if peak ranges were bridged by interpolation (see
   * {@link #interpolateRangesInArrays(List)}) and the {@link #xBufferRemovedPeaks()} /
   * {@link #yBufferRemovedPeaks()} should be used for the baseline fit
   */
  public boolean hasInterpolatedRanges() {
    return rangesInterpolated;
  }

  /**
   * remaining values after removing ranges
   *
   * @return
   */
  public int remaining() {
    return remaining;
  }

  /**
   * Bridges the given retention time ranges (detected peaks) by linear interpolation instead of
   * removing them. See {@link #interpolateRanges(List)}.
   *
   * @param rtRanges detected peak ranges in retention time. May be empty.
   * @return the number of values written (always the full {@link #numValues()}).
   */
  public int interpolateRangesInArrays(final List<Range<Double>> rtRanges) {
    final List<IndexRange> indices = rtRanges.stream()
        .map(range -> BinarySearch.indexRange(xBuffer, range, 0, numValues)).toList();

    return interpolateRanges(indices);
  }

  /**
   * Bridges the given index ranges (detected peaks) by linear interpolation instead of removing
   * them. The full resolution, regularly spaced grid is preserved: the original signal is copied
   * into {@link #xBufferRemovedPeaks()} / {@link #yBufferRemovedPeaks()} and only the intensities
   * inside each range are overwritten with a straight line between the retained points on both
   * sides. Keeping the regular spacing gives the downstream LOESS/spline fit a consistent sample
   * distance and constrains the baseline estimate underneath peaks, which sub sampling across gaps
   * could not. Only used for the baseline fit - the original signal in {@link #xBuffer()} /
   * {@link #yBuffer()} stays untouched.
   *
   * @param indices sorted, non-overlapping index ranges of detected peaks. May be empty.
   * @return the number of values written (always the full {@link #numValues()}).
   */
  public int interpolateRanges(final List<IndexRange> indices) {
    // keep the full grid: copy the original signal, then bridge peak interiors in place
    System.arraycopy(xBuffer, 0, xBufferRemovedPeaks, 0, numValues);
    System.arraycopy(yBuffer, 0, yBufferRemovedPeaks, 0, numValues);

    // spacing stays regular -> only the first and last point are needed as sampling landmarks
    indicesOfInterest.clear();
    indicesOfInterest.add(0);
    indicesOfInterest.add(numValues - 1);

    remaining = numValues;
    rangesInterpolated = !indices.isEmpty();

    for (final IndexRange range : indices) {
      interpolateSingleRange(range);
    }
    return numValues;
  }

  /**
   * Linearly interpolates the intensities inside a single peak range in the
   * {@link #yBufferRemovedPeaks()}, using the retained points just outside the range as anchors.
   * Ranges touching the array bounds are filled flat from the single available anchor, because
   * there is no baseline information on the missing side.
   */
  private void interpolateSingleRange(final IndexRange range) {
    // last retained point before the peak and first retained point after the peak
    final int left = range.min() - 1;
    final int right = range.maxExclusive(); // first index after the peak, may be == numValues

    final boolean hasLeft = left >= 0;
    final boolean hasRight = right <= numValues - 1;

    if (hasLeft && hasRight) {
      // linear bridge between both anchors
      final double x0 = xBuffer[left];
      final double y0 = yBuffer[left];
      final double slope = (yBuffer[right] - y0) / (xBuffer[right] - x0);
      for (int i = left + 1; i < right; i++) {
        yBufferRemovedPeaks[i] = y0 + slope * (xBuffer[i] - x0);
      }
    } else if (hasRight) {
      // peak at the start: no left anchor -> flat fill with the right anchor intensity
      final double y1 = yBuffer[right];
      for (int i = 0; i < right; i++) {
        yBufferRemovedPeaks[i] = y1;
      }
    } else if (hasLeft) {
      // peak at the end: no right anchor -> flat fill with the left anchor intensity
      final double y0 = yBuffer[left];
      for (int i = left + 1; i < numValues; i++) {
        yBufferRemovedPeaks[i] = y0;
      }
    }
    // decision: if neither anchor exists (whole trace flagged as one peak) keep the original signal
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

