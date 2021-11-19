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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel;

/**
 * @author https://github.com/SteffenHeu
 */
public enum BrukerScanMode {

  MS(0, "MS"), //
  AUTO_MSMS(1, "Auto MS/MS"), //
  MRM(2, "MRM"), //
  IN_SOURCE_CID(3, "in-source CID"), //
  BROADBAND_CID(4, "broadband CID"), //
  PASEF(8, "PASEF"), //
  DIA(9, "DIA"), //
  PRM(10, "PRM"), //
  MALDI(20, "MALDI"), //
  UNKNOWN(-1, "Unknown");

  private final int num;

  private final String description;

  BrukerScanMode(final int num, final String description) {
    this.num = num;
    this.description = description;
  }

  public static BrukerScanMode fromScanMode(final int scanMode) {
    for (BrukerScanMode mode : BrukerScanMode.values()) {
      if (mode.num == scanMode) {
        return mode;
      }
    }
    return BrukerScanMode.UNKNOWN;
  }

  public int getNum() {
    return num;
  }

  public String getDescription() {
    return description;
  }
}
