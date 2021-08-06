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

package io.github.mzmine.datamodel;

/**
 * Represents the type of chromatogram.
 */
public enum ChromatogramType {

  /**
   * Total ion current (TIC) chromatogram. This is the summed intensity of all ions in each
   * spectrum.
   */
  TIC,

  /**
   * Base peak chromatogram (BPC). This is the intensity of the most intense ion in each spectrum.
   */
  BPC,

  /**
   * Extracted-ion chromatogram (XIC). Also referred to as EIC or reconstructed-ion chromatogram
   * (RIC). This is the intensity of one or more m/z values in each spectrum.
   */
  XIC,

  /**
   * Single ion current (SIC) chromatogram. Only a narrow range of m/z values are detected in the
   * analysis and a chromatogram similar to the XIC is generated.
   */
  SIC,

  /**
   * Multiple Reaction Monitoring (MRM) or Selected Reaction Monitoring (SRM). A specific product
   * ion of a specific parent ion is detected.
   */
  MRM_SRM,

  /**
   * Unknown chromatogram type.
   */
  UNKNOWN;
}
