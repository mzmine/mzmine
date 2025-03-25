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

package io.github.mzmine.util.spectraldb.parser.mzmine;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.processing.Generated;
import org.jetbrains.annotations.Nullable;

@JsonNaming(SnakeCaseStrategy.class)
@JsonPropertyOrder({"softwaresource", "mergedSpectrumType", "entry_id", "ms_level", "polarity",
    "compound_name", "synonyms", "adduct", "charge", "precursor_mz", "exact_mass", "rt", "ccs",

    // structure/compound specific
    "formula", "smiles", "inchi", "inchikey",

    // external ids
    "cas", "splash",

    // fragmentation
    "fragmentation_energy", "fragmentation_method", "isolation_window", "multi_stage_fragmentation",

    // instrument
    "instrument_type", "instrument", "ion_source", "resolution",

    // dataset
    "scan_number", "dataset_id", "usi",

    // extra
    "comment", "compound_source", "investigator", "data_collector",

    // data
    "num_signals", "signals"})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Generated("jsonschema2pojo")
@Deprecated
public class MZmineJsonLibraryEntry {

  public String softwaresource;
  public Integer msLevel;
  public String entryId;
  public String compoundName;
  public List<String> synonyms = new ArrayList<>();
  public String adduct;
  public Integer charge;
  public Double precursorMz, isolationWindow;
  public Double exactMass;
  public Double rt, ccs;
  public String cas, splash;
  public String formula, smiles, inchi, inchikey, peptideSequence;
  public FloatArrayList fragmentationEnergy;
  public String mergedSpectrumType;
  public String fragmentationMethod;
  public String instrumentType, instrument, resolution, ionSource;
  public String polarity;
  public String datasetId, usi;
  public Integer scanNumber;
  public String comment;
  public String compoundSource, dataCollector, investigator, imsType;
  public MSnDefinition multiStageFragmentation;
  public SpectralQuality quality;
  public Double purity;
  public Integer numSignals;

  public String classyFireSuperclass;
  public String classyFireClass;
  public String classyFireSubclass;
  public String classyFireParent;
  public String npClassifierSuperclass;
  public String npClassifierPathway;
  public String npClassifierClass;


  @JsonDeserialize(using = SpectrumDeserializer.class)
  public double[][] signals;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  public static MZmineJsonLibraryEntry fromSpectralLibraryEntry(SpectralLibraryEntry lib) {
    MZmineJsonLibraryEntry entry = new MZmineJsonLibraryEntry();
    entry.signals = new double[][]{lib.getMzValues(new double[0]),
        lib.getIntensityValues(new double[0])};
    for (final Entry<DBEntryField, Object> field : lib.getFields().entrySet()) {
      entry.set(field.getKey(), field.getValue());
    }
    // handle more complex objects
    entry.multiStageFragmentation = MSnDefinition.fromSpectralLibraryEntry(lib);
    return entry;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public Object get(DBEntryField field) {
    return switch (field) {
      case ENTRY_ID -> entryId;
      case NAME -> compoundName;
      case SYNONYMS -> synonyms;
      case COMMENT -> comment;
      case EXACT_MASS -> exactMass;
      case FORMULA -> formula;
      case INCHI -> inchi;
      case INCHIKEY -> inchikey;
      case SMILES -> smiles;
      case PEPTIDE_SEQ -> peptideSequence;
      case CLASSYFIRE_SUPERCLASS -> classyFireSuperclass;
      case CLASSYFIRE_CLASS -> classyFireClass;
      case CLASSYFIRE_SUBCLASS -> classyFireSubclass;
      case CLASSYFIRE_PARENT -> classyFireParent;
      case NPCLASSIFIER_SUPERCLASS -> npClassifierSuperclass;
      case NPCLASSIFIER_CLASS -> npClassifierClass;
      case NPCLASSIFIER_PATHWAY -> npClassifierPathway;
      case CAS -> cas;

      case MS_LEVEL -> msLevel;
      case RT -> rt;
      case CCS -> ccs;
      case ION_TYPE -> adduct;
      case IMS_TYPE -> imsType;
      case PRECURSOR_MZ -> precursorMz;
      case CHARGE -> charge;
      case MERGED_SPEC_TYPE -> mergedSpectrumType;
      case COLLISION_ENERGY -> fragmentationEnergy;
      case FRAGMENTATION_METHOD -> fragmentationMethod;
      case ISOLATION_WINDOW -> isolationWindow;
      case ONLINE_REACTIVITY -> null;
      case NUM_PEAKS -> numSignals;
      case ACQUISITION -> compoundSource;
      case MSN_COLLISION_ENERGIES ->
          multiStageFragmentation == null ? null : multiStageFragmentation.fragmentationEnergies();
      case MSN_PRECURSOR_MZS ->
          multiStageFragmentation == null ? null : multiStageFragmentation.precursorMzs();
      case MSN_FRAGMENTATION_METHODS ->
          multiStageFragmentation == null ? null : multiStageFragmentation.fragmentationMethods();
      case MSN_ISOLATION_WINDOWS ->
          multiStageFragmentation == null ? null : multiStageFragmentation.precursorWindows();
      case INSTRUMENT_TYPE -> instrumentType;
      case INSTRUMENT -> instrument;
      case ION_SOURCE -> ionSource;
      case RESOLUTION -> resolution;
      case POLARITY -> polarity;
      case PRINCIPAL_INVESTIGATOR -> investigator;
      case DATA_COLLECTOR -> dataCollector;
      case SOFTWARE -> softwaresource;
      case DATASET_ID -> datasetId;
      case FILENAME -> null;
      case USI -> usi;
      case SOURCE_SCAN_USI -> null;
      case SPLASH -> splash;
      case QUALITY -> quality;
      case QUALITY_PRECURSOR_PURITY -> purity;
      case QUALITY_CHIMERIC -> quality != null ? quality.chimeric() : null;
      case QUALITY_EXPLAINED_INTENSITY -> quality != null ? quality.explainedIntensity() : null;
      case QUALITY_EXPLAINED_SIGNALS -> quality != null ? quality.explainedSignals() : null;
      case DESCRIPTION -> null;
      case MOLWEIGHT -> null;
      case PUBMED -> null;
      case PUBCHEM -> null;
      case GNPS_ID -> null;
      case MONA_ID -> null;
      case CHEMSPIDER -> null;
      case MERGED_N_SAMPLES -> null;
      case SIRIUS_MERGED_SCANS -> null;
      case SIRIUS_MERGED_STATS -> null;
      case OTHER_MATCHED_COMPOUNDS_N -> null;
      case OTHER_MATCHED_COMPOUNDS_NAMES -> null;
      case FEATURE_ID, FEATURELIST_NAME_FEATURE_ID, FEATURE_MS1_HEIGHT, FEATURE_MS1_REL_HEIGHT ->
          null;
      case SCAN_NUMBER -> scanNumber;
      case UNSPECIFIED -> null;
    };
  }

  public void set(DBEntryField field, Object value) {
    if (value == null) {
      return;
    }
    switch (field) {
      case CLASSYFIRE_SUPERCLASS -> classyFireSuperclass = value.toString();
      case CLASSYFIRE_CLASS -> classyFireClass = value.toString();
      case CLASSYFIRE_SUBCLASS -> classyFireSubclass = value.toString();
      case CLASSYFIRE_PARENT -> classyFireParent = value.toString();
      case NPCLASSIFIER_SUPERCLASS -> npClassifierSuperclass = value.toString();
      case NPCLASSIFIER_CLASS -> npClassifierClass = value.toString();
      case NPCLASSIFIER_PATHWAY -> npClassifierPathway = value.toString();
      case ENTRY_ID -> entryId = value.toString();
      case NAME -> compoundName = value.toString();
      case SYNONYMS -> synonyms.addAll((Collection<? extends String>) value);
      case COMMENT -> comment = value.toString();
      case FORMULA -> formula = value.toString();
      case INCHI -> inchi = value.toString();
      case INCHIKEY -> inchikey = value.toString();
      case SMILES -> smiles = value.toString();
      case CAS -> cas = value.toString();
      case ION_TYPE -> adduct = value.toString();
      case MS_LEVEL -> msLevel = (int) value;
      case CHARGE -> charge = (int) value;
      case NUM_PEAKS -> numSignals = (int) value;
      case EXACT_MASS -> exactMass = (double) value;
      case RT -> rt = (double) value;
      case CCS -> ccs = (double) value;
      case PRECURSOR_MZ -> precursorMz = (double) value;
      case MERGED_SPEC_TYPE -> mergedSpectrumType = value.toString();
      case COLLISION_ENERGY -> fragmentationEnergy = (FloatArrayList) value;
      case FRAGMENTATION_METHOD -> fragmentationMethod = value.toString();
      case ISOLATION_WINDOW -> isolationWindow = (double) value;
      case ACQUISITION -> compoundSource = value.toString();
      case INSTRUMENT_TYPE -> instrumentType = value.toString();
      case INSTRUMENT -> instrument = value.toString();
      case ION_SOURCE -> ionSource = value.toString();
      case RESOLUTION -> resolution = value.toString();
      case POLARITY -> polarity = value.toString();
      case PRINCIPAL_INVESTIGATOR -> investigator = value.toString();
      case DATA_COLLECTOR -> dataCollector = value.toString();
      case SOFTWARE -> softwaresource = value.toString();
      case DATASET_ID -> datasetId = value.toString();
      case USI -> usi = value.toString();
      case SPLASH -> splash = value.toString();
      case QUALITY -> quality = (SpectralQuality) value;
    }
  }

  public SpectralLibraryEntry toSpectralLibraryEntry(@Nullable SpectralLibrary library) {
    MemoryMapStorage storage = library == null ? null : library.getStorage();
    SpectralDBEntry entry = new SpectralDBEntry(storage, signals[0], signals[1]);
    for (var field : DBEntryField.values()) {
      entry.putIfNotNull(field, get(field));
    }
    return entry;
  }
}
