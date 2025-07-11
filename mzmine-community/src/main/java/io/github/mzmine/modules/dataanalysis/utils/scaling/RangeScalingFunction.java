/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.utils.scaling;

import io.github.mzmine.datamodel.statistics.DataTable;
import java.util.Arrays;
import org.apache.commons.math3.linear.RealVector;

public class RangeScalingFunction implements ScalingFunction {

  private final double maxValue;

  public RangeScalingFunction() {
    this(1);
  }

  public RangeScalingFunction(double maxValue) {
    this.maxValue = maxValue;
  }

  @Override
  public RealVector apply(RealVector realVector) {
    final double columnMin = realVector.getMinValue();

    // create a new vector once
    realVector = realVector.mapSubtract(columnMin);
    final double columnMax = realVector.getLInfNorm();

    // apply to same vector now
    realVector.mapDivideToSelf(columnMax / maxValue);
    return realVector.mapToSelf(scalingResultChecker);
  }


  @Override
  public <T extends DataTable> T processInPlace(T data) {
    for (double[] feature : data) {
      if (feature == null || feature.length == 0) {
        continue;
      }

      // Find min and max
      double min = feature[0];
      double max = feature[0];
      for (double value : feature) {
        if (value > max) {
          max = value;
        }
        if (value < min) {
          min = value;
        }
      }

      // Scale values between 0 and maxValue
      final double range = max - min;
      final double scale = maxValue / range;
      if (range == 0) {
        // If all values are identical, set them to maxValue/2
        Arrays.fill(feature, maxValue / 2d);
      } else {
        for (int i = 0; i < feature.length; i++) {
          feature[i] = (feature[i] - min) * scale;
        }
      }
    }
    return data;
  }
}
