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
 * Enumeration of different compression types which are parsed by the MzML Parser
 */
public enum MzMLCompressionType {
  NUMPRESS_LINPRED("MS:1002312", "MS-Numpress linear prediction compression"), //
  NUMPRESS_POSINT("MS:1002313", "MS-Numpress positive integer compression"), //
  NUMPRESS_SHLOGF("MS:1002314", "MS-Numpress short logged float compression"), //
  ZLIB("MS:1000574", "zlib compression"), //
  NO_COMPRESSION("MS:1000576", "no compression"), //
  NUMPRESS_LINPRED_ZLIB("MS:1002746",
      "MS-Numpress linear prediction compression followed by zlib compression"), //
  NUMPRESS_POSINT_ZLIB("MS:1002747",
      "MS-Numpress positive integer compression followed by zlib compression"), //
  NUMPRESS_SHLOGF_ZLIB("MS:1002748",
      "MS-Numpress short logged float compression followed by zlib compression");

  private String accession;
  private String name;

  MzMLCompressionType(String accession, String name) {
    this.accession = accession;
    this.name = name;
  }

  /**
   * <p>Getter for the field <code>accession</code>.</p>
   *
   * @return the CV Parameter accession of the compression type as {@link String String}
   */
  public String getAccession() {
    return accession;
  }

  /**
   * <p>Getter for the field <code>name</code>.</p>
   *
   * @return the CV Parameter name of the compression type as {@link String String}
   */
  public String getName() {
    return name;
  }

}


