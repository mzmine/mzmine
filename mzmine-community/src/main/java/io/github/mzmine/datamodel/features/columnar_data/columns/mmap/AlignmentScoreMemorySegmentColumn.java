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

import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.innercolumns.NullableDoubleInnerColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.innercolumns.NullableFloatInnerColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.innercolumns.NullableIntegerInnerColumn;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentScores;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import org.jetbrains.annotations.NotNull;
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

  private static final NullableDoubleInnerColumn maxMzDelta = new NullableDoubleInnerColumn(LAYOUT,
      "maxMzDelta");

  private static final NullableFloatInnerColumn rateHandle = new NullableFloatInnerColumn(LAYOUT,
      "rate");
  private static final NullableFloatInnerColumn weightedDistanceScore = new NullableFloatInnerColumn(
      LAYOUT, "weightedDistanceScore");
  private static final NullableFloatInnerColumn mzPpmDelta = new NullableFloatInnerColumn(LAYOUT,
      "mzPpmDelta");
  private static final NullableFloatInnerColumn maxRtDelta = new NullableFloatInnerColumn(LAYOUT,
      "maxRtDelta");
  private static final NullableFloatInnerColumn maxMobilityDelta = new NullableFloatInnerColumn(
      LAYOUT, "maxMobilityDelta");
  private static final NullableIntegerInnerColumn alignedFeatures = new NullableIntegerInnerColumn(
      LAYOUT, "alignedFeatures");
  private static final NullableIntegerInnerColumn extraFeatures = new NullableIntegerInnerColumn(
      LAYOUT, "extraFeatures");

  public AlignmentScoreMemorySegmentColumn(final @NotNull MemoryMapStorage storage,
      int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected @NotNull MemoryLayout getValueLayout() {
    return LAYOUT;
  }

  @Override
  public @Nullable AlignmentScores get(final int index) {
    // rate null means the whole alignmentscore is null
    Float rate = rateHandle.get(data, index);
    if (rate == null) {
      return null;
    }

    return new AlignmentScores( //
        rate, //
        alignedFeatures.get(data, index), //
        extraFeatures.get(data, index), //
        weightedDistanceScore.get(data, index), //
        mzPpmDelta.get(data, index), //
        maxMzDelta.get(data, index), //
        maxRtDelta.get(data, index), //
        maxMobilityDelta.get(data, index) //
    );
  }

  @Override
  public void set(@NotNull final MemorySegment data, final int index, final AlignmentScores value) {
    if (value == null) {
      rateHandle.clear(data, index);
      return;
    }

    rateHandle.set(data, index, value.rate());
    mzPpmDelta.set(data, index, value.mzPpmDelta());
    alignedFeatures.set(data, index, value.alignedFeatures());
    extraFeatures.set(data, index, value.extraFeatures());
    weightedDistanceScore.set(data, index, value.weightedDistanceScore());
    maxMzDelta.set(data, index, value.maxMzDelta());
    maxRtDelta.set(data, index, value.maxRtDelta());
    maxMobilityDelta.set(data, index, value.maxMobilityDelta());
  }

}
