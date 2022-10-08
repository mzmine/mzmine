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
