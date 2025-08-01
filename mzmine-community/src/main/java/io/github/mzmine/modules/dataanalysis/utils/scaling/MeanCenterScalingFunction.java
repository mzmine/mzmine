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
import io.github.mzmine.util.MathUtils;
import org.apache.commons.math3.linear.RealVector;

/**
 * Center data to mean
 */
public class MeanCenterScalingFunction implements ScalingFunction {

  public MeanCenterScalingFunction() {

  }

  @Override
  public RealVector apply(RealVector columnVector) {
    final double sum = columnVector.getL1Norm();
    final double mean = sum / columnVector.getDimension();

    return columnVector.mapSubtract(mean);
  }


  @Override
  public <T extends DataTable> T processInPlace(T data) {
    // do not use data array directly as it is not given that all tables.featureArray will reflect the changes
    for (int featureIndex = 0; featureIndex < data.getNumberOfFeatures(); featureIndex++) {
      final double mean = MathUtils.calcAvg(data.getFeatureData(featureIndex, false));

      for (int i = 0; i < data.getNumberOfSamples(); i++) {
        final double scaled = data.getValue(featureIndex, i) - mean;
        data.setValue(featureIndex, i, scaled);
      }
    }
    return data;
  }

}
