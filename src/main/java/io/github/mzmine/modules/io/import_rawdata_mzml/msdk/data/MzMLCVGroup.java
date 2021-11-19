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
 * <p>
 * A group (or list) of {@link MzMLCVParam CV Parameter}s
 * </p>
 */
public class MzMLCVGroup {
  private ArrayList<MzMLCVParam> cvParams;

  /**
   * <p>Constructor for MzMLCVGroup.</p>
   */
  public MzMLCVGroup() {
    this.cvParams = new ArrayList<>();
  }

  /**
   * <p>getCVParamsList.</p>
   *
   * @return an {@link ArrayList ArrayList<MzMLCVParam>} of
   *         {@link MzMLCVParam CV Parameter}s
   */
  public ArrayList<MzMLCVParam> getCVParamsList() {
    return cvParams;
  }

  /**
   * <p>
   * Adds a {@link MzMLCVParam CV Parameter} to the
   * {@link MzMLCVGroup MzMLCVGroup}
   * </p>
   *
   * @param cvParam the {@link MzMLCVParam CV Parameter} to be added to
   *        the {@link MzMLCVGroup MzMLCVGroup}
   */
  public void addCVParam(MzMLCVParam cvParam) {
    cvParams.add(cvParam);
  }
}
