/*
 * (C) Copyright 2015-2017 by MSDK Development Team
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

/**
 * <p>MzMLReferenceableParamGroup class.</p>
 *
 */
public class MzMLReferenceableParamGroup extends MzMLCVGroup {

  private String paramGroupName;

  /**
   * <p>Constructor for MzMLReferenceableParamGroup.</p>
   *
   * @param paramGroupName a {@link String} object.
   */
  public MzMLReferenceableParamGroup(String paramGroupName) {
    this.paramGroupName = paramGroupName;
  }

  /**
   * <p>Getter for the field <code>paramGroupName</code>.</p>
   *
   * @return a {@link String} object.
   */
  public String getParamGroupName() {
    return paramGroupName;
  }

  /**
   * <p>Setter for the field <code>paramGroupName</code>.</p>
   *
   * @param paramGroupName a {@link String} object.
   */
  public void setParamGroupName(String paramGroupName) {
    this.paramGroupName = paramGroupName;
  }

}
