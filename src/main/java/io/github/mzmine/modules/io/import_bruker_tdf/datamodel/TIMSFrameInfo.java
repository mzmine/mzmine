/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.import_bruker_tdf.datamodel;

import io.github.mzmine.datamodel.PolarityType;

/**
 * @author https://github.com/SteffenHeu
 */
public class TIMSFrameInfo {

  private final int id;
  private final double time;
  private final PolarityType polarityType;
  private final int scanMode;
  private final int msmsType;
  private final int maxIntensity;
  private final int test;

  public TIMSFrameInfo(int id, double time, PolarityType polarityType,
      int scanMode, int msmsType, int maxIntensity, int test) {
    this.id = id;
    this.time = time;
    this.polarityType = polarityType;
    this.scanMode = scanMode;
    this.msmsType = msmsType;
    this.maxIntensity = maxIntensity;
    this.test = test;
  }
}
