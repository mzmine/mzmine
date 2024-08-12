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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.IndexRange;
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
  protected double[] xBuffer = new double[0];
  protected double[] yBuffer = new double[0];
  protected double[] xBufferRemovedPeaks = new double[0];
  protected double[] yBufferRemovedPeaks = new double[0];
  boolean preview = false;

  public AbstractBaselineCorrector(@Nullable MemoryMapStorage storage, int numSamples,
      @NotNull String suffix, @Nullable MinimumSearchFeatureResolver resolver) {

    this.storage = storage;
    this.numSamples = numSamples;
    this.suffix = suffix;
    this.resolver = resolver;
  }

  /**
   * Removes the given list of index ranges from the array, always keeping the first and last value
   * even if they are contained in one of the ranges. This may be needed for
   * {@link org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction}, because it will
   * not extrapolate beyond the sides.
   *
   * @param indices   The list of index ranges. May be empty.
   * @param numValues the number of values in the array. Can be used to limit the search if
   *                  {@param src} is a buffer.
   * @param src       The source to copy values frm
   * @param dst       The destination to write the new array to.
   * @return The number of values written to the array.
   */
  public static int removeRangesFromArray(List<IndexRange> indices, int numValues, double[] src,
      double[] dst) {
    int startInRemovedArray = 0;
    int lastEndPointInOriginalArray = 0;

    if (indices.isEmpty()) {
      System.arraycopy(src, 0, dst, 0, numValues);
      startInRemovedArray = numValues;
      return startInRemovedArray;
    } else {
      if (indices.getFirst().min() == 0) {
        dst[0] = src[0];
        startInRemovedArray++;
        lastEndPointInOriginalArray++;
      }

      for (int i = 0; i < indices.size(); i++) {
        final IndexRange range = indices.get(i);
        final int numPoints = range.min() - lastEndPointInOriginalArray;

        // in case the first range starts at 0 and the first point was copied manually, this condition is not met.
        if (numPoints > 0) {
          System.arraycopy(src, lastEndPointInOriginalArray, dst, startInRemovedArray, numPoints);
          startInRemovedArray += numPoints;
        }
        lastEndPointInOriginalArray = range.maxExclusive();
      }
    }

    // add last value
    if (indices.getLast().maxExclusive() >= numValues) {
      dst[startInRemovedArray] = src[numValues - 1];
      startInRemovedArray++;
    } else {
      // add values until the end
      System.arraycopy(src, lastEndPointInOriginalArray, dst, startInRemovedArray,
          numValues - lastEndPointInOriginalArray);
      startInRemovedArray += numValues - lastEndPointInOriginalArray;
    }
    return startInRemovedArray;
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
    return switch (timeSeries) {
      case IonSpectrumSeries<?> s -> (T) s.copyAndReplace(storage, s.getMzValues(new double[0]),
          Arrays.copyOfRange(newIntensities, 0, numValues));
      case OtherTimeSeries o -> (T) o.copyAndReplace(
          o.getTimeSeriesData().getOtherDataFile().getCorrespondingRawDataFile()
              .getMemoryMapStorage(), Arrays.copyOfRange(newIntensities, 0, numValues),
          o.getName() + " " + suffix);
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
