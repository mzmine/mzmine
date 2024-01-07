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


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>MzMLIsolationWindow class.</p>
 */
public class MzMLIsolationWindow extends MzMLCVGroup {

  private static final Logger logger = Logger.getLogger(MzMLIsolationWindow.class.getName());

  private Integer msLevel;

  public void setMSLevel(String level) {
    try {
      this.msLevel = Integer.parseInt(level);
    } catch (Exception ex) {
      logger.log(Level.WARNING, ex.getMessage(), ex);
    }
  }

  /**
   * Important: This is the MS level in which the precursor was selected. So an MS2 scan has level 1
   * for precursor selection.This value might not be set in all mzML files. It is a user parameter
   * in MSconvert to signal MS level, which is important for MSn experiments.
   *
   * @return the MS level of the precursor selection MS2 was selected from level 1. Or null if not
   * defined
   */
  public Integer getMsLevelFromUserParam() {
    return msLevel;
  }
}
