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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spectral library entry is a mass spectrum that can be memory mapped (memory map defined in
 * {@link SpectralLibrary}.
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public interface SpectralLibraryEntry extends MassList {

  Logger logger = Logger.getLogger(SpectralLibraryEntry.class.getName());
  String XML_ELEMENT_ENTRY = "spectraldatabaseentry";

  static SpectralLibraryEntry create(@Nullable MemoryMapStorage storage, double precursorMZ,
      DataPoint[] dps) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(dps);
    Map<DBEntryField, Object> fields = new HashMap<>();
    fields.put(DBEntryField.PRECURSOR_MZ, precursorMZ);
    fields.put(DBEntryField.NUM_PEAKS, dps.length);
    return new SpectralDBEntry(storage, data[0], data[1], fields);
  }

  static SpectralLibraryEntry create(@Nullable MemoryMapStorage storage, double precursorMZ,
      int charge, DataPoint[] dps) {
    SpectralLibraryEntry entry = create(storage, precursorMZ, dps);
    entry.putIfNotNull(DBEntryField.CHARGE, charge);
    return entry;
  }

  static SpectralLibraryEntry create(@Nullable MemoryMapStorage storage,
      Map<DBEntryField, Object> fields, DataPoint[] dps) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(dps);
    return new SpectralDBEntry(storage, data[0], data[1], fields);
  }

  /**
   * Create a new spectral library entry from any {@link FeatureAnnotation} but new spectral data
   *
   * @param scan        only used for scan metadata - data is provided through dataPoints
   * @param match       the annotation for additional metadata
   * @param dataPoints  the actual data
   * @param metadataMap add additional fields to the spectral library entry
   * @return spectral library entry
   */
  static SpectralLibraryEntry create(final FeatureListRow row, @Nullable MemoryMapStorage storage,
      final Scan scan, final FeatureAnnotation match, final DataPoint[] dataPoints,
      final @Nullable Map<DBEntryField, Object> metadataMap) {

    Double precursorMZ = Objects.requireNonNullElse(match.getPrecursorMZ(), scan.getPrecursorMz());
    SpectralLibraryEntry entry = create(storage, precursorMZ, dataPoints);

    // add additional fields early
    if (metadataMap != null) {
      entry.putAll(metadataMap);
    }

    // write feature ID as feature list and row ID to identify MSn trees or MS2 spectra of the same row
    var flist = row.getFeatureList();
    if (flist != null) {
      entry.putIfNotNull(DBEntryField.FEATURE_ID, flist.getName() + ":" + row.getID());
    }

    // transfer match to fields
    entry.addFeatureAnnotationFields(match);

    // scan details
    Integer charge = getCharge(match, row, scan);
    if (charge != null) {
      entry.putIfNotNull(DBEntryField.CHARGE, charge);
    }
    entry.putIfNotNull(DBEntryField.POLARITY, scan.getPolarity());

    MsMsInfo msMsInfo = scan.getMsMsInfo();
    if (msMsInfo instanceof MSnInfoImpl msnInfo) {
      // energies are quite complex
      // [MS2, MS3, MS4] and multiple energies in last level due to merging
      var msnEnergies = ScanUtils.extractMSnCollisionEnergies(scan);
      if (!msnEnergies.isEmpty()) {
        entry.putIfNotNull(DBEntryField.MSN_COLLISION_ENERGIES, msnEnergies);
      }
      //
      List<DDAMsMsInfo> precursors = msnInfo.getPrecursors();
      entry.putIfNotNull(DBEntryField.MSN_PRECURSOR_MZS,
          extractJsonList(precursors, DDAMsMsInfo::getIsolationMz));
      entry.putIfNotNull(DBEntryField.MSN_FRAGMENTATION_METHODS,
          extractJsonList(precursors, DDAMsMsInfo::getActivationMethod));
      entry.putIfNotNull(DBEntryField.MSN_ISOLATION_WINDOWS, extractJsonList(precursors, info -> {
        Range<Double> window = info.getIsolationWindow();
        return window == null ? null : RangeUtils.rangeLength(window);
      }));
      entry.putIfNotNull(DBEntryField.MS_LEVEL, msnInfo.getMsLevel());
    } else if (msMsInfo != null) {
      entry.putIfNotNull(DBEntryField.FRAGMENTATION_METHOD, msMsInfo.getActivationMethod());
      Range<Double> window = msMsInfo.getIsolationWindow();
      if (window != null) {
        entry.putIfNotNull(DBEntryField.ISOLATION_WINDOW, RangeUtils.rangeLength(window));
      }
      entry.putIfNotNull(DBEntryField.MS_LEVEL, msMsInfo.getMsLevel());
    }
    List<Float> energies = ScanUtils.extractCollisionEnergies(scan);
    if (!energies.isEmpty()) {
      entry.putIfNotNull(DBEntryField.COLLISION_ENERGY, energies);
    }

    // merged scans are derived from multiple source scans - add all information here and overwrite
    String datasetID = entry.getOrElse(DBEntryField.DATASET_ID, null);
    entry.putIfNotNull(DBEntryField.USI, ScanUtils.extractUSI(scan, datasetID).toList());

    if (scan instanceof MergedMassSpectrum merged) {
      entry.putIfNotNull(DBEntryField.MS_LEVEL, merged.getMSLevel());
      entry.putIfNotNull(DBEntryField.SCAN_NUMBER,
          ScanUtils.extractScanNumbers(merged).boxed().toList());
      entry.putIfNotNull(DBEntryField.MERGED_SPEC_TYPE, merged.getMergingType());
    } else {
      entry.putIfNotNull(DBEntryField.MERGED_SPEC_TYPE, MergingType.SINGLE_BEST_SCAN);
      entry.putIfNotNull(DBEntryField.SCAN_NUMBER, scan.getScanNumber());
    }

    return entry;
  }

  /**
   * Add metadata to spectral library entry from feature annotation.
   *
   * @param match
   */
  default void addFeatureAnnotationFields(FeatureAnnotation match) {
    switch (match) {
      case CompoundDBAnnotation dbmatch -> addAnnotationFields(dbmatch);
      case SpectralLibraryEntry dbmatch -> addAnnotationFields(dbmatch);
      case FeatureAnnotation _ -> {
        putIfNotNull(DBEntryField.ION_TYPE, match.getAdductType());
        putIfNotNull(DBEntryField.CCS, match.getCCS());
        putIfNotNull(DBEntryField.NAME, match.getCompoundName());
        putIfNotNull(DBEntryField.FORMULA, match.getFormula());
        putIfNotNull(DBEntryField.INCHI, match.getInChI());
        putIfNotNull(DBEntryField.INCHIKEY, match.getInChIKey());
        putIfNotNull(DBEntryField.SMILES, match.getSmiles());
      }
    }
  }

  default void addAnnotationFields(CompoundDBAnnotation match) {
    for (var dbentry : match.getReadOnlyMap().entrySet()) {
      DBEntryField field = switch (dbentry.getKey()) {
        case RTType _ -> DBEntryField.RT;
        case CompoundNameType _ -> DBEntryField.NAME;
        case FormulaType _ -> DBEntryField.FORMULA;
        case SmilesStructureType _ -> DBEntryField.SMILES;
        case InChIStructureType _ -> DBEntryField.INCHI;
        case InChIKeyStructureType _ -> DBEntryField.INCHIKEY;
        case CCSType _ -> DBEntryField.CCS;
        case ChargeType _ -> DBEntryField.CHARGE;
        case NeutralMassType _ -> DBEntryField.EXACT_MASS;
        case CommentType _ -> DBEntryField.COMMENT;
        case IonTypeType _ -> DBEntryField.ION_TYPE;
//        case SynonymType _ -> DBEntryField.SYNONYM;
        default -> null;
      };
      try {
        putIfNotNull(field, dbentry.getValue());
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Types were not converted from DB match to DB entry " + ex.getMessage(), ex);
      }
    }
  }

  default void addAnnotationFields(SpectralLibraryEntry match) {
    for (var dbentry : match.getFields().entrySet()) {
      switch (dbentry.getKey()) {
        case RT, NAME, FORMULA, SMILES, INCHI, INCHIKEY, EXACT_MASS, ION_TYPE, SYNONYMS, CAS,
             PUBCHEM, PUBMED, MOLWEIGHT -> putIfNotNull(dbentry.getKey(), dbentry.getValue());
      }
    }
  }

  static Integer getCharge(FeatureAnnotation match, FeatureListRow row, Scan scan) {
    IonType adduct = match.getAdductType();
    if (adduct != null) {
      return adduct.getCharge();
    }
    return FeatureUtils.extractBestSignedChargeState(row, scan);
  }

  private static List<?> extractJsonList(final List<DDAMsMsInfo> precursors,
      Function<DDAMsMsInfo, Object> extractor) {
    return precursors.stream().map(extractor).filter(Objects::nonNull).toList();
  }

  static SpectralLibraryEntry loadFromXML(XMLStreamReader reader, MZmineProject project)
      throws XMLStreamException {
    return SpectralDBEntry.loadFromXML(reader, project);
  }

  void putAll(Map<DBEntryField, Object> fields);

  /**
   * @return True if the value was not null and the value was placed in the field.
   */
  boolean putIfNotNull(DBEntryField field, Object value);

  Double getPrecursorMZ();

  Optional<Object> getField(DBEntryField f);

  <T> T getOrElse(DBEntryField f, T defaultValue);

  Map<DBEntryField, Object> getFields();

  void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  default boolean setCharge(@Nullable Integer charge) {
    return setCharge(charge, PolarityType.fromInt(charge));
  }

  /**
   * Sets the charge and the polarity of this entry. The polarity overrides any +/- in the charge
   * integer.
   *
   * @return {@link #putIfNotNull(DBEntryField, Object)}
   */
  default boolean setCharge(@Nullable Integer charge, @Nullable PolarityType polarity) {
    if (charge == null) {
      return false;
    }
    return putIfNotNull(DBEntryField.CHARGE,
        Math.abs(charge) + Objects.requireNonNullElse(polarity, PolarityType.POSITIVE)
            .asSingleChar());
  }

  /**
   * Extract MS level from scan
   */
  @NotNull
  default Optional<Integer> getMsLevel() {
    return getAsInteger(DBEntryField.MS_LEVEL);
  }

  /**
   * Extract MS level from scan
   *
   * @return null if none provided
   */
  @Nullable
  default Double getPrecursorMz() {
    return getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null);
  }

  @NotNull
  default PolarityType getPolarity() {
    return getField(DBEntryField.POLARITY).map(Object::toString).map(PolarityType::parseFromString)
        .orElse(PolarityType.UNKNOWN);
  }

  default Optional<Float> getAsFloat(DBEntryField field) {
    try {
      return getField(field).map(this::toFloat);
    } catch (Exception ex) {
      logger.finest("Cannot convert to float " + field.toString() + " value: " + getField(field));
      return Optional.empty();
    }
  }

  default Float toFloat(Object v) {
    return switch (v) {
      case Number n -> n.floatValue();
      case String s -> Float.parseFloat(s);
      default -> null;
    };
  }

  default Optional<Double> getAsDouble(DBEntryField field) {
    try {
      return getField(field).map(this::toDouble);
    } catch (Exception ex) {
      logger.finest("Cannot convert to double " + field.toString() + " value: " + getField(field));
      return Optional.empty();
    }
  }

  default Double toDouble(Object v) {
    return switch (v) {
      case Number n -> n.doubleValue();
      case String s -> Double.parseDouble(s);
      default -> null;
    };
  }

  default Optional<Integer> getAsInteger(DBEntryField field) {
    try {
      return getField(field).map(this::toInteger);
    } catch (Exception ex) {
      logger.finest("Cannot convert to integer " + field.toString() + " value: " + getField(field));
      return Optional.empty();
    }
  }

  default Integer toInteger(Object v) {
    return switch (v) {
      case Number n -> n.intValue();
      case String s -> Integer.parseInt(s);
      default -> null;
    };
  }

  @Nullable
  SpectralLibrary getLibrary();

  void setLibrary(@Nullable SpectralLibrary library);

  @Nullable
  String getLibraryName();

  /**
   * @return the structure parsed from smiles or inchi
   */
  MolecularStructure getStructure();
}
