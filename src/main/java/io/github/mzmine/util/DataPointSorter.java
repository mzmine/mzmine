/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.DataPoint;
import java.util.Comparator;

/**
 * This class implements Comparator class to provide a comparison between two DataPoints.
 */
public class DataPointSorter implements Comparator<DataPoint> {

  public static final DataPointSorter DEFAULT_MZ_ASCENDING = new DataPointSorter(SortingProperty.MZ,
      SortingDirection.Ascending);
  public static final DataPointSorter DEFAULT_INTENSITY = new DataPointSorter(
      SortingProperty.Intensity, SortingDirection.Descending);

  private final SortingProperty property;
  private final SortingDirection direction;

  public DataPointSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  public int compare(DataPoint dp1, DataPoint dp2) {
    int result;

    switch (property) {
      case MZ:

        result = Double.compare(dp1.getMZ(), dp2.getMZ());

        // If the data points have same m/z, we do a second comparison of
        // intensity, to ensure that this comparator is consistent with
        // equality: (compare(x, y)==0) == (x.equals(y)),
        if (result == 0) {
          result = Double.compare(dp1.getIntensity(), dp2.getIntensity());
        }

        if (direction == SortingDirection.Ascending) {
          return result;
        } else {
          return -result;
        }

      case Intensity:
        result = Double.compare(dp1.getIntensity(), dp2.getIntensity());

        // If the data points have same intensity, we do a second comparison
        // of m/z, to ensure that this comparator is consistent with
        // equality: (compare(x, y)==0) == (x.equals(y)),
        if (result == 0) {
          result = Double.compare(dp1.getMZ(), dp2.getMZ());
        }

        if (direction == SortingDirection.Ascending) {
          return result;
        } else {
          return -result;
        }
      default:
        // We should never get here, so throw an exception
        throw (new IllegalStateException());
    }

  }
}
