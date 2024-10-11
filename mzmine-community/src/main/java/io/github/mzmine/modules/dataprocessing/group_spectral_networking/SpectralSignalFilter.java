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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingTask;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Filter used before spectral matching to reduce number of signals, see
 * {@link SignalFiltersParameters} and {@link ModifiedCosineSpectralNetworkingTask}
 *
 * @param isRemovePrecursor                        remove precursor signals
 * @param removePrecursorMz                        range to remove around the precursor +-
 * @param cropToMaxSignals                         abs max signals
 * @param signalThresholdForTargetIntensityPercent above this number of signals, the signals are
 *                                                 filtered to include only the top N signals that
 *                                                 make up X% of the intensity. This might include
 *                                                 fewer than the specified number of signals.
 * @param targetIntensityPercentage                filter signals to retain X% intensity. e.g.,
 *                                                 0.98
 */
public record SpectralSignalFilter(boolean isRemovePrecursor, double removePrecursorMz,
                                   int cropToMaxSignals,
                                   int signalThresholdForTargetIntensityPercent,
                                   double targetIntensityPercentage) {

  public static final SpectralSignalFilter DEFAULT = new SpectralSignalFilter(true, 10d, 250, 50,
      0.98);

  /**
   * Apply signal filters and sort by {@link DataPointSorter#DEFAULT_INTENSITY}
   *
   * @param scan        data source
   * @param precursorMz precursor mz
   * @return null if <minDP otherwise the DataPoint[] sorted by intensity
   * @throws MissingMassListException apply mass detection first
   */
  @Nullable
  public DataPoint[] applyFilterAndSortByIntensity(final @NotNull Scan scan,
      final double precursorMz) throws MissingMassListException {
    return applyFilterAndSortByIntensity(scan, precursorMz, -1);
  }

  /**
   * Apply signal filters and sort by {@link DataPointSorter#DEFAULT_INTENSITY}
   *
   * @param scan  data source
   * @param minDP minimum number of data points or -1 to deactivate
   * @return null if <minDP otherwise the DataPoint[] sorted by intensity
   * @throws MissingMassListException apply mass detection first
   */
  @Nullable
  public DataPoint[] applyFilterAndSortByIntensity(final @NotNull Scan scan, final int minDP)
      throws MissingMassListException {
    Double precursorMz = scan.getPrecursorMz();
    if (precursorMz == null) {
      return null;
    }
    return applyFilterAndSortByIntensity(scan, precursorMz, minDP);
  }

  /**
   * Apply signal filters and sort by {@link DataPointSorter#DEFAULT_INTENSITY}
   *
   * @param scan        data source
   * @param precursorMz precursor mz
   * @param minDP       minimum number of data points or -1 to deactivate
   * @return null if <minDP otherwise the DataPoint[] sorted by intensity
   * @throws MissingMassListException apply mass detection first
   */
  @Nullable
  public DataPoint[] applyFilterAndSortByIntensity(final @NotNull Scan scan,
      final double precursorMz, final int minDP) throws MissingMassListException {
    MassList masses = scan.getMassList();
    if (masses == null) {
      throw new MissingMassListException(scan);
    }
    if (masses.getNumberOfDataPoints() < minDP) {
      return null;
    }
    DataPoint[] dps = masses.getDataPoints();
    return applyFilterAndSortByIntensity(dps, precursorMz, minDP);
  }

  /**
   * Apply signal filters and sort by {@link DataPointSorter#DEFAULT_INTENSITY}
   *
   * @param dps         spectral data points
   * @param precursorMz precursor mz
   * @return null if <minDP otherwise the DataPoint[] sorted by intensity
   * @throws MissingMassListException apply mass detection first
   */
  @Nullable
  public DataPoint[] applyFilterAndSortByIntensity(final @NotNull DataPoint[] dps,
      final Double precursorMz) {
    return applyFilterAndSortByIntensity(dps, precursorMz, -1);
  }

  /**
   * Apply signal filters and sort by {@link DataPointSorter#DEFAULT_INTENSITY}
   *
   * @param dps         spectral data points
   * @param precursorMz precursor mz
   * @param minDP       minimum number of data points or -1 to deactivate
   * @return null if <minDP otherwise the DataPoint[] sorted by intensity
   * @throws MissingMassListException apply mass detection first
   */
  @Nullable
  public DataPoint[] applyFilterAndSortByIntensity(@NotNull DataPoint[] dps,
      final Double precursorMz, final int minDP) {
    // remove precursor signals
    if (isRemovePrecursor && removePrecursorMz > 0 && precursorMz != null && precursorMz > 0) {
      dps = DataPointUtils.removePrecursorMz(dps, precursorMz, removePrecursorMz);
      if (dps.length < minDP) {
        return null;
      }
    }

    // sort by intensity
    Arrays.sort(dps, DataPointSorter.DEFAULT_INTENSITY);

    // apply some filters to avoid noisy spectra with too many signals
    if (dps.length > signalThresholdForTargetIntensityPercent) {
      dps = DataPointUtils.filterDataByIntensityPercent(dps, targetIntensityPercentage,
          cropToMaxSignals);
    }

    if (dps.length < minDP) {
      return null;
    }
    return dps;
  }

}
