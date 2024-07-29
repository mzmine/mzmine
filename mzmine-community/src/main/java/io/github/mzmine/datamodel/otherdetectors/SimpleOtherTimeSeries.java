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

import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleOtherTimeSeries implements OtherTimeSeries {

  protected final DoubleBuffer intensityBuffer;
  protected final FloatBuffer timeBuffer;
  protected final String name;
  private final OtherTimeSeriesData timeSeriesData;

  public SimpleOtherTimeSeries(@Nullable MemoryMapStorage storage, @NotNull float[] rtValues,
      @NotNull double[] intensityValues, String name, OtherTimeSeriesData timeSeriesData) {
    this.timeSeriesData = timeSeriesData;
    intensityBuffer = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
    timeBuffer = StorageUtils.storeValuesToFloatBuffer(storage, rtValues);
    this.name = name;
  }

  @Override
  public DoubleBuffer getIntensityValueBuffer() {
    return intensityBuffer;
  }

  @Override
  public float getRetentionTime(int index) {
    assert index < timeBuffer.limit();
    return timeBuffer.get(index);
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
  public OtherDataFile getOtherDataFile() {
    return timeSeriesData.getOtherDataFile();
  }

  @Override
  public OtherTimeSeriesData getTimeSeriesData() {
    return timeSeriesData;
  }
}
