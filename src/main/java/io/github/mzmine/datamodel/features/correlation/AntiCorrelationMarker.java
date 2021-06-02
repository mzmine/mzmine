/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;

/**
 * Marker to flag a reason why a correlation is not used.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum AntiCorrelationMarker {
  /**
   * intensity range is not shared between these two rows the features do not overlap with X % of
   * their intensity
   */
  AntiOverlap,
  /**
   * Minimum number of shared features in samples or groups not met
   */
  MinFeaturesRequirementNotMet,
  /**
   * At least for one sample out of RT range
   */
  OutOfRTRange;

  public static AntiCorrelationMarker fromOverlapResult(
      MinimumFeatureFilter.OverlapResult overlap) {
    switch (overlap) {
      case AntiOverlap:
        return AntiCorrelationMarker.AntiOverlap;
      case BelowMinSamples:
        return AntiCorrelationMarker.MinFeaturesRequirementNotMet;
      case OutOfRTRange:
        return AntiCorrelationMarker.OutOfRTRange;
    }
    return null;
  }
}
