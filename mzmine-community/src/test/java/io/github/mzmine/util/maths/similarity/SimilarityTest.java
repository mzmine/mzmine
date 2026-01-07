/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.maths.similarity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SimilarityTest {

  @Test
  void testCosine() {
    // no need to normalize input vectors for cosine similarity
    // already normalized internally - this test proves this
    double[] a = new double[]{3, 2.0, 1.0, 4.0};
    double[] large = Arrays.stream(a).map(v -> v * 1000).toArray();

    assertEquals(1d, cos(a, large), 0.00001);
    // test when setting one value to 0 (missing)
    double[] copyA = Arrays.copyOf(a, a.length);
    copyA[0] = 0;
    double cosA = cos(copyA, large);

    double[] copyLarge = Arrays.copyOf(large, large.length);
    copyLarge[0] = 0;
    double cosLarge = cos(a, copyLarge);
    assertEquals(cosA, cosLarge, 0.00001);

    // test when a factor is applied
    copyA = Arrays.copyOf(a, a.length);
    copyA[0] *= 10;
    cosA = cos(copyA, large);

    copyLarge = Arrays.copyOf(large, large.length);
    copyLarge[0] *= 10;
    cosLarge = cos(a, copyLarge);
    assertEquals(cosA, cosLarge, 0.00001);

    // largest value is multiplied
    copyA = Arrays.copyOf(a, a.length);
    copyA[3] *= 10;
    cosA = cos(copyA, large);

    copyLarge = Arrays.copyOf(large, large.length);
    copyLarge[3] *= 10;
    cosLarge = cos(a, copyLarge);
    assertEquals(cosA, cosLarge, 0.00001);
  }

  private static double cos(final double[] a, final double[] large) {
    return Similarity.COSINE.calc(a, large);
  }

}