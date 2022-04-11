/*
 * (C) Copyright 2015-2016 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

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
