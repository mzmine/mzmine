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

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.mzmine.datamodel.features.types.alignment.AlignmentScores;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;
import org.jetbrains.annotations.Nullable;

public class AlignmentScoreMemorySegmentColumn extends
    AbstractMemorySegmentColumn<AlignmentScores> {

  public static final StructLayout LAYOUT = MemoryLayout.structLayout( //
      // Double needs to be at start otherwise layout may be unaligned
      JAVA_DOUBLE.withName("maxMzDelta") //
      , JAVA_FLOAT.withName("rate") //
      , JAVA_INT.withName("alignedFeatures") //
      , JAVA_INT.withName("extraFeatures") //
      , JAVA_FLOAT.withName("weightedDistanceScore") //
      , JAVA_FLOAT.withName("mzPpmDelta") //
      , JAVA_FLOAT.withName("maxRtDelta") //
      , JAVA_FLOAT.withName("maxMobilityDelta"), //
      // add padding to avoid unaligned memory (uneven number of int/floats)
      MemoryLayout.paddingLayout(4) //
  );

  private static final VarHandle rateHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("rate"));
  private static final VarHandle alignedFeaturesHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("alignedFeatures"));
  private static final VarHandle extraFeaturesHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("extraFeatures"));
  private static final VarHandle weightedDistanceScoreHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("weightedDistanceScore"));
  private static final VarHandle mzPpmDeltaHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("mzPpmDelta"));
  private static final VarHandle maxMzDeltaHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("maxMzDelta"));
  private static final VarHandle maxRtDeltaHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("maxRtDelta"));
  private static final VarHandle maxMobilityDeltaHandle = LAYOUT.arrayElementVarHandle(
      PathElement.groupElement("maxMobilityDelta"));

  public AlignmentScoreMemorySegmentColumn(final MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected MemoryLayout getValueLayout() {
    return LAYOUT;
  }

  @Override
  public @Nullable AlignmentScores get(final int index) {
    float rate = (float) rateHandle.get(data, 0L, index);
    if (Float.isNaN(rate)) {
      return null;
    }

    return new AlignmentScores( //
        rate, //
        (int) alignedFeaturesHandle.get(data, 0L, index), //
        (int) extraFeaturesHandle.get(data, 0L, index), //
        (float) weightedDistanceScoreHandle.get(data, 0L, index), //
        (float) mzPpmDeltaHandle.get(data, 0L, index), //
        (double) maxMzDeltaHandle.get(data, 0L, index), //
        (float) maxRtDeltaHandle.get(data, 0L, index), //
        (float) maxMobilityDeltaHandle.get(data, 0L, index) //
    );
  }

  @Override
  public void set(final MemorySegment data, final int index, final AlignmentScores value) {
    if (value == null) {
      rateHandle.set(data, 0L, index, Float.NaN);
      return;
    }

    rateHandle.set(data, 0L, index, value.rate());
    mzPpmDeltaHandle.set(data, 0L, index, value.mzPpmDelta());
    alignedFeaturesHandle.set(data, 0L, index, value.alignedFeatures());
    extraFeaturesHandle.set(data, 0L, index, value.extraFeatures());
    weightedDistanceScoreHandle.set(data, 0L, index, value.weightedDistanceScore());
    maxMzDeltaHandle.set(data, 0L, index, value.maxMzDelta());
    maxRtDeltaHandle.set(data, 0L, index, value.maxRtDelta());
    maxMobilityDeltaHandle.set(data, 0L, index, value.maxMobilityDelta());
  }

}
