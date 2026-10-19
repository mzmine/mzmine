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

package io.github.mzmine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MathUtilsTest {

  @Test
  void greatestCommonDivisor() {
    assertEquals(2, MathUtils.greatestCommonDivisor(2, 6));
    assertEquals(1, MathUtils.greatestCommonDivisor(1, 2));
    assertEquals(1, MathUtils.greatestCommonDivisor(2, 5));
    assertEquals(2, MathUtils.greatestCommonDivisor(2, 2));
    assertEquals(2, MathUtils.greatestCommonDivisor(2, 4));
    assertEquals(3, MathUtils.greatestCommonDivisor(6, 9));
  }

  @Test
  void undirectedPairingIsSymmetric() {
    assertEquals(MathUtils.undirectedPairing(5, 17), MathUtils.undirectedPairing(17, 5));
    assertEquals(MathUtils.undirectedPairing(0, 0), MathUtils.undirectedPairing(0, 0));
    assertEquals(MathUtils.undirectedPairing(32632, 32904),
        MathUtils.undirectedPairing(32904, 32632));
  }

  @Test
  void undirectedPairingHasNoCollisionsForLargeIds() {
    // these two pairs collided with the former Cantor pairing that overflowed int
    assertNotEquals(MathUtils.undirectedPairing(59, 302),
        MathUtils.undirectedPairing(32632, 32904));

    // ids around the former int overflow threshold (a + b >= 46341)
    final Set<Long> keys = new HashSet<>();
    int pairs = 0;
    for (int a = 1; a <= 200; a++) {
      for (int b = 46200; b <= 46500; b++) {
        keys.add(MathUtils.undirectedPairing(a, b));
        keys.add(MathUtils.undirectedPairing(b, b + a));
        pairs += 2;
      }
    }
    assertEquals(pairs, keys.size());
    assertTrue(keys.add(MathUtils.undirectedPairing(Integer.MAX_VALUE, Integer.MAX_VALUE)));
  }

  @Test
  void pairingRoundTrip() {
    final int[][] pairs = new int[][]{{0, 0}, {1, 2}, {59, 302}, {32632, 32904}, {46341, 46342},
        {1, Integer.MAX_VALUE}, {Integer.MAX_VALUE, Integer.MAX_VALUE}, {-7, 13}};
    for (final int[] pair : pairs) {
      final long directed = MathUtils.directedPairing(pair[0], pair[1]);
      assertEquals(pair[0], MathUtils.pairingA(directed));
      assertEquals(pair[1], MathUtils.pairingB(directed));

      // undirected always packs the smaller value first
      final long undirected = MathUtils.undirectedPairing(pair[1], pair[0]);
      assertEquals(Math.min(pair[0], pair[1]), MathUtils.pairingA(undirected));
      assertEquals(Math.max(pair[0], pair[1]), MathUtils.pairingB(undirected));
    }
  }
}