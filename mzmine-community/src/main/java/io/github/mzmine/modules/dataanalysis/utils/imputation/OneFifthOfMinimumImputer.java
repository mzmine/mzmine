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

package io.github.mzmine.modules.dataanalysis.utils.imputation;

import io.github.mzmine.datamodel.statistics.DataTable;
import io.github.mzmine.datamodel.statistics.DataTableUtils;
import org.apache.commons.math3.linear.RealVector;

/**
 * 1/5 of the minimum intensity for each feature
 */
public class OneFifthOfMinimumImputer implements ImputationFunction {

  public static final double DEVISOR = 5;

  @Override
  public <T extends DataTable> T processInPlace(T data) {
    // do not use data array directly as it is not given that all tables.featureArray will reflect the changes
    for (int featureIndex = 0; featureIndex < data.getNumberOfFeatures(); featureIndex++) {
      final double minValue =
          DataTableUtils.getMinimum(data.getFeatureData(featureIndex, false), true).orElse(1d)
              / DEVISOR;
      DataTableUtils.replaceNaN(data, featureIndex, minValue, true);
    }

    return data;
  }


  public Double apply(RealVector realVector) {
    final double minValue = realVector.getMinValue();
    return minValue / DEVISOR;
  }
}
