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
