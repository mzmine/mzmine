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
