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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import com.google.common.collect.Range;

public class BuildingPASEFMsMsInfo {

  private final double precursorMz;
  private final Range<Integer> spectrumNumberRange;
  private final float collisionEnergy;
  private final int precursorCharge;
  private final int parentFrameNumber;
  private final int fragmentFrameNumber;


  public BuildingPASEFMsMsInfo(double precursorMz,
      Range<Integer> spectrumNumberRange, float collisionEnergy, int precursorCharge,
      int parentFrameNumber, int fragmentFrameNumber) {
    this.precursorMz = precursorMz;
    this.spectrumNumberRange = spectrumNumberRange;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.parentFrameNumber = parentFrameNumber;
    this.fragmentFrameNumber = fragmentFrameNumber;
  }

  public double getLargestPeakMz() {
    return precursorMz;
  }

  public Range<Integer> getSpectrumNumberRange() {
    return spectrumNumberRange;
  }

  public float getCollisionEnergy() {
    return collisionEnergy;
  }

  public int getPrecursorCharge() {
    return precursorCharge;
  }

  public int getParentFrameNumber() {
    return parentFrameNumber;
  }

  public int getFrameNumber() {
    return fragmentFrameNumber;
  }
}
