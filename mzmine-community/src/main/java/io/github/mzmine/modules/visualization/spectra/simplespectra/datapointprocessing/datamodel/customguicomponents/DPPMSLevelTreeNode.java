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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;

/**
 * Stores MSLevel in a tree item. Used to organize the tree view automatically. Every
 * {@link MSLevel} is automatically added in {@link ProcessingComponent}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPMSLevelTreeNode extends DisableableTreeNode {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private MSLevel mslevel;

  public DPPMSLevelTreeNode(MSLevel mslevel) {
    super(mslevel.toString());
    setMSLevel(mslevel);
  }

  public MSLevel getMSLevel() {
    return mslevel;
  }

  private void setMSLevel(MSLevel mslevel) {
    this.mslevel = mslevel;
  }
}
