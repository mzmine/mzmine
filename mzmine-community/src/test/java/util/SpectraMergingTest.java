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

package util;

import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpectraMergingTest {

  @Test
  void testMerge_Summed() {

    final SimpleMassList ml1 = new SimpleMassList(null, new double[]{1, 5, 10},
        new double[]{10, 10, 10});
    final SimpleMassList ml2 = new SimpleMassList(null, new double[]{1, 5, 10},
        new double[]{20, 30, 40});
    final double[][] mzsIntensities = SpectraMerging.calculatedMergedMzsAndIntensities(
        List.of(ml1, ml2), new MZTolerance(0.01, 10), IntensityMergingType.SUMMED,
        SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, null);

    Assertions.assertEquals(2, mzsIntensities.length);
    Assertions.assertEquals(3, mzsIntensities[0].length);
    Assertions.assertEquals(3, mzsIntensities[1].length);

    Assertions.assertEquals(1d, mzsIntensities[0][0]);
    Assertions.assertEquals(5d, mzsIntensities[0][1]);
    Assertions.assertEquals(10d, mzsIntensities[0][2]);

    Assertions.assertEquals(30d, mzsIntensities[1][0]);
    Assertions.assertEquals(40d, mzsIntensities[1][1]);
    Assertions.assertEquals(50d, mzsIntensities[1][2]);
  }

  @Test
  void testMerge_Max() {

    final SimpleMassList ml1 = new SimpleMassList(null, new double[]{1, 5, 10},
        new double[]{10, 10, 10});
    final SimpleMassList ml2 = new SimpleMassList(null, new double[]{1, 5, 10},
        new double[]{20, 30, 40});
    final double[][] mzsIntensities = SpectraMerging.calculatedMergedMzsAndIntensities(
        List.of(ml1, ml2), new MZTolerance(0.01, 10), IntensityMergingType.MAXIMUM,
        SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, null);

    Assertions.assertEquals(2, mzsIntensities.length);
    Assertions.assertEquals(3, mzsIntensities[0].length);
    Assertions.assertEquals(3, mzsIntensities[1].length);

    Assertions.assertEquals(1d, mzsIntensities[0][0]);
    Assertions.assertEquals(5d, mzsIntensities[0][1]);
    Assertions.assertEquals(10d, mzsIntensities[0][2]);

    Assertions.assertEquals(20d, mzsIntensities[1][0]);
    Assertions.assertEquals(30d, mzsIntensities[1][1]);
    Assertions.assertEquals(40d, mzsIntensities[1][2]);
  }

  @Test
  void testMerge_Avg() {

    final SimpleMassList ml1 = new SimpleMassList(null, new double[]{1, 5, 10},
        new double[]{10, 10, 10});
    final SimpleMassList ml2 = new SimpleMassList(null, new double[]{1, 5, 10},
        new double[]{20, 30, 40});
    final double[][] mzsIntensities = SpectraMerging.calculatedMergedMzsAndIntensities(
        List.of(ml1, ml2), new MZTolerance(0.01, 10), IntensityMergingType.AVERAGE,
        SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, null);

    Assertions.assertEquals(2, mzsIntensities.length);
    Assertions.assertEquals(3, mzsIntensities[0].length);
    Assertions.assertEquals(3, mzsIntensities[1].length);

    Assertions.assertEquals(1d, mzsIntensities[0][0]);
    Assertions.assertEquals(5d, mzsIntensities[0][1]);
    Assertions.assertEquals(10d, mzsIntensities[0][2]);

    Assertions.assertEquals(15d, mzsIntensities[1][0]);
    Assertions.assertEquals(20d, mzsIntensities[1][1]);
    Assertions.assertEquals(25d, mzsIntensities[1][2]);
  }

}
