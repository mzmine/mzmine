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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;

/**
 * @see io.github.mzmine.datamodel.ImsMsMsInfo
 *
 * @author https://github.com/SteffenHeu
 */
public class ImsMsMsInfoImpl implements ImsMsMsInfo {

  private final double precursorMz;
  private final Range<Integer> spectrumNumberRange;
  private final float collisionEnergy;
  private final int precursorCharge;
  private final Frame parentFrameNumber;
  private final Frame fragmentFrameNumber;


  public ImsMsMsInfoImpl(double precursorMz,
      Range<Integer> spectrumNumberRange, float collisionEnergy, int precursorCharge,
      Frame parentFrameNumber, Frame fragmentFrameNumber) {
    this.precursorMz = precursorMz;
    this.spectrumNumberRange = spectrumNumberRange;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.parentFrameNumber = parentFrameNumber;
    this.fragmentFrameNumber = fragmentFrameNumber;
  }

  @Override
  public double getLargestPeakMz() {
    return precursorMz;
  }

  @Override
  public Range<Integer> getSpectrumNumberRange() {
    return spectrumNumberRange;
  }

  @Override
  public float getCollisionEnergy() {
    return collisionEnergy;
  }

  @Override
  public int getPrecursorCharge() {
    return precursorCharge;
  }

  @Override
  public Frame getParentFrameNumber() {
    return parentFrameNumber;
  }

  @Override
  public Frame getFrameNumber() {
    return fragmentFrameNumber;
  }

  @Override
  public String toString() {
    return "m/z " + precursorMz + " - Scans " + spectrumNumberRange.toString();
  }
}
