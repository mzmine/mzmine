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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * Derives the detector saturation level of a raw file from the data: a saturated detector clips
 * many data points at (almost) the same highest intensity, while the highest point of an
 * unsaturated file is a single peak. The base peak of every spectrum of one type (mobility scans or
 * frames and other scans) and MS level is compared with the highest base peak of the file.
 */
public final class SaturationPlateauEstimator {

  /**
   * Base peaks within this relative distance of the highest base peak belong to the plateau
   */
  public static final double PLATEAU_TOLERANCE = 0.01;
  /**
   * Minimum number of spectra with a base peak on the plateau to call the file saturated
   */
  public static final int MIN_PLATEAU_SCANS = 10;

  private SaturationPlateauEstimator() {
  }

  /**
   * @param file    the raw file
   * @param msLevel ms level of the frames to check
   * @return the plateau intensity (highest mobility scan intensity of the file) or NaN if the
   * highest intensity does not occur repeatedly
   */
  public static double estimateMobilityScanPlateau(@NotNull final IMSRawDataFile file,
      final int msLevel) {
    final DoubleArrayList basePeaks = new DoubleArrayList();
    for (final Frame frame : file.getFrames()) {
      if (frame.getMSLevel() != msLevel) {
        continue;
      }
      for (final MobilityScan scan : frame.getMobilityScans()) {
        addBasePeak(scan, basePeaks);
      }
    }
    return estimatePlateau(basePeaks.toDoubleArray());
  }

  /**
   * @param file    the raw file
   * @param msLevel ms level of the scans to check
   * @return the plateau intensity (highest scan intensity of the file) or NaN if the highest
   * intensity does not occur repeatedly. The scans of an ion mobility file are its frames.
   */
  public static double estimateScanPlateau(@NotNull final RawDataFile file, final int msLevel) {
    final DoubleArrayList basePeaks = new DoubleArrayList();
    for (final Scan scan : file.getScans()) {
      if (scan.getMSLevel() == msLevel) {
        addBasePeak(scan, basePeaks);
      }
    }
    return estimatePlateau(basePeaks.toDoubleArray());
  }

  /**
   * @param file    the raw file
   * @param msLevel ms level of the scans to check
   * @return the highest base peak of the scans of the ms level (the frames of an ion mobility
   * file), 0 if there are none
   */
  public static double maxScanBasePeak(@NotNull final RawDataFile file, final int msLevel) {
    double max = 0;
    for (final Scan scan : file.getScans()) {
      final Double basePeak = scan.getBasePeakIntensity();
      if (scan.getMSLevel() == msLevel && basePeak != null) {
        max = Math.max(max, basePeak);
      }
    }
    return max;
  }

  private static void addBasePeak(@NotNull final Scan scan,
      @NotNull final DoubleArrayList basePeaks) {
    final Double basePeak = scan.getBasePeakIntensity();
    if (basePeak != null) {
      basePeaks.add(basePeak.doubleValue());
    }
  }

  /**
   * @param basePeaks base peak intensity of each spectrum
   * @return the highest base peak if at least {@link #MIN_PLATEAU_SCANS} base peaks are within
   * {@link #PLATEAU_TOLERANCE} of it, otherwise NaN
   */
  public static double estimatePlateau(final double @NotNull [] basePeaks) {
    double max = 0;
    for (final double basePeak : basePeaks) {
      max = Math.max(max, basePeak);
    }
    if (max <= 0) {
      return Double.NaN;
    }

    final double plateauStart = max * (1 - PLATEAU_TOLERANCE);
    int plateauScans = 0;
    for (final double basePeak : basePeaks) {
      if (basePeak >= plateauStart && ++plateauScans >= MIN_PLATEAU_SCANS) {
        return max;
      }
    }
    return Double.NaN;
  }
}
