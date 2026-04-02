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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.datamodel.RawDataFile;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InterpolatedNormalizationFunctionTest {

  @Test
  void constructorThrowsIfWeightsDoNotSumToOne() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
    final FactorNormalizationFunction previousFunction = new FactorNormalizationFunction(file,
        timestamp, 2d);
    final FactorNormalizationFunction nextFunction = new FactorNormalizationFunction(file,
        timestamp, 4d);

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> new InterpolatedNormalizationFunction(file, timestamp, previousFunction, 0.8d,
            nextFunction, 0.8d));

    assertEquals("Sum of previous and next run weight must be 1. prev=0.800000, next=0.800000",
        exception.getMessage());
  }

  @Test
  void getNormalizationFactorInterpolatesPreviousAndNextFunctions() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
    final FactorNormalizationFunction previousFunction = new FactorNormalizationFunction(file,
        timestamp, 2d);
    final FactorNormalizationFunction nextFunction = new FactorNormalizationFunction(file,
        timestamp, 4d);

    final InterpolatedNormalizationFunction interpolation = new InterpolatedNormalizationFunction(
        file, timestamp, previousFunction, 0.25d, nextFunction, 0.75d);

    assertEquals(3.5d, interpolation.getNormalizationFactor(100d, 5f), 1e-12);
  }
}
