/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleOtherTimeSeries implements OtherTimeSeries {

  /**
   * doubles
   */
  protected final MemorySegment intensityBuffer;

  /**
   * floats
   */
  protected final MemorySegment timeBuffer;
  protected final String name;
  private final @NotNull OtherTimeSeriesData timeSeriesData;

  public SimpleOtherTimeSeries(@Nullable MemoryMapStorage storage, @NotNull float[] rtValues,
      @NotNull double[] intensityValues, String name, @NotNull OtherTimeSeriesData timeSeriesData) {
    if (intensityValues.length != rtValues.length) {
      throw new IllegalArgumentException("Intensities and RT values must have the same length");
    }
    this.timeSeriesData = timeSeriesData;
    intensityBuffer = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
    timeBuffer = StorageUtils.storeValuesToFloatBuffer(storage, rtValues);
    this.name = name;
  }

  public SimpleOtherTimeSeries(@NotNull MemorySegment rtValues,
      @NotNull MemorySegment intensityValues, String name,
      @NotNull OtherTimeSeriesData timeSeriesData) {
    if (StorageUtils.numDoubles(intensityValues) != StorageUtils.numFloats(rtValues)) {
      throw new IllegalArgumentException("Intensities and RT values must have the same length");
    }
    this.timeSeriesData = timeSeriesData;
    intensityBuffer = intensityValues;
    timeBuffer = rtValues;
    this.name = name;
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    return intensityBuffer;
  }

  @Override
  public float getRetentionTime(int index) {
    assert index < StorageUtils.numFloats(timeBuffer);
    return timeBuffer.getAtIndex(ValueLayout.JAVA_FLOAT, index);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ChromatogramType getChromatoogramType() {
    return getTimeSeriesData().getChromatogramType();
  }

  @Override
  public @NotNull OtherDataFile getOtherDataFile() {
    return timeSeriesData.getOtherDataFile();
  }

  @Override
  @NotNull
  public OtherTimeSeriesData getTimeSeriesData() {
    return timeSeriesData;
  }

  @Override
  public OtherTimeSeries copyAndReplace(MemoryMapStorage storage, double[] newIntensities,
      String newName) {
    if (getNumberOfValues() != newIntensities.length) {
      throw new IllegalArgumentException("The number of intensities does not match number of rts.");
    }

    return new SimpleOtherTimeSeries(timeBuffer,
        StorageUtils.storeValuesToDoubleBuffer(storage, newIntensities), newName,
        getTimeSeriesData());
  }

  @Override
  public IntensityTimeSeries subSeries(MemoryMapStorage storage, float start, float end) {
    // todo does this work with float to double?
    final IndexRange indexRange = BinarySearch.indexRange(start, end, getNumberOfValues(),
        this::getRetentionTime);
    return subSeries(storage, indexRange.min(), indexRange.maxExclusive());
  }

  @Override
  public IntensityTimeSeries subSeries(MemoryMapStorage storage, int startIndexInclusive,
      int endIndexExclusive) {

    return new SimpleOtherTimeSeries(
        StorageUtils.sliceFloats(timeBuffer, startIndexInclusive, endIndexExclusive),
        StorageUtils.sliceDoubles(intensityBuffer, startIndexInclusive, endIndexExclusive), name,
        timeSeriesData);
  }

  @Override
  public @Nullable MemoryMapStorage getStorage() {
    return getTimeSeriesData().getOtherDataFile().getCorrespondingRawDataFile()
        .getMemoryMapStorage();
  }
}
