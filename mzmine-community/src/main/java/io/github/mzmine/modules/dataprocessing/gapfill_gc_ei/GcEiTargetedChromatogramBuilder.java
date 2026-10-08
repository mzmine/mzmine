/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogram;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds chromatograms (EICs) for target m/z values in one pass over the mass lists of a raw data
 * file. Applies the same filters as the {@link ModularADAPChromatogramBuilderTask}: minimum
 * absolute height, minimum number of consecutive scans above the group intensity, and one zero
 * intensity at both edges of each detected segment.
 * <p>
 * In contrast to the ADAP chromatogram builder, each chromatogram spans the m/z tolerance around
 * the target m/z instead of the most intense data point.
 */
class GcEiTargetedChromatogramBuilder {

  private final MZTolerance mzTolerance;
  private final int minConsecutiveScans;
  private final double minGroupIntensity;
  private final double minHighestPoint;

  GcEiTargetedChromatogramBuilder(@NotNull final GcEiFeatureFindingSettings settings) {
    mzTolerance = settings.mzTolerance();
    minConsecutiveScans = settings.minConsecutiveScans();
    minGroupIntensity = settings.minGroupIntensity();
    minHighestPoint = settings.minHighestPoint();
  }

  /**
   * @param file      the raw data file with mass lists
   * @param scans     the scans to build chromatograms on, usually the selected scans of the
   *                  feature list
   * @param targetMzs the m/z values to build chromatograms for
   * @return one chromatogram per target m/z, null for chromatograms that do not pass the filters of
   * the chromatogram builder. Stored in RAM.
   */
  @NotNull
  public List<@Nullable IonTimeSeries<Scan>> buildChromatograms(@NotNull final RawDataFile file,
      @NotNull final List<? extends Scan> scans, final double @NotNull [] targetMzs) {
    final int numScans = scans.size();
    final int numTargets = targetMzs.length;
    final double[] lowerMz = new double[numTargets];
    final double[] upperMz = new double[numTargets];
    for (int t = 0; t < numTargets; t++) {
      final Range<Double> mzRange = mzTolerance.getToleranceRange(targetMzs[t]);
      lowerMz[t] = mzRange.lowerEndpoint();
      upperMz[t] = mzRange.upperEndpoint();
    }

    // dense arrays [target][scan]: zero intensity marks a scan without data point
    final double[][] mzs = new double[numTargets][numScans];
    final double[][] intensities = new double[numTargets][numScans];

    final ScanDataAccess access = EfficientDataAccess.of(file, ScanDataType.MASS_LIST, scans);
    int scanIndex = 0;
    while (access.hasNextScan()) {
      access.nextScan();
      final int numDataPoints = access.getNumberOfDataPoints();
      for (int t = 0; t < numTargets; t++) {
        final int start = access.binarySearch(lowerMz[t], DefaultTo.GREATER_EQUALS);
        if (start < 0) {
          continue;
        }
        // like the ADAP chromatogram builder, keep the most intense data point per scan
        for (int i = start; i < numDataPoints; i++) {
          final double mz = access.getMzValue(i);
          if (mz > upperMz[t]) {
            break;
          }
          final double intensity = access.getIntensityValue(i);
          if (intensity > intensities[t][scanIndex]) {
            intensities[t][scanIndex] = intensity;
            mzs[t][scanIndex] = mz;
          }
        }
      }
      scanIndex++;
    }

    final List<IonTimeSeries<Scan>> chromatograms = new ArrayList<>(numTargets);
    for (int t = 0; t < numTargets; t++) {
      chromatograms.add(createChromatogram(scans, mzs[t], intensities[t]));
    }
    return chromatograms;
  }

  /**
   * @return the chromatogram or null if it does not pass the chromatogram builder filters
   */
  private @Nullable IonTimeSeries<Scan> createChromatogram(
      @NotNull final List<? extends Scan> scans, final double @NotNull [] mzs,
      final double @NotNull [] intensities) {
    int detected = 0;
    double mzSum = 0;
    double maxIntensity = 0;
    for (int i = 0; i < intensities.length; i++) {
      if (intensities[i] > 0) {
        detected++;
        mzSum += mzs[i];
        maxIntensity = Math.max(maxIntensity, intensities[i]);
      }
    }

    // the ADAP chromatogram builder only starts chromatograms with a data point >= min height
    if (detected < minConsecutiveScans || maxIntensity < minHighestPoint
        || !matchesMinContinuousDataPoints(intensities)) {
      return null;
    }

    // zero data points use the mean m/z, like ADAPChromatogram#addNZeros(scans, 1, 1)
    final double meanMz = mzSum / detected;
    final List<Scan> seriesScans = new ArrayList<>();
    final DoubleArrayList seriesMzs = new DoubleArrayList();
    final DoubleArrayList seriesIntensities = new DoubleArrayList();
    for (int i = 0; i < intensities.length; i++) {
      final boolean isDetected = intensities[i] > 0;
      final boolean isEdge = (i > 0 && intensities[i - 1] > 0) || (i < intensities.length - 1
          && intensities[i + 1] > 0);
      if (isDetected || isEdge) {
        seriesScans.add(scans.get(i));
        seriesMzs.add(isDetected ? mzs[i] : meanMz);
        seriesIntensities.add(isDetected ? intensities[i] : 0d);
      }
    }

    return new SimpleIonTimeSeries(null, seriesMzs.toDoubleArray(),
        seriesIntensities.toDoubleArray(), seriesScans);
  }

  /**
   * Same check as {@link ADAPChromatogram#matchesMinContinuousDataPoints(Scan[], double, int,
   * double)} on dense intensities.
   */
  private boolean matchesMinContinuousDataPoints(final double @NotNull [] intensities) {
    if (minConsecutiveScans <= 1) {
      return true;
    }
    int connectedScans = 0;
    double maxHeight = 0d;
    for (final double intensity : intensities) {
      if (intensity > 0 && intensity >= minGroupIntensity) {
        connectedScans++;
        maxHeight = Math.max(maxHeight, intensity);
        if (connectedScans >= minConsecutiveScans && maxHeight >= minHighestPoint) {
          return true;
        }
      } else {
        connectedScans = 0;
      }
    }
    return false;
  }
}
