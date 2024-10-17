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

import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import java.util.Arrays;

public class BaselineDataBuffer {

  private double[] xBuffer = new double[0];
  private double[] yBuffer = new double[0];
  private double[] xBufferRemovedPeaks = new double[0];
  private double[] yBufferRemovedPeaks = new double[0];

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
    boolean yAlreadySet = false;
    if (timeSeries instanceof FeatureDataAccess access) {
      // can use the whole
      yBuffer = access.getIntensityValues();
      yAlreadySet = true;
    }

    int numValues = timeSeries.getNumberOfValues();
    ensureCapacity(numValues);
    extractRtValues(timeSeries);
    clearRemovedPeaksBuffers();

    if (!yAlreadySet) {
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
}

