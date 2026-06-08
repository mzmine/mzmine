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

import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.statistics.DataTable;
import io.github.mzmine.datamodel.statistics.DataTableUtils;
import io.github.mzmine.util.ArrayUtils;
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
    // do not use data array directly as it is not given that all tables.featureArray will reflect the changes
    for (int featureIndex = 0; featureIndex < data.getNumberOfFeatures(); featureIndex++) {
      // scale within value range
      final var optionalRange = ArrayUtils.rangeOf(data.getFeatureData(featureIndex, false));
      if (optionalRange.isPresent()) {
        final SimpleDoubleRange range = optionalRange.get();
        final double valueDistance = range.length();

        // single value = scale to 0.5
        if (Double.compare(valueDistance, 0d) == 0) {
          DataTableUtils.fillFeatureData(data, featureIndex, maxValue / 2d);
        } else {
          // Scale values between 0 and maxValue
          final double scale = maxValue / valueDistance;

          for (int i = 0; i < data.getNumberOfSamples(); i++) {
            final double value = data.getValue(featureIndex, i);
            data.setValue(featureIndex, i, (value - range.lowerBound()) * scale);
          }
        }
      }
    }

    return data;
  }
}
