/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.RawDataFile;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandardCompoundNormalizationFunctionTest {

  @Test
  void nearestUsesClosestReferencePoint() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
    final StandardCompoundNormalizationFunction function = new StandardCompoundNormalizationFunction(
        file, timestamp, StandardUsageType.Nearest, 1.0d,
        List.of(new StandardCompoundReferencePoint(100d, 5f, 200d),
            new StandardCompoundReferencePoint(150d, 10f, 500d)));

    final double factor = function.getNormalizationFactor(100.1d, 5.1f);

    assertEquals(0.005d, factor, 1e-12);
    assertEquals(timestamp, function.acquisitionTimestamp());
  }

  @Test
  void nearestUsesLaterReferencePointOnEqualDistance() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final StandardCompoundNormalizationFunction function = new StandardCompoundNormalizationFunction(
        file, LocalDateTime.of(2026, 1, 1, 10, 0), StandardUsageType.Nearest, 1.0d,
        List.of(new StandardCompoundReferencePoint(100d, 4f, 200d),
            new StandardCompoundReferencePoint(100d, 6f, 400d)));

    final double factor = function.getNormalizationFactor(100d, 5f);

    assertEquals(0.0025d, factor, 1e-12);
  }

  @Test
  void weightedUsesInverseDistanceAveraging() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final StandardCompoundNormalizationFunction function = new StandardCompoundNormalizationFunction(
        file, LocalDateTime.of(2026, 1, 1, 10, 0), StandardUsageType.Weighted, 1.0d,
        List.of(new StandardCompoundReferencePoint(100d, 4f, 100d),
            new StandardCompoundReferencePoint(100d, 6f, 300d)));

    final double factor = function.getNormalizationFactor(100d, 4.5f);

    assertEquals(2d / 3d * 0.01d, factor, 1e-12);
  }

  @Test
  void weightedUsesDirectMatchesBeforeDistanceWeights() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final StandardCompoundNormalizationFunction function = new StandardCompoundNormalizationFunction(
        file, LocalDateTime.of(2026, 1, 1, 10, 0), StandardUsageType.Weighted, 1.0d,
        List.of(new StandardCompoundReferencePoint(100d, 5f, 250d),
            new StandardCompoundReferencePoint(100d, 8f, 500d)));

    final double factor = function.getNormalizationFactor(100d, 5f);

    assertEquals(0.004d, factor, 1e-12);
  }
}

