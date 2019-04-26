/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry;

public enum DBEntryField {

  ENTRY_ID, NAME, SYNONYM, COMMENT, IONTYPE, RT(Double.class), MZ(Double.class), CHARGE(
      Integer.class), ION_MODE, COLLISION_ENERGY, FORMULA, MOLWEIGHT(Double.class), EXACT_MASS(
          Double.class), INCHI, INCHIKEY, SMILES, CAS, PUBMED, PUBCHEM, MONA_ID, CHEMSPIDER, INSTRUMENT_TYPE, INSTRUMENT, ION_SOURCE, NUM_PEAKS(
              Integer.class), ACQUISITION, PRINZIPLE_INVESTIGATOR, DATA_COLLECTOR, SOFTWARE, MS_LEVEL, RESOLUTION;

  private final Class clazz;

  DBEntryField() {
    this(String.class);
  }

  DBEntryField(Class clazz) {
    this.clazz = clazz;
  }

  public Class getObjectClass() {
    return clazz;
  }

  /**
   *
   * @return The gnps json format key or an empty String
   */
  public String getGnpsJsonID() {
    switch (this) {
      case ACQUISITION:
        return "ACQUISITION";
      case SOFTWARE:
        return "softwaresource";
      case CAS:
        return "CASNUMBER";
      case CHARGE:
        return "CHARGE";
      case COLLISION_ENERGY:
        return "FRAGMENTATION_METHOD";
      case COMMENT:
        return "description";
      case DATA_COLLECTOR:
        return "DATACOLLECTOR";
      case EXACT_MASS:
        return "EXACTMASS";
      case FORMULA:
        return "FORMULA";
      case INCHI:
        return "INCHI";
      case INCHIKEY:
        return "INCHIAUX";
      case INSTRUMENT:
        return "INSTRUMENT_NAME";
      case INSTRUMENT_TYPE:
        return "INSTRUMENT";
      case IONTYPE:
        return "ADDUCT";
      case ION_MODE:
        return "IONMODE";
      case ION_SOURCE:
        return "IONSOURCE";
      case MZ:
        return "MZ";
      case NAME:
        return "COMPOUND_NAME";
      case PRINZIPLE_INVESTIGATOR:
        return "PI";
      case PUBMED:
        return "PUBMED";
      case RT:
        return "RT";
      case SMILES:
        return "SMILES";
      case MS_LEVEL:
        return "MS_LEVEL";
      case PUBCHEM:
        return "PUBCHEM";
      case CHEMSPIDER:
        return "CHEMSPIDER";
      case MONA_ID:
        return "MONA_ID";
      case RESOLUTION:
      case NUM_PEAKS:
      case ENTRY_ID:
      case SYNONYM:
      case MOLWEIGHT:
        return "";
      default:
        return "";
    }
  }

  /**
   * DBENtryField for GNPS json key
   * 
   * @param key
   * @return
   */
  public static DBEntryField forGnpsJasonID(String key) {
    for (DBEntryField f : values()) {
      if (f.getGnpsJsonID().equals(key))
        return f;
    }
    return null;
  }

  /**
   *
   * @return The NIST MSP format key or an empty String
   */
  public String getNistMspID() {
    switch (this) {
      case ENTRY_ID:
        return "DB#";
      case ACQUISITION:
        return "";
      case SOFTWARE:
        return "";
      case CAS:
        return "";
      case CHARGE:
        return "";
      case COLLISION_ENERGY:
        return "Collision_energy";
      case COMMENT:
        return "Comments";
      case DATA_COLLECTOR:
        return "";
      case EXACT_MASS:
        return "ExactMass";
      case FORMULA:
        return "Formula";
      case INCHI:
        return "";
      case INCHIKEY:
        return "InChIKey";
      case INSTRUMENT:
        return "Instrument";
      case INSTRUMENT_TYPE:
        return "Instrument_type";
      case IONTYPE:
        return "Precursor_type";
      case ION_MODE:
        return "Ion_mode"; // P / N
      case ION_SOURCE:
        return "";
      case MZ:
        return "PrecursorMZ";
      case NAME:
        return "Name";
      case PRINZIPLE_INVESTIGATOR:
        return "";
      case PUBMED:
        return "";
      case RT:
        return "RT";
      case SMILES:
        return "";
      case MS_LEVEL:
        return "Spectrum_type";
      case PUBCHEM:
        return "";
      case CHEMSPIDER:
        return "";
      case MONA_ID:
        return "";
      case NUM_PEAKS:
        return "Num Peaks";
      case RESOLUTION:
      case SYNONYM:
      case MOLWEIGHT:
        return "";
      default:
        return "";
    }
  }

  /**
   * DBENtryField for NIST msp key
   * 
   * @param key
   * @return
   */
  public static DBEntryField forMspID(String key) {
    for (DBEntryField f : values()) {
      if (f.getNistMspID().equals(key))
        return f;
    }
    return null;
  }

  /**
   * Converts the content to the correct value type
   * 
   * @param content
   * @return
   * @throws NumberFormatException
   */
  public Object convertValue(String content) throws NumberFormatException {
    if (getObjectClass() == Double.class)
      return Double.parseDouble(content);
    if (getObjectClass() == Float.class)
      return Float.parseFloat(content);
    if (getObjectClass() == Integer.class)
      return Integer.parseInt(content);
    return content;
  }
}
