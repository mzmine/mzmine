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

package io.github.mzmine.datamodel.features.columnar_data.mmap;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import io.github.mzmine.datamodel.features.types.alignment.AlignmentScores;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;

public class AlignmenScoreMemorySegmentColumn extends AbstractMemorySegmentColumn<AlignmentScores> {

  //float rate, int alignedFeatures, int extraFeatures,
  //                              Float weightedDistanceScore, Float mzPpmDelta, Double maxMzDelta,
  //                              Float maxRtDelta, Float maxMobilityDelta)

  public static final StructLayout LAYOUT = MemoryLayout.structLayout( //
      JAVA_FLOAT.withName("rate") //
      , ValueLayout.JAVA_INT.withName("alignedFeatures") //
      , ValueLayout.JAVA_INT.withName("extraFeatures") //
      , JAVA_FLOAT.withName("weightedDistanceScore") //
      , JAVA_FLOAT.withName("mzPpmDelta") //
      , ValueLayout.JAVA_DOUBLE_UNALIGNED.withName("maxMzDelta")
      // needs padding before this otherwsie not aligned
      , JAVA_FLOAT.withName("maxRtDelta") //
      , JAVA_FLOAT.withName("maxMobilityDelta") //
  );

  public static final long OFFSET = LAYOUT.byteSize();

  public static long[] offsets = new long[]{ //
      calcOffset(0, 1, 0, 0)//
      , calcOffset(0, 1, 0, 1)//
      , calcOffset(0, 1, 0, 2)//
      , calcOffset(0, 2, 0, 2)//
      , calcOffset(0, 3, 0, 2)//
      , calcOffset(1, 3, 0, 2)//
      , calcOffset(1, 4, 0, 2)//
  };


  public AlignmenScoreMemorySegmentColumn(final MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected MemoryLayout getValueLayout() {
    return LAYOUT;
  }

  @Override
  public AlignmentScores get(final int index) {
    long offset = index * OFFSET;

    float rate = data.get(JAVA_FLOAT, offset);
    if (Float.isNaN(rate)) {
      return null;
    }

    return new AlignmentScores( //
        rate, //
        data.get(JAVA_INT, offset + offsets[0]), //
        data.get(JAVA_INT, offset + offsets[1]), //
        data.get(JAVA_FLOAT, offset + offsets[2]), //
        data.get(JAVA_FLOAT, offset + offsets[3]), //
        data.get(JAVA_DOUBLE_UNALIGNED, offset + offsets[4]), //
        data.get(JAVA_FLOAT, offset + offsets[5]), //
        data.get(JAVA_FLOAT, offset + offsets[6]) //
    );
  }

  @Override
  public void set(final MemorySegment data, final int index, final AlignmentScores value) {
    long offset = index * OFFSET;
    if (value == null) {
      data.set(JAVA_FLOAT, index * OFFSET, Float.NaN);
      return;
    }

    data.set(JAVA_FLOAT, offset, value.rate());
    data.set(JAVA_INT, offset + offsets[0], value.alignedFeatures());
    data.set(JAVA_INT, offset + offsets[1], value.extraFeatures());
    data.set(JAVA_FLOAT, offset + offsets[2], value.weightedDistanceScore());
    data.set(JAVA_FLOAT, offset + offsets[3], value.mzPpmDelta());
    data.set(JAVA_DOUBLE_UNALIGNED, offset + offsets[4], value.maxMzDelta());
    data.set(JAVA_FLOAT, offset + offsets[5], value.maxRtDelta());
    data.set(JAVA_FLOAT, offset + offsets[6], value.maxMobilityDelta());
  }

  private static long calcOffset(int doubles, int floats, int longs, int ints) {
    return JAVA_DOUBLE_UNALIGNED.byteSize() * doubles + JAVA_FLOAT.byteSize() * floats
           + JAVA_LONG.byteSize() * longs + JAVA_INT.byteSize() * ints;
  }
}
