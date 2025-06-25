/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBaselineCorrector implements BaselineCorrector {

  protected final double samplePercentage;
  protected final MemoryMapStorage storage;
  protected final String suffix;
  protected final List<PlotXYDataProvider> additionalData = new ArrayList<>();
  protected final BaselineDataBuffer buffer = new BaselineDataBuffer();
  boolean preview = false;


  public AbstractBaselineCorrector(@Nullable MemoryMapStorage storage, double samplePercentage,
      @NotNull String suffix) {

    this.storage = storage;
    this.samplePercentage = samplePercentage;
    this.suffix = suffix;
  }

  protected XYDataArrays subSampleData(final IntList subsampleIndices, final double[] xDataFiltered,
      final double[] yDataFiltered, final int numValuesFiltered) {

    final double[] subsampleX = BaselineCorrector.subsample(xDataFiltered, numValuesFiltered,
        subsampleIndices, true);
    final double[] subsampleY = BaselineCorrector.subsample(yDataFiltered, numValuesFiltered,
        subsampleIndices, false);

    // somehow first and last data point are often 0 - maybe because MS switches to early/late?
    // adjust subsampleY to nearest non 0 value max 2 indices away
    if (Double.compare(subsampleY[0], 0d) == 0) {
      int index = subsampleIndices.getInt(0);
      for (int next = 1; next <= 2 && index + next < numValuesFiltered; next++) {
        double intensity = yDataFiltered[index + next];
        if (intensity > 0) {
          subsampleY[0] = intensity;
          break;
        }
      }
    }
    if (Double.compare(subsampleY[subsampleY.length - 1], 0d) == 0) {
      int index = subsampleIndices.getLast();
      for (int next = 1; next <= 2 && index - next >= 0; next++) {
        double intensity = yDataFiltered[index - next];
        if (intensity > 0) {
          subsampleY[subsampleY.length - 1] = intensity;
          break;
        }
      }
    }
    return new XYDataArrays(subsampleX, subsampleY);
  }


  /**
   * @param timeSeries     The original time series
   * @param numValues      The number of values to extract from the buffer.
   * @param newIntensities A buffer containing the new intensities. numValues are copied into the
   *                       new time series from this buffer.
   * @param <T>
   * @return
   */
  protected <T extends IntensityTimeSeries> T createNewTimeSeries(T timeSeries, int numValues,
      double[] newIntensities) {
    final double[] newIntensityValues = Arrays.copyOfRange(newIntensities, 0, numValues);
    return switch (timeSeries) {
      case FeatureDataAccess da -> (T) da.copyAndReplace(storage, newIntensityValues);
      case IonTimeSeries<?> s ->
          (T) s.copyAndReplace(storage, s.getMzValues(new double[0]), newIntensityValues);
      case OtherTimeSeries o -> (T) o.copyAndReplace(
          o.getTimeSeriesData().getOtherDataFile().getCorrespondingRawDataFile()
              .getMemoryMapStorage(), newIntensityValues, o.getName() + " " + suffix);
      default -> throw new IllegalStateException(
          "Unexpected time series: " + timeSeries.getClass().getName());
    };
  }

  public boolean isPreview() {
    return preview;
  }

  public void setPreview(boolean preview) {
    this.preview = preview;
  }

  @Override
  public List<PlotXYDataProvider> getAdditionalPreviewData() {
    return additionalData;
  }

}
