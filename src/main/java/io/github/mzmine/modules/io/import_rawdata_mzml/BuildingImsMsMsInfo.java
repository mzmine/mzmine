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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;

public class BuildingImsMsMsInfo {

  private final double precursorMz;
  private final float collisionEnergy;
  private final int precursorCharge;
  private final int fragmentFrameNumber;
  private final int firstSpectrumNumber;
  private int parentFrameNumber;
  private int lastSpectrumNumber;

  public BuildingImsMsMsInfo(final double precursorMz, final float collisionEnergy,
      final int precursorCharge, final int fragmentFrameNumber, final int firstSpectrumNumber) {
    this.precursorMz = precursorMz;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.firstSpectrumNumber = firstSpectrumNumber;
    this.fragmentFrameNumber = fragmentFrameNumber;
    this.lastSpectrumNumber = firstSpectrumNumber;
    parentFrameNumber = 0;
  }

  public double getPrecursorMz() {
    return precursorMz;
  }

  public int getFragmentFrameNumber() {
    return fragmentFrameNumber;
  }

  public int getFirstSpectrumNumber() {
    return firstSpectrumNumber;
  }

  public int getLastSpectrumNumber() {
    return lastSpectrumNumber;
  }

  public void setLastSpectrumNumber(int lastSpectrumNumber) {
    this.lastSpectrumNumber = lastSpectrumNumber;
  }

  public double getLargestPeakMz() {
    return precursorMz;
  }

  public Range<Integer> getSpectrumNumberRange() {
    throw new UnsupportedOperationException();
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

  public PasefMsMsInfoImpl build(Frame parentFrame, Frame thisFragmentFrame) {
    return new PasefMsMsInfoImpl(precursorMz, Range.closed(firstSpectrumNumber, lastSpectrumNumber),
        collisionEnergy, precursorCharge, parentFrame, thisFragmentFrame, null);
  }
}
