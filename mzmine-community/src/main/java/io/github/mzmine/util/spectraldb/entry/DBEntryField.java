/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.DatasetIdType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.PeptideSequenceType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SourceScanUsiType;
import io.github.mzmine.datamodel.features.types.annotations.SplashType;
import io.github.mzmine.datamodel.features.types.annotations.UsiType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireParentType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSubclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierPathwayType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.annotations.online_reaction.OnlineLcReactionMatchType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.RelativeHeightType;
import io.github.mzmine.datamodel.features.types.numbers.TotalSamplesType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.io.JsonUtils;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The order reflects the rough order of these fields when exported
 */
public enum DBEntryField {
  // Compound specific
  ENTRY_ID, NAME, SYNONYMS, COMMENT, DESCRIPTION, MOLWEIGHT(Double.class), EXACT_MASS(Double.class),

  // structure
  FORMULA, INCHI, INCHIKEY, SMILES, PEPTIDE_SEQ,

  //Structure classifiers
  CLASSYFIRE_SUPERCLASS, CLASSYFIRE_CLASS, CLASSYFIRE_SUBCLASS, CLASSYFIRE_PARENT, NPCLASSIFIER_SUPERCLASS, NPCLASSIFIER_CLASS, NPCLASSIFIER_PATHWAY,

  // identifier
  CAS, PUBMED, PUBCHEM, GNPS_ID, MONA_ID, CHEMSPIDER,

  /**
   * row ID, used by GNPS, SIRIUS and other tools to connect results
   */
  FEATURE_ID,

  /**
   * feature list name:row ID
   */
  FEATURELIST_NAME_FEATURE_ID,

  // spectrum specific
  MS_LEVEL, RT(Float.class), CCS(Float.class), ION_TYPE, PRECURSOR_MZ(Double.class), CHARGE(
      Integer.class), // height of feature
  FEATURE_MS1_HEIGHT(Float.class), FEATURE_MS1_REL_HEIGHT(Float.class),

  //
  MERGED_SPEC_TYPE, MERGED_N_SAMPLES, SIRIUS_MERGED_SCANS, SIRIUS_MERGED_STATS,

  // MS2
  COLLISION_ENERGY(FloatArrayList.class), FRAGMENTATION_METHOD, ISOLATION_WINDOW, ACQUISITION,

  // MSn
  MSN_COLLISION_ENERGIES, MSN_PRECURSOR_MZS, MSN_FRAGMENTATION_METHODS, MSN_ISOLATION_WINDOWS,

  // Instrument specific
  INSTRUMENT_TYPE, INSTRUMENT, IMS_TYPE, ION_SOURCE, RESOLUTION, POLARITY,

  // other
  PRINCIPAL_INVESTIGATOR, DATA_COLLECTOR, SOFTWARE,

  // Dataset ID is for MassIVE or other repositories
  DATASET_ID, FILENAME, USI, SOURCE_SCAN_USI(List.class),
  /**
   * int or a {@code List<Integer>} `
   */
  SCAN_NUMBER, SPLASH,

  // Quality measures in wrapper object
  QUALITY, // individual properties
  // percentage of precursor purity
  QUALITY_PRECURSOR_PURITY(Float.class), // flag if was chimeric
  QUALITY_CHIMERIC, QUALITY_EXPLAINED_INTENSITY(Float.class), QUALITY_EXPLAINED_SIGNALS(
      Float.class),

  // compound annotation might match to multiple different compounds
  OTHER_MATCHED_COMPOUNDS_N, OTHER_MATCHED_COMPOUNDS_NAMES,

  // online reactivity
  ONLINE_REACTIVITY,

  // number of signals
  NUM_PEAKS(Integer.class), // only used for everything that cannot easily be mapped
  UNSPECIFIED;

  // group of DBEntryFields logically
  public static final DBEntryField[] OTHER_FIELDS = new DBEntryField[]{PRINCIPAL_INVESTIGATOR,
      DATA_COLLECTOR, ENTRY_ID, COMMENT};
  public static final DBEntryField[] DATABASE_FIELDS = new DBEntryField[]{USI, PUBMED, PUBCHEM,
      MONA_ID, CHEMSPIDER, CAS};
  public static final DBEntryField[] COMPOUND_FIELDS = new DBEntryField[]{NAME, SYNONYMS, FORMULA,
      MOLWEIGHT, EXACT_MASS, ION_TYPE, PRECURSOR_MZ, CHARGE, RT, CCS, POLARITY, INCHI, INCHIKEY,
      SMILES, NUM_PEAKS, FEATURE_ID};
  public static final DBEntryField[] INSTRUMENT_FIELDS = new DBEntryField[]{INSTRUMENT_TYPE,
      INSTRUMENT, ION_SOURCE, RESOLUTION, MS_LEVEL, COLLISION_ENERGY, MERGED_SPEC_TYPE, ACQUISITION,
      SOFTWARE};

  private static final Logger logger = Logger.getLogger(DBEntryField.class.getName());

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

  /**
   * @return enum field for a DataType or {@link #UNSPECIFIED} if no clear mapping exists
   */
  public static @NotNull DBEntryField fromDataTypeClass(@NotNull Class<? extends DataType> type) {
    return fromDataType(DataTypes.get(type));
  }

  /**
   * @return enum field for a DataType or {@link #UNSPECIFIED} if no clear mapping exists
   */
  public static @NotNull DBEntryField fromDataType(@NotNull DataType type) {
    return switch (type) {
      case BestScanNumberType _ -> SCAN_NUMBER;
      case PrecursorMZType _ -> PRECURSOR_MZ;
      case MZType _ -> PRECURSOR_MZ;
      case NeutralMassType _ -> EXACT_MASS;
      case IDType _ -> FEATURE_ID;
      case ChargeType _ -> CHARGE;
      case FormulaType _ -> FORMULA;
      case InChIStructureType _ -> INCHI;
      case InChIKeyStructureType _ -> INCHIKEY;
      case SmilesStructureType _ -> SMILES;
      case IonTypeType _ -> ION_TYPE;
      case CompoundNameType _ -> NAME;
      case RTType _ -> RT;
      case CCSType _ -> CCS;
      case UsiType _ -> USI;
      case SourceScanUsiType _ -> SOURCE_SCAN_USI;
      case SplashType _ -> SPLASH;
      case HeightType _ -> FEATURE_MS1_HEIGHT;
      case RelativeHeightType _ -> FEATURE_MS1_REL_HEIGHT;
      case CommentType _ -> DBEntryField.COMMENT;
      case ClassyFireSuperclassType _ -> DBEntryField.CLASSYFIRE_SUPERCLASS;
      case ClassyFireClassType _ -> DBEntryField.CLASSYFIRE_CLASS;
      case ClassyFireSubclassType _ -> DBEntryField.CLASSYFIRE_SUBCLASS;
      case ClassyFireParentType _ -> DBEntryField.CLASSYFIRE_PARENT;
      case NPClassifierSuperclassType _ -> DBEntryField.NPCLASSIFIER_SUPERCLASS;
      case NPClassifierClassType _ -> DBEntryField.NPCLASSIFIER_CLASS;
      case NPClassifierPathwayType _ -> DBEntryField.NPCLASSIFIER_PATHWAY;
//        case SynonymType _ -> DBEntryField.SYNONYM;
      default -> UNSPECIFIED;
    };
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
      case UNSPECIFIED, ACQUISITION, SOFTWARE, CAS, COMMENT, DESCRIPTION, DATA_COLLECTOR,
           INSTRUMENT, //
           INSTRUMENT_TYPE, POLARITY, ION_SOURCE, PRINCIPAL_INVESTIGATOR, PUBMED, PUBCHEM,  //
           CHEMSPIDER, MONA_ID, GNPS_ID, ENTRY_ID, SYNONYMS, RESOLUTION, FRAGMENTATION_METHOD, //
           QUALITY, QUALITY_CHIMERIC, FILENAME, //
           SIRIUS_MERGED_SCANS, SIRIUS_MERGED_STATS, OTHER_MATCHED_COMPOUNDS_N,
           OTHER_MATCHED_COMPOUNDS_NAMES, //
           MERGED_SPEC_TYPE, MSN_COLLISION_ENERGIES, MSN_PRECURSOR_MZS, MSN_FRAGMENTATION_METHODS,
           MSN_ISOLATION_WINDOWS, IMS_TYPE, FEATURELIST_NAME_FEATURE_ID -> StringType.class;
      case MERGED_N_SAMPLES -> TotalSamplesType.class;
      case CLASSYFIRE_SUPERCLASS -> ClassyFireSuperclassType.class;
      case CLASSYFIRE_CLASS -> ClassyFireClassType.class;
      case CLASSYFIRE_SUBCLASS -> ClassyFireSubclassType.class;
      case CLASSYFIRE_PARENT -> ClassyFireParentType.class;
      case NPCLASSIFIER_SUPERCLASS -> NPClassifierSuperclassType.class;
      case NPCLASSIFIER_CLASS -> NPClassifierClassType.class;
      case NPCLASSIFIER_PATHWAY -> NPClassifierPathwayType.class;
      case SCAN_NUMBER -> BestScanNumberType.class;
      case MS_LEVEL, NUM_PEAKS, FEATURE_ID -> IntegerType.class;
      case EXACT_MASS, PRECURSOR_MZ, MOLWEIGHT -> MZType.class;
      case CHARGE -> ChargeType.class;
      // TODO change to float
      case COLLISION_ENERGY, ISOLATION_WINDOW, QUALITY_EXPLAINED_INTENSITY,
           QUALITY_EXPLAINED_SIGNALS -> DoubleType.class;
      case QUALITY_PRECURSOR_PURITY -> FloatType.class;
      case FORMULA -> FormulaType.class;
      case INCHI -> InChIStructureType.class;
      case INCHIKEY -> InChIKeyStructureType.class;
      case ION_TYPE -> IonTypeType.class;
      case NAME -> CompoundNameType.class;
      case RT -> RTType.class;
      case SMILES -> SmilesStructureType.class;
      case PEPTIDE_SEQ -> PeptideSequenceType.class;
      case CCS -> CCSType.class;
      case DATASET_ID -> DatasetIdType.class;
      case USI -> UsiType.class;
      case SOURCE_SCAN_USI -> SourceScanUsiType.class;
      case SPLASH -> SplashType.class;
      case ONLINE_REACTIVITY -> OnlineLcReactionMatchType.class;
      // TODO change to real data types instead of strings
      // are there other formats that define those properly?
      case FEATURE_MS1_HEIGHT -> HeightType.class;
      case FEATURE_MS1_REL_HEIGHT -> RelativeHeightType.class;
    };
  }

  /**
   * @return The mzmine json format key or an empty String
   */
  public String getMZmineJsonID() {
    return switch (this) {
      case CLASSYFIRE_SUPERCLASS, CLASSYFIRE_CLASS, CLASSYFIRE_SUBCLASS, CLASSYFIRE_PARENT,
           NPCLASSIFIER_SUPERCLASS, NPCLASSIFIER_CLASS, NPCLASSIFIER_PATHWAY ->
          name().toLowerCase();
      case SCAN_NUMBER -> "scan_number";
      case FEATURE_MS1_HEIGHT -> "feature_ms1_height";
      case FEATURE_MS1_REL_HEIGHT -> "feature_ms1_relative_height";
      case MERGED_SPEC_TYPE -> "merge_type";
      case MERGED_N_SAMPLES -> "merged_across_n_samples";
      case ACQUISITION -> "acquisition";
      case SOFTWARE -> "softwaresource";
      case PEPTIDE_SEQ -> "peptide_sequence";
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
      case IMS_TYPE -> "ims_type";
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
      case SOURCE_SCAN_USI -> "source_scan_usi";
      case QUALITY -> "quality";
      case QUALITY_PRECURSOR_PURITY -> "precursor_purity";
      case QUALITY_CHIMERIC -> "quality_chimeric";
      case QUALITY_EXPLAINED_INTENSITY -> "quality_explained_intensity";
      case QUALITY_EXPLAINED_SIGNALS -> "quality_explained_signals";
      case OTHER_MATCHED_COMPOUNDS_N -> "other_matched_compounds";
      case OTHER_MATCHED_COMPOUNDS_NAMES -> "other_matched_compounds_names";
      case FEATURE_ID -> "feature_id";
      case FEATURELIST_NAME_FEATURE_ID -> "featurelist_feature_id";
      case FILENAME -> "raw_file_name";
      case SIRIUS_MERGED_SCANS -> "merged_scans";
      case SIRIUS_MERGED_STATS -> "merged_statistics";
      case ONLINE_REACTIVITY -> "online_reactivity";
      case UNSPECIFIED -> "";
    };
  }

  /**
   * @return The NIST MSP format key or an empty String
   */
  public String getNistMspID() {
    return switch (this) {
      case CLASSYFIRE_SUPERCLASS, CLASSYFIRE_CLASS, CLASSYFIRE_SUBCLASS, CLASSYFIRE_PARENT,
           NPCLASSIFIER_SUPERCLASS, NPCLASSIFIER_CLASS, NPCLASSIFIER_PATHWAY, ACQUISITION, GNPS_ID,
           MONA_ID, CHEMSPIDER, RESOLUTION, SYNONYMS, MOLWEIGHT, PUBCHEM, PUBMED,
           PRINCIPAL_INVESTIGATOR, CHARGE, CAS, SOFTWARE, DATA_COLLECTOR, SOURCE_SCAN_USI ->
          this.name().toLowerCase();
      case SCAN_NUMBER -> "scan_number";
      case MERGED_SPEC_TYPE -> "merge_type";
      case MERGED_N_SAMPLES -> "merged_across_n_samples";
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
      case IMS_TYPE -> "ims_type";
      case PRECURSOR_MZ -> "PrecursorMZ";
      case NAME -> "Name";
      case SPLASH -> "Splash";
      case RT -> "RT";
      case MS_LEVEL -> "Spectrum_type";
      case NUM_PEAKS -> "Num Peaks";
      case CCS -> "CCS";
      case SMILES -> "SMILES";
      case INCHI -> "INCHI";
      case PEPTIDE_SEQ -> "peptide_sequence";
      case MSN_COLLISION_ENERGIES -> "MSn_collision_energies";
      case MSN_PRECURSOR_MZS -> "MSn_precursor_mzs";
      case MSN_FRAGMENTATION_METHODS -> "MSn_fragmentation_methods";
      case MSN_ISOLATION_WINDOWS -> "MSn_isolation_windows";
      case USI -> "usi";
      case DESCRIPTION -> "description";
      case QUALITY -> "quality";
      case DATASET_ID -> "dataset_id";
      case QUALITY_CHIMERIC -> "quality_chimeric";
      case QUALITY_PRECURSOR_PURITY -> "precursor_purity";
      case QUALITY_EXPLAINED_INTENSITY -> "quality_explained_intensity";
      case QUALITY_EXPLAINED_SIGNALS -> "quality_explained_signals";
      case OTHER_MATCHED_COMPOUNDS_N -> "other_matched_compounds";
      case OTHER_MATCHED_COMPOUNDS_NAMES -> "other_matched_compounds_names";
      case FEATURE_ID -> "feature_id";
      case FEATURELIST_NAME_FEATURE_ID -> "featurelist_feature_id";
      case FILENAME -> "file_name";
      case ONLINE_REACTIVITY -> "online_reactivity";
      case FEATURE_MS1_HEIGHT -> "feature_ms1_height";
      case FEATURE_MS1_REL_HEIGHT -> "feature_ms1_relative_height";
      case SIRIUS_MERGED_SCANS -> "";
      case SIRIUS_MERGED_STATS -> "";
      case UNSPECIFIED -> "";
    };
  }

  /**
   * @return The mgf format (used by GNPS)
   */
  public String getMgfID() {
    return switch (this) {
      case ACQUISITION, FEATURE_MS1_HEIGHT, FEATURE_MS1_REL_HEIGHT, GNPS_ID, MONA_ID, CHEMSPIDER,
           PUBCHEM, RESOLUTION, SYNONYMS, MOLWEIGHT, CAS, SOFTWARE, COLLISION_ENERGY,
           CLASSYFIRE_SUPERCLASS, CLASSYFIRE_CLASS, CLASSYFIRE_SUBCLASS, CLASSYFIRE_PARENT,
           NPCLASSIFIER_SUPERCLASS, NPCLASSIFIER_CLASS, NPCLASSIFIER_PATHWAY, SOURCE_SCAN_USI ->
          name();
      case RT -> "RTINSECONDS";
      case SCAN_NUMBER -> "SCANS";
      case MERGED_SPEC_TYPE -> "SPECTYPE";
      case MERGED_N_SAMPLES -> "MERGED_ACROSS_N_SAMPLES";
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
      case ION_TYPE -> "ADDUCT";
      case POLARITY -> "IONMODE"; // Positive Negative
      case ION_SOURCE -> "ION_SOURCE";
      case IMS_TYPE -> "IMS_TYPE";
      case PRECURSOR_MZ -> "PEPMASS";
      case NAME -> "NAME";
      case PRINCIPAL_INVESTIGATOR -> "PI";
      case PUBMED -> "PUBMED";
      case SMILES -> "SMILES";
      case MS_LEVEL -> "MSLEVEL";
      case CCS -> "CCS";
      case SPLASH -> "SPLASH";
      case NUM_PEAKS -> "Num peaks";
      case MSN_COLLISION_ENERGIES -> "MSn_collision_energies";
      case MSN_PRECURSOR_MZS -> "MSn_precursor_mzs";
      case MSN_FRAGMENTATION_METHODS -> "MSn_fragmentation_methods";
      case MSN_ISOLATION_WINDOWS -> "MSn_isolation_windows";
      case FRAGMENTATION_METHOD -> "FRAGMENTATION_METHOD";
      case ISOLATION_WINDOW -> "ISOLATION_WINDOW";
      case USI -> "USI";
      case PEPTIDE_SEQ -> "PEPTIDE_SEQUENCE";
      case QUALITY_CHIMERIC -> "QUALITY_CHIMERIC";
      case QUALITY_PRECURSOR_PURITY -> "PRECURSOR_PURITY";
      case DATASET_ID -> "DATASET_ID";
      case QUALITY -> "QUALITY";
      case QUALITY_EXPLAINED_INTENSITY -> "QUALITY_EXPLAINED_INTENSITY";
      case QUALITY_EXPLAINED_SIGNALS -> "QUALITY_EXPLAINED_SIGNALS";
      case OTHER_MATCHED_COMPOUNDS_N -> "OTHER_MATCHED_COMPOUNDS";
      case OTHER_MATCHED_COMPOUNDS_NAMES -> "OTHER_MATCHED_COMPOUNDS_NAMES";
      case FEATURE_ID -> "FEATURE_ID";
      case FEATURELIST_NAME_FEATURE_ID -> "FEATURELIST_FEATURE_ID";
      case FILENAME -> "FILENAME";
      case SIRIUS_MERGED_SCANS -> "MERGED_SCANS";
      case SIRIUS_MERGED_STATS -> "MERGED_STATS";
      case ONLINE_REACTIVITY -> "ONLINE_REACTIVITY";
      case UNSPECIFIED -> "";
    };
  }

  /**
   * @return The mgf format (used by GNPS)
   */
  public String getGnpsBatchSubmissionID() {
    return switch (this) {
      case GNPS_ID, MONA_ID, CHEMSPIDER, PUBCHEM, RESOLUTION, SYNONYMS, MOLWEIGHT, SOFTWARE,
           COLLISION_ENERGY, FEATURE_MS1_HEIGHT, FEATURE_MS1_REL_HEIGHT, CLASSYFIRE_SUPERCLASS,
           CLASSYFIRE_CLASS, CLASSYFIRE_SUBCLASS, CLASSYFIRE_PARENT, NPCLASSIFIER_SUPERCLASS,
           NPCLASSIFIER_CLASS, NPCLASSIFIER_PATHWAY, SOURCE_SCAN_USI -> this.name();
      case FILENAME -> "FILENAME";
      case PEPTIDE_SEQ -> "SEQ";
      case NAME -> "COMPOUND_NAME";
      case PRECURSOR_MZ -> "MOLECULEMASS";
      case INSTRUMENT_TYPE -> "INSTRUMENT";
      case ION_SOURCE -> "IONSOURCE";
      case IMS_TYPE -> "IMS_TYPE";
      case SCAN_NUMBER -> "EXTRACTSCAN";
      case SMILES -> "SMILES";
      case INCHI -> "INCHI";
      case INCHIKEY -> "INCHIAUX";
      case CHARGE -> "CHARGE";
      case POLARITY -> "IONMODE";
      case PUBMED -> "PUBMED";
      case ACQUISITION -> "ACQUISITION";
      case EXACT_MASS -> "EXACTMASS";
      case DATA_COLLECTOR -> "DATACOLLECTOR";
      case ION_TYPE -> "ADDUCT";
      case CAS -> "CASNUMBER";
      case PRINCIPAL_INVESTIGATOR -> "PI";
      //not covered
      case INSTRUMENT -> "INSTRUMENT_NAME";
      case RT -> "RTINSECONDS";
      case ENTRY_ID -> "SPECTRUMID";
      case COMMENT -> "COMMENT";
      case DESCRIPTION -> "DESCRIPTION";
      case FORMULA -> "FORMULA";
      case MS_LEVEL -> "MSLEVEL";
      case CCS -> "CCS";
      case SPLASH -> "SPLASH";
      case MERGED_SPEC_TYPE -> "SPECTYPE";
      case MERGED_N_SAMPLES -> "merged_across_n_samples";
      case NUM_PEAKS -> "Num peaks";
      case MSN_COLLISION_ENERGIES -> "MSn_collision_energies";
      case MSN_PRECURSOR_MZS -> "MSn_precursor_mzs";
      case MSN_FRAGMENTATION_METHODS -> "MSn_fragmentation_methods";
      case MSN_ISOLATION_WINDOWS -> "MSn_isolation_windows";
      case FRAGMENTATION_METHOD -> "FRAGMENTATION_METHOD";
      case ISOLATION_WINDOW -> "ISOLATION_WINDOW";
      case USI -> "USI";
      case QUALITY_CHIMERIC -> "QUALITY_CHIMERIC";
      case QUALITY_PRECURSOR_PURITY -> "PRECURSOR_PURITY";
      case DATASET_ID -> "DATASET_ID";
      case QUALITY -> "QUALITY";
      case QUALITY_EXPLAINED_INTENSITY -> "QUALITY_EXPLAINED_INTENSITY";
      case QUALITY_EXPLAINED_SIGNALS -> "QUALITY_EXPLAINED_SIGNALS";
      case OTHER_MATCHED_COMPOUNDS_N -> "OTHER_MATCHED_COMPOUNDS";
      case OTHER_MATCHED_COMPOUNDS_NAMES -> "OTHER_MATCHED_COMPOUNDS_NAMES";
      case FEATURE_ID -> "FEATURE_ID";
      case FEATURELIST_NAME_FEATURE_ID -> "FEATURELIST_FEATURE_ID";
      case SIRIUS_MERGED_SCANS -> "MERGED_SCANS";
      case SIRIUS_MERGED_STATS -> "MERGED_STATS";
      case ONLINE_REACTIVITY -> "ONLINE_REACTIVITY";
      case UNSPECIFIED -> "";
    };
  }

  /**
   * @return The JCAMP-DX format key or an empty String
   */
  public String getJdxID() {
    return switch (this) {
      case SIRIUS_MERGED_STATS, ONLINE_REACTIVITY, FEATURE_MS1_HEIGHT, FEATURE_MS1_REL_HEIGHT,
           CLASSYFIRE_SUPERCLASS, CLASSYFIRE_CLASS, CLASSYFIRE_SUBCLASS, CLASSYFIRE_PARENT,
           NPCLASSIFIER_SUPERCLASS, NPCLASSIFIER_CLASS, NPCLASSIFIER_PATHWAY, MERGED_N_SAMPLES,
           SOURCE_SCAN_USI -> "";
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
      case IMS_TYPE -> "";
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
      case PEPTIDE_SEQ -> "";
      case NAME -> "##TITLE";
      case PRINCIPAL_INVESTIGATOR -> "";
      case PUBMED -> "";
      case RT -> "RT";
      case SMILES -> "";
      case MS_LEVEL -> "";
      case PUBCHEM -> "";
      case CHEMSPIDER -> "";
      case MONA_ID, GNPS_ID -> "";
      case OTHER_MATCHED_COMPOUNDS_N -> "";
      case OTHER_MATCHED_COMPOUNDS_NAMES -> "";
      case NUM_PEAKS -> "##NPOINTS";
      case RESOLUTION, SYNONYMS, MOLWEIGHT -> "";
      case CCS -> "";
      case MSN_COLLISION_ENERGIES -> "";
      case MSN_PRECURSOR_MZS -> "";
      case MSN_FRAGMENTATION_METHODS -> "";
      case MSN_ISOLATION_WINDOWS -> "";
      case QUALITY_PRECURSOR_PURITY -> "";
      case FRAGMENTATION_METHOD -> "";
      case ISOLATION_WINDOW -> "";
      case FILENAME -> "";
      case USI -> "";
      case QUALITY -> "";
      case DATASET_ID -> "";
      case QUALITY_CHIMERIC -> "";
      case QUALITY_EXPLAINED_INTENSITY -> "";
      case QUALITY_EXPLAINED_SIGNALS -> "";
      case FEATURE_ID, FEATURELIST_NAME_FEATURE_ID -> "";
      case SIRIUS_MERGED_SCANS -> "";
      case UNSPECIFIED -> "";
    };
  }

  /**
   * Converts the content to the correct value type - on exception this will log the warning and
   * return null.
   *
   * @param content the value to be converted
   * @return the converted value or original if the target object class is string or null if there
   * is an exception during conversion
   */
  @Nullable
  public Object tryConvertValue(String content) {
    try {
      return convertValue(content);
    } catch (Throwable t) {
      logger.log(Level.WARNING, """
          Cannot convert value '%s' to type %s for field %s""".formatted(content,
          this.getObjectClass(), this.toString()));
      return null;
    }
  }

  /**
   * Converts the content to the correct value type
   *
   * @param content the value to be converted
   * @return the original value or Double, Float, Integer
   * @throws NumberFormatException if the object class was specified as number but was not parsable
   */
  public Object convertValue(String content) throws NumberFormatException {
    if (getObjectClass().equals(Double.class)) {
      return Double.parseDouble(content);
    }
    if (getObjectClass().equals(Float.class)) {
      return Float.parseFloat(content);
    }
    if (getObjectClass().equals(Integer.class)) {
      return Integer.parseInt(content);
    }
    if (getObjectClass().equals(Long.class)) {
      return Long.parseLong(content);
    }
    if (getObjectClass().equals(FloatArrayList.class)) {
      final String replaced = content.replaceAll("[\\[\\]]", "");
      final float[] floats = ParsingUtils.stringToFloatArray(replaced, ",");
      return new FloatArrayList(floats);
    }
    // TODO currently we can only parse this as list of strings - should be either json list or java object list
    // FloatArrayList IntArrayList and other specialized classes help to load numbers
    else if (getObjectClass().equals(List.class) && content != null) {
      List<String> list = JsonUtils.readValueOrThrow(content);
      return list;
    }
    return content;
  }

  public String formatForMgf(@NotNull final Object value) {
    return switch (this) {
      case UNSPECIFIED, QUALITY, QUALITY_EXPLAINED_INTENSITY, QUALITY_EXPLAINED_SIGNALS, GNPS_ID, //
           PUBCHEM, MONA_ID, CHEMSPIDER, FEATURE_ID, PUBMED, SYNONYMS, NAME, ENTRY_ID, NUM_PEAKS, //
           MS_LEVEL, INSTRUMENT, ION_SOURCE, RESOLUTION, PRINCIPAL_INVESTIGATOR, DATA_COLLECTOR, //
           COMMENT, DESCRIPTION, MOLWEIGHT, FORMULA, INCHI, INCHIKEY, SMILES, CAS, CCS, //
           ION_TYPE, CHARGE, MERGED_SPEC_TYPE, SIRIUS_MERGED_SCANS, SIRIUS_MERGED_STATS,
           COLLISION_ENERGY, FRAGMENTATION_METHOD, ISOLATION_WINDOW, ACQUISITION,
           MSN_COLLISION_ENERGIES, MSN_PRECURSOR_MZS, //
           MSN_FRAGMENTATION_METHODS, MSN_ISOLATION_WINDOWS, INSTRUMENT_TYPE, SOFTWARE, FILENAME, //
           DATASET_ID, USI, SOURCE_SCAN_USI, SPLASH, QUALITY_CHIMERIC, //
           OTHER_MATCHED_COMPOUNDS_N, OTHER_MATCHED_COMPOUNDS_NAMES, QUALITY_PRECURSOR_PURITY,
           PEPTIDE_SEQ, //
           IMS_TYPE, ONLINE_REACTIVITY, CLASSYFIRE_SUPERCLASS, CLASSYFIRE_CLASS,
           CLASSYFIRE_SUBCLASS, CLASSYFIRE_PARENT, NPCLASSIFIER_SUPERCLASS, NPCLASSIFIER_CLASS,
           NPCLASSIFIER_PATHWAY, FEATURELIST_NAME_FEATURE_ID, MERGED_N_SAMPLES -> {

        // format lists and arrays as json so that they can easily be parsed
        if (value instanceof Collection<?> || value.getClass().isArray()) {
          yield JsonUtils.writeStringOrEmpty(value);
        }

        yield value.toString();
      }
      case SCAN_NUMBER -> switch (value) {
        // multiple scans can be written as 1,4,6-9
        case List<?> list -> {
          List<Integer> values = list.stream().map(MathUtils::parseInt).filter(Objects::nonNull)
              .toList();
          yield IndexRange.findRanges(values).stream().map(Objects::toString)
              .collect(Collectors.joining(","));
        }
        default -> value.toString();
      };
      case RT -> switch (value) {
        // float is default for RT but handle Double in case wrong value was present
        case Float f -> "%.2f".formatted(f * 60.f);
        case Double d -> "%.2f".formatted(d * 60.0);
        default -> throw new IllegalArgumentException("RT has to be a number");
      };
      case PRECURSOR_MZ, EXACT_MASS -> switch (value) {
        case Number d -> MZmineCore.getConfiguration().getExportFormats().mz(d);
        default -> throw new IllegalArgumentException("MZ has to be a number");
      };
      case FEATURE_MS1_HEIGHT -> switch (value) {
        case Number d -> MZmineCore.getConfiguration().getExportFormats().intensity(d);
        default -> throw new IllegalArgumentException("Height has to be a number");
      };
      case FEATURE_MS1_REL_HEIGHT -> switch (value) {
        case Number d -> MZmineCore.getConfiguration().getExportFormats().percent(d);
        default -> throw new IllegalArgumentException("Relative height has to be a number");
      };
      // SIRIUS 6.0.7 had issues with Polarity and would parse the spectrum without extended metadata like the adduct
      // Therefore it was changed from Positive to POSITIVE
      case POLARITY -> PolarityType.NEGATIVE.equals(value) ? "NEGATIVE" : "POSITIVE";
    };
  }
}
