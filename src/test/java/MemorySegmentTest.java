/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfDouble;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MemorySegmentTest {

  @Test
  void testLoad() {

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment segment = arena.allocate(Double.BYTES * 10,
          OfDouble.JAVA_DOUBLE.byteAlignment());

      SegmentAllocator alloc = SegmentAllocator.slicingAllocator(segment);

      final double[] stored = new double[]{15.345, 7.231, 123.34234};
      final MemorySegment allocated = alloc.allocateArray(ValueLayout.JAVA_DOUBLE, stored);

      // ok
      Assertions.assertEquals(stored[0], allocated.getAtIndex(ValueLayout.JAVA_DOUBLE, 0));

      final ByteBuffer byteBuffer = allocated.asByteBuffer().order(ByteOrder.nativeOrder());
      final double fromByteBuffer = byteBuffer.getDouble(0);
      Assertions.assertEquals(stored[0], fromByteBuffer);

      final DoubleBuffer doubleBuffer = allocated.asByteBuffer().order(ByteOrder.nativeOrder())
          .asDoubleBuffer();
      final double fromDoubleBuffer = doubleBuffer.get(0);
      Assertions.assertEquals(stored[0], fromDoubleBuffer);
    }
  }
}
