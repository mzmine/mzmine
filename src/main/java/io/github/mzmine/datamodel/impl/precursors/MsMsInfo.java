/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.datamodel.impl.precursors;

import com.google.common.collect.Range;

public class MsMsInfo {

  private final Range<Integer> scanNumberRange;
  private final Float collisionEnergy;
  private final int frameNumber;

  public MsMsInfo(Range<Integer> scanNumberRange, Float collisionEnergy) {
    this(-1, scanNumberRange, collisionEnergy);
  }

  /**
   * @param frameNumber The frame number the given range and collision energy is valid for.
   * @param scanNumberRange Scan/spectrum number range this precursor was fragmented with the
   *                        given CE
   * @param collisionEnergy The collision energy.
   */
  public MsMsInfo(int frameNumber, Range<Integer> scanNumberRange, Float collisionEnergy) {
    this.scanNumberRange = scanNumberRange;
    this.collisionEnergy = collisionEnergy;
    this.frameNumber = frameNumber;
  }

  public Range<Integer> getScanNumberRange() {
    return scanNumberRange;
  }

  public Float getCollisionEnergy() {
    return collisionEnergy;
  }
}
