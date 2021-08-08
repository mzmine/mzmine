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

package io.github.mzmine.modules.io.import_rawdata_mzml.data;

import java.util.Optional;

/**
 * <p>MzMLScan class.</p>
 *
 */
public class MzMLScan extends MzMLCVGroup {

  Optional<MzMLScanWindowList> scanWindowList;

  /**
   * <p>Constructor for MzMLScan.</p>
   */
  public MzMLScan() {
    this.scanWindowList = Optional.ofNullable(null);
  }

  /**
   * <p>Getter for the field <code>scanWindowList</code>.</p>
   *
   * @return a {@link Optional} object.
   */
  public Optional<MzMLScanWindowList> getScanWindowList() {
    return scanWindowList;
  }

  /**
   * <p>Setter for the field <code>scanWindowList</code>.</p>
   *
   * @param scanWindowList a {@link MzMLScanWindowList} object.
   */
  public void setScanWindowList(MzMLScanWindowList scanWindowList) {
    this.scanWindowList = Optional.ofNullable(scanWindowList);
  }

}
