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

package stats;

import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.utils.scaling.RangeScalingFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatsUtilsTest {

  double[][] data = new double[][]{ //
      {0, 2, 1, 2}, //
      {1, 2, 5, 0},//
      {1, 1, 1, 0}};

  double[][] missingData = new double[][]{ //
      {0, 2, 1, 2}, //
      {1, Double.NaN, 5, 2}, //
      {1, 1, 1, Double.NaN}}; //

  @Test
  void testCentering() {

    final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(data);
    final RealMatrix centered = StatisticUtils.center(matrix, false);

    Assertions.assertEquals((double) -2 / 3, centered.getEntry(0, 0));
    Assertions.assertEquals(2 - (double) 5 / 3, centered.getEntry(1, 1));
    Assertions.assertEquals(1 - (double) 7 / 3, centered.getEntry(0, 2));
    Assertions.assertEquals(0 - (double) 2 / 3, centered.getEntry(2, 3));
  }

  @Test
  void test() {
    final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(data);
    final RealMatrix unitVar = StatisticUtils.scale(matrix, new RangeScalingFunction(1), false);

    Assertions.assertEquals(0, unitVar.getEntry(0, 0));
    Assertions.assertEquals(1, unitVar.getEntry(1, 1));
    Assertions.assertEquals(0.0, unitVar.getEntry(2, 1));
    Assertions.assertEquals(0d, unitVar.getEntry(0, 2));
    Assertions.assertEquals(0, unitVar.getEntry(2, 3));
  }

}
