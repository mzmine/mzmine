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
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class ParetoScalingFunction implements ScalingFunction {

  private final StandardDeviation dev = new StandardDeviation(true);

  @Override
  public RealVector apply(RealVector realVector) {
    final double sd = dev.evaluate(realVector.toArray());
    if (Double.compare(sd, 0d) == 0) {
      return realVector.mapToSelf(v -> 0);
    }
    return realVector.mapDivide(Math.sqrt(sd)).mapToSelf(scalingResultChecker);
  }

  @Override
  public <T extends DataTable> T processInPlace(T data) {
    for (double[] feature : data) {
      final double sd = dev.evaluate(feature);
      if (Double.compare(sd, 0d) == 0) {
        Arrays.fill(feature, 0d);
      } else {
        final double sqrtSD = Math.sqrt(sd);
        for (int i = 0; i < feature.length; i++) {
          feature[i] = scalingResultChecker.value(feature[i] / sqrtSD);
        }
      }
    }
    return data;
  }
}
