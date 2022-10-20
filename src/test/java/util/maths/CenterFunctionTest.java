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

package util.maths;

import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CenterFunctionTest {

  private static final Logger logger = Logger.getLogger(CenterFunctionTest.class.getName());

  /**
   * This test exists to test if calculation of m/zs is consistent even if leading/trailing 0s are
   * added to a feature.
   */
  @Test
  public void testCalcCenter() {
    // 9 values
    final double[] mzsNoZeros = {522.0200, 522.0290, 522.0210, 522.0000, 521.9910, 522.0200,
        522.0200, 522.0200, 522.0200};
    final double[] intensitiesNoZeros = {1E5, 4E3, 8E4, 9E4, 3E3, 5E5, 3E5, 1E5, 1E5};

    final double[] mzsZeros = {522.0200, 522.0290, 522.0210, 522.0000, 521.9910, 522.0200,
        522.0200, 522.0200, 522.0200, 0, 0, 0, 0, 0};
    final double[] intensitiesZeros = {1E5, 4E3, 8E4, 9E4, 3E3, 5E5, 3E5, 1E5, 1E5, 0, 0, 0, 0, 0};

    for (Weighting weighting : Weighting.values()) {
      final CenterFunction cf = new CenterFunction(CenterMeasure.AVG, Weighting.LINEAR);
      final double centerNoZeros = cf.calcCenter(mzsNoZeros, intensitiesNoZeros);
      final double centerZeros = cf.calcCenter(mzsZeros, intensitiesZeros);

      logger.info(weighting.toString() + " Center no zeros: " + centerNoZeros);
      logger.info(weighting.toString() + " Center zeros: " + centerZeros);
      Assertions.assertEquals(centerNoZeros, centerZeros);
    }
  }
}
