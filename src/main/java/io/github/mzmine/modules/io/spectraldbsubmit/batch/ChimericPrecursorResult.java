/*
 * Copyright 2006-2022 The MZmine Development Team
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
package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.logging.Logger;

/**
 * Categories for chimeric spectra
 */
public enum ChimericPrecursorResult {
  PASSED("Clean precursor isolation"), MISSING_MAIN_SIGNAL(
      "Missing main precursor signal"), CHIMERIC("Chimeric precursor isolation");

  private static final Logger logger = Logger.getLogger(ChimericPrecursorResult.class.getName());
  private final String description;

  ChimericPrecursorResult(final String description) {
    this.description = description;
  }

  /**
   * Score if the mass list contains signals with a summed intensity ratio greater than the main
   * signal
   *
   * @param masses
   * @param precursorMz
   * @param mzTolMainSignal            search for the main signal in this range, should be lower
   *                                   than the isolation tolerance, depending on the mass accuracy
   *                                   and resolution
   * @param mzTolIsolation             precursor isolation tolerance might be higher than the
   *                                   actually set window. Depending on the performance of the
   *                                   instrument
   * @param allowedOtherSignalSumRatio the allowed ratio of all other signal intensities / main
   *                                   signal intensity
   * @return CHIMERIC if all other signals summed intensity ratio>main signal intensity
   */
  public static ChimericPrecursorResult check(final MassList masses, final double precursorMz,
      MZTolerance mzTolMainSignal, MZTolerance mzTolIsolation,
      final double allowedOtherSignalSumRatio) {
    int index = masses.binarySearch(precursorMz, true);
    if (index == -1) {
      return MISSING_MAIN_SIGNAL;
    }

    Range<Double> mainMzRange = mzTolMainSignal.getToleranceRange(precursorMz);
    //  isolation window should be larger than main mz range
    Range<Double> isolationMzRange = mzTolIsolation.getToleranceRange(precursorMz)
        .span(mainMzRange);

    double mainMz = -1;
    double mainIntensity = 0;
    double sumIntensity = 0;
    // left side
    for (int i = index; i >= 0; i--) {
      double mz = masses.getMzValue(i);
      if (mz < isolationMzRange.lowerEndpoint()) {
        break;
      }

      double intensity = masses.getIntensityValue(i);
      if (mainMzRange.contains(mz) && (mainMz < 0 || intensity > mainIntensity)) {
        mainMz = masses.getMzValue(i);
        mainIntensity = intensity;
      }
      if (isolationMzRange.contains(mz)) {
        sumIntensity += intensity;
      }
    }
    // right side
    for (int i = index + 1; i < masses.getNumberOfDataPoints(); i++) {
      double mz = masses.getMzValue(i);
      if (mz > isolationMzRange.upperEndpoint()) {
        break;
      }
      double intensity = masses.getIntensityValue(i);
      if (mainMzRange.contains(mz) && (mainMz < 0 || intensity > mainIntensity)) {
        mainMz = masses.getMzValue(i);
        mainIntensity = intensity;
      }
      if (isolationMzRange.contains(mz)) {
        sumIntensity += intensity;
      }
    }
    //
    if (mainMz < 0) {
      logger.finest(
          "No signal found in main mz range during chimerics detection for " + precursorMz);
      return ChimericPrecursorResult.MISSING_MAIN_SIGNAL;
    }

    // only allow that the remaining sum is <= to the allowedSumPercentage
    double ratio = (sumIntensity - mainIntensity) / mainIntensity;
    return ratio > allowedOtherSignalSumRatio ? CHIMERIC : PASSED;
  }

  public String getDescription() {
    return description;
  }

}
