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

package io.github.mzmine.util.spectraldb.entry;

import org.apache.commons.lang3.StringUtils;

public enum DBEntryField {

  ENTRY_ID, NAME, SYNONYM, COMMENT, ION_TYPE, RT(Float.class), MZ(Double.class), CHARGE(
      Integer.class), ION_MODE, COLLISION_ENERGY, FORMULA, MOLWEIGHT(Double.class), EXACT_MASS(
      Double.class), INCHI, INCHIKEY, SMILES, CAS, PUBMED, PUBCHEM, MONA_ID, CHEMSPIDER, INSTRUMENT_TYPE, INSTRUMENT, ION_SOURCE, NUM_PEAKS(
      Integer.class), ACQUISITION, PRINCIPAL_INVESTIGATOR, DATA_COLLECTOR, SOFTWARE, MS_LEVEL, RESOLUTION, CCS(
      Float.class);

  // group of DBEntryFields logically
  public static final DBEntryField[] OTHER_FIELDS = new DBEntryField[]{PRINCIPAL_INVESTIGATOR,
      DATA_COLLECTOR, ENTRY_ID, COMMENT};
  public static final DBEntryField[] DATABASE_FIELDS = new DBEntryField[]{PUBMED, PUBCHEM, MONA_ID,
      CHEMSPIDER, CAS};
  public static final DBEntryField[] COMPOUND_FIELDS = new DBEntryField[]{NAME, SYNONYM, FORMULA,
      MOLWEIGHT, EXACT_MASS, ION_TYPE, MZ, CHARGE, RT, CCS, ION_MODE, INCHI, INCHIKEY, SMILES,
      NUM_PEAKS};
  public static final DBEntryField[] INSTRUMENT_FIELDS = new DBEntryField[]{INSTRUMENT_TYPE,
      INSTRUMENT, ION_SOURCE, RESOLUTION, MS_LEVEL, COLLISION_ENERGY, ACQUISITION, SOFTWARE};

  private final Class clazz;

  DBEntryField() {
    this(String.class);
  }

  DBEntryField(Class clazz) {
    this.clazz = clazz;
  }

  /**
   * DBENtryField for GNPS json key
   *
   * @param key
   * @return
   */
  public static DBEntryField forGnpsJasonID(String key) {
    for (DBEntryField f : values()) {
      // equalsIgnoreCase is more robust against changes in library
      // consistency
      if (f.getGnpsJsonID().equalsIgnoreCase(key)) {
        return f;
      }
    }
    return null;
  }

  /**
   * DBENtryField for NIST msp key
   *
   * @param key
   * @return
   */
  public static DBEntryField forMspID(String key) {
    for (DBEntryField f : values()) {
      // equalsIgnoreCase is more robust against changes in library
      // consistency
      if (f.getNistMspID().equalsIgnoreCase(key)) {
        return f;
      }
    }
    return null;
  }

  /**
   * DBENtryField for mgf (GNPS) key
   *
   * @param key
   * @return
   */
  public static DBEntryField forMgfID(String key) {
    for (DBEntryField f : values()) {
      // equalsIgnoreCase is more robust against changes in library
      // consistency
      if (f.getMgfID().equalsIgnoreCase(key)) {
        return f;
      }
    }
    return null;
  }

  /**
   * DBENtryField for JDX key
   *
   * @param key
   * @return
   */
  public static DBEntryField forJdxID(String key) {
    for (DBEntryField f : values()) {
      // equalsIgnoreCase is more robust against changes in library
      // consistency
      if (f.getJdxID().equalsIgnoreCase(key)) {
        return f;
      }
    }
    return null;
  }

  public Class getObjectClass() {
    return clazz;
  }

  @Override
  public String toString() {
    return switch (this) {
      case RT, SMILES, CAS -> super.toString().replace('_', ' ');
      case ENTRY_ID -> "Entry ID";
      case INCHI -> "InChI";
      case INCHIKEY -> "InChI key";
      case MOLWEIGHT -> "Mol. weight";
      case MONA_ID -> "MoNA ID";
      case MZ -> "Precursor m/z";
      default -> StringUtils.capitalize(super.toString().replace('_', ' ').toLowerCase());
    };
  }

  /**
   * @return The gnps json format key or an empty String
   */
  public String getGnpsJsonID() {
    return switch (this) {
      case ACQUISITION -> "ACQUISITION";
      case SOFTWARE -> "softwaresource";
      case CAS -> "CASNUMBER";
      case CHARGE -> "CHARGE";
      case COLLISION_ENERGY -> "FRAGMENTATION_METHOD";
      case COMMENT -> "description";
      case DATA_COLLECTOR -> "DATACOLLECTOR";
      case EXACT_MASS -> "EXACTMASS";
      case FORMULA -> "FORMULA";
      case INCHI -> "INCHI";
      case INCHIKEY -> "INCHIAUX";
      case INSTRUMENT -> "INSTRUMENT_NAME";
      case INSTRUMENT_TYPE -> "INSTRUMENT";
      case ION_TYPE -> "ADDUCT";
      case ION_MODE -> "IONMODE";
      case ION_SOURCE -> "IONSOURCE";
      case MZ -> "MZ";
      case NAME -> "COMPOUND_NAME";
      case PRINCIPAL_INVESTIGATOR -> "PI";
      case PUBMED -> "PUBMED";
      case RT -> "RT";
      case SMILES -> "SMILES";
      case MS_LEVEL -> "MS_LEVEL";
      case PUBCHEM -> "PUBCHEM";
      case CHEMSPIDER -> "CHEMSPIDER";
      case MONA_ID -> "MONA_ID";
      case CCS -> "CCS";
      case RESOLUTION, NUM_PEAKS, ENTRY_ID, SYNONYM, MOLWEIGHT -> "";
    };
  }

  /**
   * @return The NIST MSP format key or an empty String
   */
  public String getNistMspID() {
    return switch (this) {
      case ENTRY_ID -> "DB#";
      case COLLISION_ENERGY -> "Collision_energy";
      case COMMENT -> "Comments";
      case EXACT_MASS -> "ExactMass";
      case FORMULA -> "Formula";
      case INCHIKEY -> "InChIKey";
      case INSTRUMENT -> "Instrument";
      case INSTRUMENT_TYPE -> "Instrument_type";
      case ION_TYPE -> "Precursor_type";
      case ION_MODE -> "Ion_mode"; // P / N
      case ION_SOURCE -> "";
      case MZ -> "PrecursorMZ";
      case NAME -> "Name";
      case RT -> "RT";
      case MS_LEVEL -> "Spectrum_type";
      case NUM_PEAKS -> "Num Peaks";
      case CCS -> "CCS";
      case SMILES -> "SMILES";
      case ACQUISITION, MONA_ID, CHEMSPIDER, RESOLUTION, SYNONYM, MOLWEIGHT, PUBCHEM, PUBMED, PRINCIPAL_INVESTIGATOR, CHARGE, CAS, SOFTWARE, INCHI, DATA_COLLECTOR -> "";
    };
  }

  /**
   * @return The mgf format (used by GNPS)
   */
  public String getMgfID() {
    return switch (this) {
      case ENTRY_ID -> "SPECTRUMID";
      case CHARGE -> "CHARGE";
      case COMMENT -> "ORGANISM";
      case DATA_COLLECTOR -> "DATACOLLECTOR";
      case EXACT_MASS -> "ExactMass";
      case FORMULA -> "Formula";
      case INCHI -> "INCHI";
      case INCHIKEY -> "INCHIAUX";
      case INSTRUMENT -> "SOURCE_INSTRUMENT";
      case INSTRUMENT_TYPE -> "Instrument_type";
      case ION_TYPE -> "Precursor_type";
      case ION_MODE -> "IONMODE"; // Positive Negative
      case ION_SOURCE -> "";
      case MZ -> "PEPMASS";
      case NAME -> "NAME";
      case PRINCIPAL_INVESTIGATOR -> "PI";
      case PUBMED -> "PUBMED";
      case SMILES -> "SMILES";
      case MS_LEVEL -> "MSLEVEL";
      case CCS -> "CCS";
      case ACQUISITION, NUM_PEAKS, MONA_ID, CHEMSPIDER, PUBCHEM, RT, RESOLUTION, SYNONYM, MOLWEIGHT, CAS, SOFTWARE, COLLISION_ENERGY -> "";
    };
  }

  /**
   * @return The JCAMP-DX format key or an empty String
   */
  public String getJdxID() {
    return switch (this) {
      case ENTRY_ID -> "";
      case ACQUISITION -> "";
      case SOFTWARE -> "";
      case CAS -> "##CAS REGISTRY NO";
      case CHARGE -> "";
      case COLLISION_ENERGY -> "";
      case COMMENT -> "";
      case DATA_COLLECTOR -> "";
      case EXACT_MASS -> "##MW";
      case FORMULA -> "##MOLFORM";
      case INCHI -> "";
      case INCHIKEY -> "";
      case INSTRUMENT -> "";
      case INSTRUMENT_TYPE -> "";
      case ION_TYPE -> "";
      case ION_MODE -> "";
      case ION_SOURCE -> "";
      case MZ -> "";
      case NAME -> "##TITLE";
      case PRINCIPAL_INVESTIGATOR -> "";
      case PUBMED -> "";
      case RT -> "RT";
      case SMILES -> "";
      case MS_LEVEL -> "";
      case PUBCHEM -> "";
      case CHEMSPIDER -> "";
      case MONA_ID -> "";
      case NUM_PEAKS -> "##NPOINTS";
      case RESOLUTION, SYNONYM, MOLWEIGHT -> "";
      case CCS -> "";
    };
  }

  /**
   * Converts the content to the correct value type
   *
   * @param content
   * @return
   * @throws NumberFormatException
   */
  public Object convertValue(String content) throws NumberFormatException {
    if (getObjectClass() == Double.class) {
      return Double.parseDouble(content);
    }
    if (getObjectClass() == Float.class) {
      return Float.parseFloat(content);
    }
    if (getObjectClass() == Integer.class) {
      return Integer.parseInt(content);
    }
    return content;
  }

}
