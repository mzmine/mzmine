/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
