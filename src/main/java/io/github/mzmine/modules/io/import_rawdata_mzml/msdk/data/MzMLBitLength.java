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

/**
 * Enumeration of different types of precision bit lengths which are parsed by the MzML Parser
 */
public enum MzMLBitLength {
  THIRTY_TWO_BIT_INTEGER("MS:1000519"), // 32-bit integer
  SIXTEEN_BIT_FLOAT("MS:1000520"), // 16-bit float
  THIRTY_TWO_BIT_FLOAT("MS:1000521"), // 32-bit float
  SIXTY_FOUR_BIT_INTEGER("MS:1000522"), // 64-bit integer
  SIXTY_FOUR_BIT_FLOAT("MS:1000523"); // 64-bit float

  private final String accession;

  MzMLBitLength(String accession) {
    this.accession = accession;
  }

  /**
   * <p>getValue.</p>
   *
   * @return the CV Parameter accession of the precision bit length as {@link String
   *         String}
   */
  public String getValue() {
    return accession;
  }

}
