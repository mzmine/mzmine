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

package io.github.mzmine.datamodel.impl.precursors;

import io.github.mzmine.datamodel.PrecursorInfo;
import java.util.Set;

public class IonMobilityPrecursorInfo extends SimplePrecursorInfo implements PrecursorInfo {

  /**
   * Key = Frame Ids of MS/MS Frames
   * <p></p>
   * Value = Scan number range of the scans this precursor was fragmented in
   */
  private final Set<MsMsInfo> imsMsMsInfo;

  public IonMobilityPrecursorInfo(double precursorMz, int representativeScanNumber, int precursorCharge,
      Set<MsMsInfo> imsMsMsInfo) {
    super(precursorMz, representativeScanNumber, precursorCharge, null);
    this.imsMsMsInfo = imsMsMsInfo;
  }

  @Override
  public Set<MsMsInfo> getMsMsInfo() {
    return imsMsMsInfo;
  }

}
