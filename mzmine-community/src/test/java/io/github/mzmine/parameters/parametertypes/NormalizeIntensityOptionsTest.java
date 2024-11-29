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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NormalizeIntensityOptionsTest {

  @Test
  void testNormalizeArray() {
    var values = generateRandomNumbers();
    Assertions.assertEquals(100,
        Arrays.stream(NormalizeIntensityOptions.SUM_AS_100.normalize(values)).sum(), 0.000001);
    Assertions.assertEquals(1,
        Arrays.stream(NormalizeIntensityOptions.SUM_AS_1.normalize(values)).sum(), 0.000001);
    Assertions.assertEquals(100,
        Arrays.stream(NormalizeIntensityOptions.HIGHEST_SIGNAL_AS_100.normalize(values)).max()
            .orElse(0), 0.000001);
    Assertions.assertEquals(1,
        Arrays.stream(NormalizeIntensityOptions.HIGHEST_SIGNAL_AS_1.normalize(values)).max()
            .orElse(0), 0.000001);
  }

  private static double[] generateRandomNumbers() {
    Random rand = new Random(42);
    return rand.doubles(10, 0, 1000).toArray();
  }

  @Test
  void testNormalizeDataPoints() {
    var values = Arrays.stream(generateRandomNumbers()).mapToObj(v -> new SimpleDataPoint(v, v))
        .toArray(DataPoint[]::new);

    Assertions.assertEquals(100,
        Arrays.stream(NormalizeIntensityOptions.SUM_AS_100.normalize(values))
            .mapToDouble(DataPoint::getIntensity).sum(), 0.000001);
    Assertions.assertEquals(1, Arrays.stream(NormalizeIntensityOptions.SUM_AS_1.normalize(values))
        .mapToDouble(DataPoint::getIntensity).sum(), 0.000001);
    Assertions.assertEquals(100,
        Arrays.stream(NormalizeIntensityOptions.HIGHEST_SIGNAL_AS_100.normalize(values))
            .mapToDouble(DataPoint::getIntensity).max().orElse(0), 0.000001);
    Assertions.assertEquals(1,
        Arrays.stream(NormalizeIntensityOptions.HIGHEST_SIGNAL_AS_1.normalize(values))
            .mapToDouble(DataPoint::getIntensity).max().orElse(0), 0.000001);
  }
}