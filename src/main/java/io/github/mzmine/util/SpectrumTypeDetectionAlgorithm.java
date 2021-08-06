/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-detection of spectrum type from data points. Determines if the spectrum represented by given
 * array of data points is centroided or continuous (profile or thresholded). Profile spectra are
 * easy to detect, because they contain zero-intensity data points. However, distinguishing
 * centroided from thresholded spectra is not trivial. We use multiple checks for that purpose, as
 * described in the code comments.
 */
public class SpectrumTypeDetectionAlgorithm {

  /**
   * <p>
   * detectSpectrumType.
   * </p>
   *
   * @param msSpectrum a {@link MassSpectrum} object.
   * @return a {@link MassSpectrumType} object.
   */
  public static MassSpectrumType detectSpectrumType(@NotNull MassSpectrum msSpectrum) {
    double mzValues[] = msSpectrum.getMzValues(null);
    double intensityValues[] = msSpectrum.getIntensityValues(null);
    Integer size = msSpectrum.getNumberOfDataPoints();
    return detectSpectrumType(mzValues, intensityValues, size);
  }

  /**
   * <p>
   * detectSpectrumType.
   * </p>
   *
   * @return a {@link MassSpectrumType} object.
   * @param mzValues an array of double.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public static @NotNull MassSpectrumType detectSpectrumType(@NotNull double mzValues[],
      @NotNull double intensityValues[], @NotNull Integer size) {

    // If the spectrum has less than 5 data points, it should be
    // centroided.
    if (size < 5)
      return MassSpectrumType.CENTROIDED;

    int basePeakIndex = 0;
    boolean hasZeroDataPoint = false;

    final double scanMzSpan = mzValues[size - 1] - mzValues[0];

    // Go through the data points and find the highest one
    for (int i = 0; i < size; i++) {

      // Update the topDataPointIndex accordingly
      if (intensityValues[i] > intensityValues[basePeakIndex])
        basePeakIndex = i;

      if (intensityValues[i] == 0.0)
        hasZeroDataPoint = true;
    }

    // Find the all data points around the base peak that have intensity
    // above half maximum
    final double halfIntensity = intensityValues[basePeakIndex] / 2.0;
    int leftIndex = basePeakIndex;
    while ((leftIndex > 0) && intensityValues[leftIndex - 1] > halfIntensity) {
      leftIndex--;
    }
    int rightIndex = basePeakIndex;
    while ((rightIndex < size - 1) && intensityValues[rightIndex + 1] > halfIntensity) {
      rightIndex++;
    }
    final double mainPeakMzSpan = mzValues[rightIndex] - mzValues[leftIndex];
    final int mainPeakDataPointCount = rightIndex - leftIndex + 1;

    // If the main peak has less than 3 data points above half intensity, it
    // indicates a centroid spectrum. Further, if the m/z span of the main
    // peak is more than 0.1% of the scan m/z range, it also indicates a
    // centroid spectrum. These criteria are empirical and probably not
    // bulletproof. However, it works for all the test cases we have.
    if ((mainPeakDataPointCount < 3) || (mainPeakMzSpan > (scanMzSpan / 1000.0)))
      return MassSpectrumType.CENTROIDED;
    else {
      if (hasZeroDataPoint)
        return MassSpectrumType.PROFILE;
      else
        return MassSpectrumType.THRESHOLDED;
    }

  }

}
