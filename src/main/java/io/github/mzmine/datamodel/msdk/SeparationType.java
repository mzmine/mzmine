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
package io.github.mzmine.datamodel.msdk;

/**
 *
 * Enum for different separation technologies used as a preprocessing step before the actual data
 * point acquisition.
 * <p>
 * A separation type will usually provide additional information for acquired {@link MsScan}
 * objects. The most common case being a chromatographic separation coupled to a mass spectrometer.
 * Here, the chromatograph would provide the retention time of each acquired data point as
 * additional information.
 */
public enum SeparationType {

  /**
   * Gas chromatography.
   */
  GC(1),
  /**
   * Comprehensive two-dimensional gas chromatography.
   */
  GCxGC(2),
  /**
   * Liquid chromatography.
   */
  LC(1),
  /**
   * Comprehensive two-dimensional liquid chromatography.
   */
  LCxLC(2),
  /**
   * Capillary electrophoresis chromatography.
   */
  CE(1),
  /**
   * Ion mobility spectrometry. First dimension is retention time, second dimension is ion drift
   * time.
   */
  IMS(2),
  /**
   * Unknown separation method. Expect 1 feature dimension, e.g. retention time.
   */
  UNKNOWN(1), UNKNOWN_2D(2);

  private final int featureDimensions;

  SeparationType(int separationDimensions) {
    this.featureDimensions = separationDimensions;
  }

  /**
   * Return the number of additional features linked to an {@link MsScan} and to
   * <p>
   * For chromatography, the number will be 1 for one-dimensional chromatography, where each data
   * point has a retention time assigned to it.
   * <p>
   * The number will be 2 for (comprehensive) two-dimensional chromatography, where each data point
   * has two retention times assigned to it.
   *
   * @return the number of feature dimensions.
   */
  public int getFeatureDimensions() {
    return this.featureDimensions;
  }
}
