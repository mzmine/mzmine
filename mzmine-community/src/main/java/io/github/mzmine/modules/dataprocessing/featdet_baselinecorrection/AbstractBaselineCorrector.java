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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.data_access.MobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBaselineCorrector implements BaselineCorrector {

  protected final MinimumSearchFeatureResolver resolver;
  protected final int numSamples;
  protected final MemoryMapStorage storage;
  protected final String suffix;
  protected final List<PlotXYDataProvider> additionalData = new ArrayList<>();
  protected final BaselineDataBuffer buffer = new BaselineDataBuffer();
  boolean preview = false;


  public AbstractBaselineCorrector(@Nullable MemoryMapStorage storage, int numSamples,
      @NotNull String suffix, @Nullable MinimumSearchFeatureResolver resolver) {

    this.storage = storage;
    this.numSamples = numSamples;
    this.suffix = suffix;
    this.resolver = resolver;
  }

  protected static @NotNull MinimumSearchFeatureResolver initializeLocalMinResolver(
      ModularFeatureList flist) {

    final MinimumSearchFeatureResolver resolver = new MinimumSearchFeatureResolver(flist,
        ResolvingDimension.RETENTION_TIME, 0.5, 0.04, 0, 0, 2.5, Range.closed(0d, 5d), 5);

    return resolver;
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
      case MobilogramDataAccess da -> {
        // TODO correct approach?
        var size = da.getNumberOfValues();
        double[] mzs = new double[size];
        for (int i = 0; i < size; i++) {
          mzs[i] = da.getMZ(i);
        }
        // TODO requires real mobilogram instance to create copy not the data access - maybe just implement the method internally?
        // maybe just make method use new intensity values and keep mz values
        yield (T) da.getCurrentMobilogram().copyAndReplace(storage, mzs, newIntensityValues);
      }
      case IonSpectrumSeries<?> s ->
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

  public double[] xBuffer() {
    return buffer.xBuffer();
  }

  public double[] yBuffer() {
    return buffer.yBuffer();
  }

  public double[] xBufferRemovedPeaks() {
    return buffer.xBufferRemovedPeaks();
  }

  public double[] yBufferRemovedPeaks() {
    return buffer.yBufferRemovedPeaks();
  }
}
