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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;

/**
 * Precursor information stored in IMS MS2 frames regarding their respective sub spectra.
 */
public interface ImsMsMsInfo {

  /**
   *
   * @return The m/z of the detected precursor.
   */
  public double getPrecursorMz();

  /**
   *
   * @return The range of spectra numbers in this frame where this precursor was fragmented in.
   */
  public Range<Integer> getSpectrumNumberRange();

  /**
   *
   * @return Collision energy this precursor was fragmented at in the given range.
   */
  public float getCollisionEnergy();

  /**
   * @return The charge of the precursor. 0 = unknown.
   */
  public int getPrecursorCharge();

  /**
   *
   * @return The frame this precursor was initially detected in.
   */
  public int getParentFrameNumber();

}
