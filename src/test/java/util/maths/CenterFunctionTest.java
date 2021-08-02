/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
   * This test exists to test if calculation of m/zs is consistent even iv leading/trailing 0s are
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
