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

import io.github.mzmine.datamodel.PrecursorInfo;
import java.util.Set;

/**
 * Contains information of precursors in regular LC-MS/MS experiments.
 */
public class SimplePrecursorInfo implements PrecursorInfo {

  protected final double precursorMz;
  protected final int representativeScanNumber;
  protected final int precursorCharge;
  protected final MsMsInfo msMsInfo;

  /**
   * @param precursorMz              the m/z of this precursor.
   * @param representativeScanNumber Representative MS1 scan.
   * @param precursorCharge          The charge of this precursor. 0 if unknown.
   * @param msMsInfo                 {@link MsMsInfo} of this precursor
   */
  public SimplePrecursorInfo(double precursorMz, int representativeScanNumber,
      int precursorCharge, MsMsInfo msMsInfo) {
    this.precursorMz = precursorMz;
    this.representativeScanNumber = representativeScanNumber;
    this.precursorCharge = precursorCharge;
    this.msMsInfo = msMsInfo;
  }

  @Override
  public double getPrecursorMZ() {
    return precursorMz;
  }

  @Override
  public int getRepresentativeScanNumber() {
    return representativeScanNumber;
  }

  @Override
  public int getCharge() {
    return precursorCharge;
  }

  @Override
  public Set<MsMsInfo> getMsMsInfo() {
    return Set.of(msMsInfo);
  }
}
