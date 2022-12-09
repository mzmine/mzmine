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

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.DatasetIdType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SplashType;
import io.github.mzmine.datamodel.features.types.annotations.UsiType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import org.apache.commons.lang3.StringUtils;

public enum DBEntryField {
  // Compound specific
  ENTRY_ID, NAME, SYNONYMS, COMMENT, DESCRIPTION, MOLWEIGHT(Double.class), EXACT_MASS(
      Double.class), FORMULA, INCHI, INCHIKEY, SMILES, CAS, PUBMED, PUBCHEM, GNPS_ID, MONA_ID, CHEMSPIDER,

  // spectrum specific
  MS_LEVEL, RT(Float.class), CCS(Float.class), ION_TYPE, PRECURSOR_MZ(Double.class), CHARGE(
      Integer.class), MERGED_SPEC_TYPE,

  // MS2
  COLLISION_ENERGY, FRAGMENTATION_METHOD, ISOLATION_WINDOW, ACQUISITION,

  // MSn
  MSN_COLLISION_ENERGIES, MSN_PRECURSOR_MZS, MSN_FRAGMENTATION_METHODS, MSN_ISOLATION_WINDOWS,

  // Instrument specific
  INSTRUMENT_TYPE, INSTRUMENT, ION_SOURCE, RESOLUTION, POLARITY,

  // other
  PRINCIPAL_INVESTIGATOR, DATA_COLLECTOR, SOFTWARE,

  // Dataset ID is for MassIVE or other repositories
  DATASET_ID, USI, SCAN_NUMBER(Integer.class), DATAFILE_COLON_SCAN_NUMBER, SPLASH,

  // Quality measures
  QUALITY_CHIMERIC,

  // number of signals
  NUM_PEAKS(Integer.class);

  // group of DBEntryFields logically
  public static final DBEntryField[] OTHER_FIELDS = new DBEntryField[]{PRINCIPAL_INVESTIGATOR,
      DATA_COLLECTOR, ENTRY_ID, COMMENT};
  public static final DBEntryField[] DATABASE_FIELDS = new DBEntryField[]{PUBMED, PUBCHEM, MONA_ID,
      CHEMSPIDER, CAS};
  public static final DBEntryField[] COMPOUND_FIELDS = new DBEntryField[]{NAME, SYNONYMS, FORMULA,
      MOLWEIGHT, EXACT_MASS, ION_TYPE, PRECURSOR_MZ, CHARGE, RT, CCS, POLARITY, INCHI, INCHIKEY,
      SMILES, NUM_PEAKS};
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
   */
  public static DBEntryField forMZmineJsonID(String key) {
    for (DBEntryField f : values()) {
      // equalsIgnoreCase is more robust against changes in library
      // consistency
      if (f.getMZmineJsonID().equalsIgnoreCase(key)) {
        return f;
      }
    }
    return null;
  }

  /**
   * DBENtryField for NIST msp key
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
      case PRECURSOR_MZ -> "Precursor m/z";
      default -> StringUtils.capitalize(super.toString().replace('_', ' ').toLowerCase());
    };
  }

  /**
   * @return The mzmine json format key or an empty String
   */
  public Class<? extends DataType> getDataType() {
    return switch (this) {
      case ACQUISITION, SOFTWARE, CAS, COMMENT, DESCRIPTION, DATA_COLLECTOR, INSTRUMENT, INSTRUMENT_TYPE, POLARITY, ION_SOURCE, PRINCIPAL_INVESTIGATOR, PUBMED, PUBCHEM, CHEMSPIDER, MONA_ID, GNPS_ID, ENTRY_ID, SYNONYMS, RESOLUTION, FRAGMENTATION_METHOD, DATAFILE_COLON_SCAN_NUMBER, QUALITY_CHIMERIC ->
          StringType.class;
      case SCAN_NUMBER -> BestScanNumberType.class;
      case MS_LEVEL, NUM_PEAKS -> IntegerType.class;
      case EXACT_MASS, PRECURSOR_MZ, MOLWEIGHT -> MZType.class;
      case CHARGE -> ChargeType.class;
      case COLLISION_ENERGY -> DoubleType.class;
      case FORMULA -> FormulaType.class;
      case INCHI -> InChIStructureType.class;
      case INCHIKEY -> InChIKeyStructureType.class;
      case ION_TYPE -> IonTypeType.class;
      case NAME -> CompoundNameType.class;
      case RT -> RTType.class;
      case SMILES -> SmilesStructureType.class;
      case CCS -> CCSType.class;
      case ISOLATION_WINDOW -> DoubleType.class;
      case DATASET_ID -> DatasetIdType.class;
      case USI -> UsiType.class;
      case SPLASH -> SplashType.class;
      // TODO change to real data types instead of strings
      // are there other formats that define those properly?
      case MERGED_SPEC_TYPE, MSN_COLLISION_ENERGIES, MSN_PRECURSOR_MZS, MSN_FRAGMENTATION_METHODS, MSN_ISOLATION_WINDOWS ->
          StringType.class;
    };
  }

  /**
   * @return The mzmine json format key or an empty String
   */
  public String getMZmineJsonID() {
    return switch (this) {
      case SCAN_NUMBER -> "scan_number";
      case MERGED_SPEC_TYPE -> "merge_type";
      case ACQUISITION -> "acquisition";
      case SOFTWARE -> "softwaresource";
      case CAS -> "cas";
      case CHARGE -> "charge";
      case COLLISION_ENERGY -> "collision_energy";
      case COMMENT -> "comment";
      case DESCRIPTION -> "description";
      case DATA_COLLECTOR -> "datacollector";
      case EXACT_MASS -> "exact_mass";
      case FORMULA -> "formula";
      case SPLASH -> "splash";
      case INCHI -> "inchi";
      case INCHIKEY -> "inchikey";
      case INSTRUMENT -> "instrument";
      case INSTRUMENT_TYPE -> "instrument_type";
      case ION_TYPE -> "adduct";
      case POLARITY -> "polarity";
      case ION_SOURCE -> "ion_source";
      case PRECURSOR_MZ -> "precursor_mz";
      case NAME -> "compound_name";
      case PRINCIPAL_INVESTIGATOR -> "investigator";
      case PUBMED -> "pubmed";
      case RT -> "rt";
      case SMILES -> "smiles";
      case MS_LEVEL -> "ms_level";
      case PUBCHEM -> "pubchem";
      case CHEMSPIDER -> "chemspider";
      case MONA_ID -> "mona_id";
      case GNPS_ID -> "gnps_id";
      case CCS -> "ccs";
      case NUM_PEAKS -> "num_peaks";
      case ENTRY_ID -> "lib_id";
      case RESOLUTION -> "mass_resolution";
      case SYNONYMS -> "synonyms";
      case MOLWEIGHT -> "molweight";
      case MSN_COLLISION_ENERGIES -> "msn_collision_energies";
      case MSN_PRECURSOR_MZS -> "msn_precursor_mzs";
      case MSN_FRAGMENTATION_METHODS -> "msn_fragmentation_methods";
      case MSN_ISOLATION_WINDOWS -> "msn_isolation_windows";
      case FRAGMENTATION_METHOD -> "fragmenation_method";
      case ISOLATION_WINDOW -> "isolation_window";
      case DATASET_ID -> "dataset_id";
      case USI -> "usi";
      case DATAFILE_COLON_SCAN_NUMBER -> "datafile_scannumber";
      case QUALITY_CHIMERIC -> "quality_chimeric";
    };
  }

  /**
   * @return The NIST MSP format key or an empty String
   */
  public String getNistMspID() {
    return switch (this) {
      case SCAN_NUMBER -> "scan_number";
      case MERGED_SPEC_TYPE -> "merge_type";
      case ENTRY_ID -> "DB#";
      case COLLISION_ENERGY -> "Collision_energy";
      case COMMENT -> "Comments";
      case EXACT_MASS -> "ExactMass";
      case FRAGMENTATION_METHOD -> "Method";
      case ISOLATION_WINDOW -> "Isolation_window";
      case FORMULA -> "Formula";
      case INCHIKEY -> "InChIKey";
      case INSTRUMENT -> "Instrument";
      case INSTRUMENT_TYPE -> "Instrument_type";
      case ION_TYPE -> "Precursor_type";
      case POLARITY -> "Ion_mode"; // P / N
      case ION_SOURCE -> "";
      case PRECURSOR_MZ -> "PrecursorMZ";
      case NAME -> "Name";
      case SPLASH -> "Splash";
      case RT -> "RT";
      case MS_LEVEL -> "Spectrum_type";
      case NUM_PEAKS -> "Num Peaks";
      case CCS -> "CCS";
      case SMILES -> "SMILES";
      case INCHI -> "INCHI";
      case ACQUISITION, GNPS_ID, MONA_ID, CHEMSPIDER, RESOLUTION, SYNONYMS, MOLWEIGHT, PUBCHEM, PUBMED, PRINCIPAL_INVESTIGATOR, CHARGE, CAS, SOFTWARE, DATA_COLLECTOR ->
          toString();
      case MSN_COLLISION_ENERGIES -> "MSn_collision_energies";
      case MSN_PRECURSOR_MZS -> "MSn_precursor_mzs";
      case MSN_FRAGMENTATION_METHODS -> "MSn_fragmentation_methods";
      case MSN_ISOLATION_WINDOWS -> "MSn_isolation_windows";
      case USI -> "usi";
      case DATAFILE_COLON_SCAN_NUMBER -> "datafile_scannumber";
      case DESCRIPTION -> "description";
      case QUALITY_CHIMERIC -> "quality_chimeric";
      case DATASET_ID -> "dataset_id";
    };
  }

  /**
   * @return The mgf format (used by GNPS)
   */
  public String getMgfID() {
    return switch (this) {
      case SCAN_NUMBER -> "SCANS";
      case MERGED_SPEC_TYPE -> "MERGE_TYPE";
      case ENTRY_ID -> "SPECTRUMID";
      case CHARGE -> "CHARGE";
      case COMMENT -> "COMMENT";
      case DESCRIPTION -> "DESCRIPTION";
      case DATA_COLLECTOR -> "DATACOLLECTOR";
      case EXACT_MASS -> "EXACTMASS";
      case FORMULA -> "FORMULA";
      case INCHI -> "INCHI";
      case INCHIKEY -> "INCHIAUX";
      case INSTRUMENT -> "SOURCE_INSTRUMENT";
      case INSTRUMENT_TYPE -> "INSTRUMENT_TYPE";
      case ION_TYPE -> "PRECURSOR_TYPE";
      case POLARITY -> "IONMODE"; // Positive Negative
      case ION_SOURCE -> "ION_SOURCE";
      case PRECURSOR_MZ -> "PEPMASS";
      case NAME -> "NAME";
      case PRINCIPAL_INVESTIGATOR -> "PI";
      case PUBMED -> "PUBMED";
      case SMILES -> "SMILES";
      case MS_LEVEL -> "MSLEVEL";
      case CCS -> "CCS";
      case SPLASH -> "SPLASH";
      case ACQUISITION, NUM_PEAKS, GNPS_ID, MONA_ID, CHEMSPIDER, PUBCHEM, RT, RESOLUTION, SYNONYMS, MOLWEIGHT, CAS, SOFTWARE, COLLISION_ENERGY ->
          toString();
      case MSN_COLLISION_ENERGIES -> "MSn_collision_energies";
      case MSN_PRECURSOR_MZS -> "MSn_precursor_mzs";
      case MSN_FRAGMENTATION_METHODS -> "MSn_fragmentation_methods";
      case MSN_ISOLATION_WINDOWS -> "MSn_isolation_windows";
      case FRAGMENTATION_METHOD -> "FRAGMENTATION_METHOD";
      case ISOLATION_WINDOW -> "ISOLATION_WINDOW";
      case USI -> "USI";
      case DATAFILE_COLON_SCAN_NUMBER -> "DATAFILE_SCANNUMBER";
      case QUALITY_CHIMERIC -> "QUALITY_CHIMERIC";
      case DATASET_ID -> "DATASET_ID";
    };
  }

  /**
   * @return The JCAMP-DX format key or an empty String
   */
  public String getJdxID() {
    return switch (this) {
      case SCAN_NUMBER -> "";
      case MERGED_SPEC_TYPE -> "";
      case ENTRY_ID -> "";
      case ACQUISITION -> "";
      case SOFTWARE -> "";
      case CAS -> "##CAS REGISTRY NO";
      case CHARGE -> "";
      case COLLISION_ENERGY -> "";
      case COMMENT -> "";
      case DESCRIPTION -> "";
      case DATA_COLLECTOR -> "";
      case EXACT_MASS -> "##MW";
      case FORMULA -> "##MOLFORM";
      case INCHI -> "";
      case INCHIKEY -> "";
      case INSTRUMENT -> "";
      case INSTRUMENT_TYPE -> "";
      case ION_TYPE, SPLASH -> "";
      case POLARITY -> "";
      case ION_SOURCE -> "";
      case PRECURSOR_MZ -> "";
      case NAME -> "##TITLE";
      case PRINCIPAL_INVESTIGATOR -> "";
      case PUBMED -> "";
      case RT -> "RT";
      case SMILES -> "";
      case MS_LEVEL -> "";
      case PUBCHEM -> "";
      case CHEMSPIDER -> "";
      case MONA_ID, GNPS_ID -> "";
      case NUM_PEAKS -> "##NPOINTS";
      case RESOLUTION, SYNONYMS, MOLWEIGHT -> "";
      case CCS -> "";
      case MSN_COLLISION_ENERGIES -> "";
      case MSN_PRECURSOR_MZS -> "";
      case MSN_FRAGMENTATION_METHODS -> "";
      case MSN_ISOLATION_WINDOWS -> "";
      case FRAGMENTATION_METHOD -> "";
      case ISOLATION_WINDOW -> "";
      case USI -> "";
      case DATAFILE_COLON_SCAN_NUMBER -> "";
      case QUALITY_CHIMERIC -> "";
      case DATASET_ID -> "";
    };
  }

  /**
   * Converts the content to the correct value type
   *
   * @param content the value to be converted
   * @return the original value or Double, Float, Integer
   * @throws NumberFormatException if the object class was specified as number but was not parsable
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
