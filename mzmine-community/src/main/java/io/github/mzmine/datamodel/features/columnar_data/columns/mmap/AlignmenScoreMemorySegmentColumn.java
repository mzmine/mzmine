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

package io.github.mzmine.datamodel.features.columnar_data.columns.mmap;

import io.github.mzmine.datamodel.features.types.alignment.AlignmentScores;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import java.lang.invoke.VarHandle;
import org.jetbrains.annotations.Nullable;

public class AlignmenScoreMemorySegmentColumn extends AbstractMemorySegmentColumn<AlignmentScores> {

  public static final StructLayout LAYOUT = MemoryLayout.structLayout( //
      // Double needs to be at start otherwise layout may be unaligned
      JAVA_DOUBLE.withName("maxMzDelta"), //
      JAVA_FLOAT.withName("rate") //
      , JAVA_INT.withName("alignedFeatures") //
      , JAVA_INT.withName("extraFeatures") //
      , JAVA_FLOAT.withName("weightedDistanceScore") //
      , JAVA_FLOAT.withName("mzPpmDelta") //
      , JAVA_FLOAT.withName("maxRtDelta") //
      , JAVA_FLOAT.withName("maxMobilityDelta") //
  );

  private static final VarHandle rateHandle = LAYOUT.varHandle(PathElement.groupElement("rate"));
  private static final VarHandle alignedFeaturesHandle = LAYOUT.varHandle(
      PathElement.groupElement("alignedFeatures"));
  private static final VarHandle extraFeaturesHandle = LAYOUT.varHandle(
      PathElement.groupElement("extraFeatures"));
  private static final VarHandle weightedDistanceScoreHandle = LAYOUT.varHandle(
      PathElement.groupElement("weightedDistanceScore"));
  private static final VarHandle mzPpmDeltaHandle = LAYOUT.varHandle(
      PathElement.groupElement("mzPpmDelta"));
  private static final VarHandle maxMzDeltaHandle = LAYOUT.varHandle(
      PathElement.groupElement("maxMzDelta"));
  private static final VarHandle maxRtDeltaHandle = LAYOUT.varHandle(
      PathElement.groupElement("maxRtDelta"));
  private static final VarHandle maxMobilityDeltaHandle = LAYOUT.varHandle(
      PathElement.groupElement("maxMobilityDelta"));

  public AlignmenScoreMemorySegmentColumn(final MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected MemoryLayout getValueLayout() {
    return LAYOUT;
  }

  @Override
  public @Nullable AlignmentScores get(final int index) {
    final long offset = LAYOUT.byteSize() * index;

    float rate = (float) rateHandle.get(data, offset);
    if (Float.isNaN(rate)) {
      return null;
    }

    return new AlignmentScores( //
        rate, //
        (int) alignedFeaturesHandle.get(data, offset), //
        (int) extraFeaturesHandle.get(data, offset), //
        (float) weightedDistanceScoreHandle.get(data, offset), //
        (float) mzPpmDeltaHandle.get(data, offset), //
        (double) maxMzDeltaHandle.get(data, offset), //
        (float) maxRtDeltaHandle.get(data, offset), //
        (float) maxMobilityDeltaHandle.get(data, offset) //
    );
  }

  @Override
  public void set(final MemorySegment data, final int index, final AlignmentScores value) {
    final long offset = LAYOUT.byteSize() * index;
    if (value == null) {
      rateHandle.set(data, offset, Float.NaN);
      return;
    }

    rateHandle.set(data, offset, value.rate());
    mzPpmDeltaHandle.set(data, offset, value.mzPpmDelta());
    alignedFeaturesHandle.set(data, offset, value.alignedFeatures());
    extraFeaturesHandle.set(data, offset, value.extraFeatures());
    weightedDistanceScoreHandle.set(data, offset, value.weightedDistanceScore());
    maxMzDeltaHandle.set(data, offset, value.maxMzDelta());
    maxRtDeltaHandle.set(data, offset, value.maxRtDelta());
    maxMobilityDeltaHandle.set(data, offset, value.maxMobilityDelta());
  }

}
