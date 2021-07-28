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

import java.util.ArrayList;

/**
 * <p>MzMLScanWindowList class.</p>
 *
 */
public class MzMLScanWindowList {
  private ArrayList<MzMLScanWindow> scanWindows;

  /**
   * <p>Constructor for MzMLScanWindowList.</p>
   */
  public MzMLScanWindowList() {
    this.scanWindows = new ArrayList<>();
  }

  /**
   * <p>Getter for the field <code>scanWindows</code>.</p>
   *
   * @return a {@link ArrayList} object.
   */
  public ArrayList<MzMLScanWindow> getScanWindows() {
    return scanWindows;
  }

  /**
   * <p>addScanWindow.</p>
   *
   * @param scanWindow a {@link MzMLScanWindow} object.
   */
  public void addScanWindow(MzMLScanWindow scanWindow) {
    scanWindows.add(scanWindow);
  }

}
