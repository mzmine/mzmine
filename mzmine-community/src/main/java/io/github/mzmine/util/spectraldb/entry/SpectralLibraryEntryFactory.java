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
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorFlag;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorResults;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReactionJsonWriter;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Control generation of library entries from matches, scans, rows
 */
public class SpectralLibraryEntryFactory {

  private static final Logger logger = Logger.getLogger(
      SpectralLibraryEntryFactory.class.getName());
  private final boolean compactUSI;
  private final boolean addExperimentalResults;
  private final boolean addAnnotation;
  private final boolean useRowIdAsScanNumber;
  private boolean flagChimerics = true;
  // online reaction workflow to flag spectra as potential educts or products of reactions
  private final OnlineReactionJsonWriter reactionJsonWriter = new OnlineReactionJsonWriter(false);
  private boolean addOnlineReactivityFlags = false;

  /**
   * Experimental results and annotation added
   *
   * @param compactUSI may reduce compatibility if true but compacting scan numbers of the same file
   *                   into a list of ranges
   */
  public SpectralLibraryEntryFactory(final boolean compactUSI) {
    this(compactUSI, false, true, true);
  }

  /**
   * @param compactUSI             may reduce compatibility if true but compacting scan numbers of
   *                               the same file into a list of ranges
   * @param addExperimentalResults add experimental results like retention time, CCS, feature
   *                               Height
   * @param addAnnotation          add annotation fields if available
   */
  public SpectralLibraryEntryFactory(final boolean compactUSI, final boolean useRowIdAsScanNumber,
      final boolean addExperimentalResults, final boolean addAnnotation) {
    this.compactUSI = compactUSI;
    this.useRowIdAsScanNumber = useRowIdAsScanNumber;
    this.addExperimentalResults = addExperimentalResults;
    this.addAnnotation = addAnnotation;
  }

  static List<?> extractJsonList(final List<DDAMsMsInfo> precursors,
      Function<DDAMsMsInfo, Object> extractor) {
    return precursors.stream().map(extractor).filter(Objects::nonNull).toList();
  }

  /**
   * General spectral library creation independent of this factories internal state
   */
  public static SpectralLibraryEntry create(@Nullable MemoryMapStorage storage,
      @Nullable Double precursorMZ, DataPoint[] dps) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(dps);
    Map<DBEntryField, Object> fields = new HashMap<>();
    if (precursorMZ != null) {
      fields.put(DBEntryField.PRECURSOR_MZ, precursorMZ);
    }
    fields.put(DBEntryField.NUM_PEAKS, dps.length);
    return new SpectralDBEntry(storage, data[0], data[1], fields);
  }

  /**
   * General spectral library creation independent of this factories internal state
   */
  public static SpectralLibraryEntry create(@Nullable MemoryMapStorage storage,
      @Nullable Double precursorMZ, int charge, DataPoint[] dps) {
    SpectralLibraryEntry entry = create(storage, precursorMZ, dps);
    entry.putIfNotNull(DBEntryField.CHARGE, charge);
    return entry;
  }

  /**
   * General spectral library creation independent of this factories internal state
   */
  public static SpectralLibraryEntry create(@Nullable MemoryMapStorage storage,
      Map<DBEntryField, Object> fields, DataPoint[] dps) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(dps);
    return new SpectralDBEntry(storage, data[0], data[1], fields);
  }

  /**
   * Create a new spectral library entry from any row, scan, and {@link FeatureAnnotation} - all
   * three optional. Already processed data can be provided as DataPoint[].
   *
   * @param feature     if present this feature will be used for experimental data like RT and CCS
   * @param scan        only used for scan metadata - data is provided through dataPoints
   * @param match       the annotation for additional metadata
   * @param dataPoints  the actual data
   * @param metadataMap add additional fields to the spectral library entry
   * @return spectral library entry
   */
  public SpectralLibraryEntry create(@Nullable MemoryMapStorage storage,
      final @Nullable FeatureListRow row, final @Nullable Feature feature,
      final @Nullable MassSpectrum scan, @Nullable final FeatureAnnotation match,
      final @NotNull DataPoint[] dataPoints,
      final @Nullable Map<DBEntryField, Object> metadataMap) {

    var precursorMZ = FeatureUtils.getPrecursorMz(match, row, scan);
    SpectralLibraryEntry entry = SpectralLibraryEntryFactory.create(storage,
        precursorMZ.orElse(null), dataPoints);

    // add additional fields early
    if (metadataMap != null) {
      entry.putAll(metadataMap);
    }

    if (row != null) {
      // FEATURE_ID is used by GNPS and SIRIUS as simple number
      entry.putIfNotNull(DBEntryField.FEATURE_ID, row.getID());

      // write feature ID as feature list and row ID to identify MSn trees or MS2 spectra of the same row
      var flist = row.getFeatureList();
      if (flist != null) {
        entry.putIfNotNull(DBEntryField.FEATURELIST_NAME_FEATURE_ID,
            flist.getName() + ":" + row.getID());
      }
    }
    // add experimental data
    if (addExperimentalResults) {
      addExperimentalFeatureResults(entry, row, feature);
    }

    // transfer match to fields
    if (addAnnotation && match != null) {
      addFeatureAnnotationFields(entry, match);
    }

    // scan details
    if (scan != null) {
      addScanSpecificFields(entry, scan);
    }

    // combined fields extracted from multiple
    OptionalInt charge = FeatureUtils.extractBestSignedChargeState(row, scan, match);
    charge.ifPresent(c -> entry.putIfNotNull(DBEntryField.CHARGE, c));
    var polarity = FeatureUtils.extractBestPolarity(row, scan, match);
    polarity.ifPresent(pol -> entry.putIfNotNull(DBEntryField.POLARITY, pol));

    final Optional<IonType> ionType = FeatureUtils.extractBestIonIdentity(match, row);
    ionType.ifPresent(ion -> entry.putIfNotNull(DBEntryField.ION_TYPE, ion.toString(false)));

    // online reactivity workflow
    addOnlineReactivityFlags(entry, row);

    // all source filenames of scan (or feature or row)
    addFilenames(entry, feature, scan);

    // replicate what GNPS does - some tools rely on the scan number to be there
    // GNPS just uses the FEATURE_ID for this
    if (useRowIdAsScanNumber && row != null) {
      entry.putIfNotNull(DBEntryField.SCAN_NUMBER, row.getID());
    }

    return entry;
  }

  /**
   * Filenames usually from the scan (all source scans of a merged or simple scan) or if scan is
   * null from feature
   */
  public static void addFilenames(final SpectralLibraryEntry entry, final @Nullable Feature feature,
      final @Nullable MassSpectrum scan) {
    final List<String> filenames;
    if (scan != null) {
      filenames = ScanUtils.streamSourceScans(scan).map(ScanUtils::getSourceFile)
          .filter(Objects::nonNull).distinct().sorted().toList();
    } else if (feature != null && feature.getRawDataFile() != null) {
      filenames = List.of(feature.getRawDataFile().getName());
    } else {
      filenames = List.of();
    }
    if (!filenames.isEmpty()) {
      entry.putIfNotNull(DBEntryField.FILENAME, String.join(";", filenames));
      entry.putIfNotNull(DBEntryField.MERGED_N_SAMPLES, filenames.size());
    }
  }


  /**
   * This method is specific to unknown spectra. Also see
   * {@link #create(MemoryMapStorage, FeatureListRow, Feature, MassSpectrum, FeatureAnnotation,
   * DataPoint[], Map)} for generation of spectral library entries in a more general way.
   *
   * @param storage  optional memory storage to memory map data
   * @param row      row to export
   * @param feature
   * @param scan     only used for scan metadata - data is provided through dps dataPoints
   * @param dps      data points
   * @param chimeric chimeric result of scan.
   * @return the new spectral library entry
   */
  @NotNull
  public SpectralLibraryEntry createUnknown(final @Nullable MemoryMapStorage storage,
      final @Nullable FeatureListRow row, final @Nullable Feature feature,
      final @Nullable MassSpectrum scan, @NotNull final DataPoint[] dps,
      @Nullable final ChimericPrecursorResults chimeric,
      final @Nullable Map<DBEntryField, Object> metadataMap) {
    // add instrument type etc by parameter
    SpectralLibraryEntry entry = create(storage, row, feature, scan, null, dps, metadataMap);

    addChimericMs1PrecursorResults(entry, chimeric); // done after all so that name may be changed
    return entry;
  }

  /**
   * This method is specific to annotated compounds for library generation. Also see
   * {@link #create(MemoryMapStorage, FeatureListRow, Feature, MassSpectrum, FeatureAnnotation,
   * DataPoint[], Map)} for generation of spectral library entries in a more general way also for
   * unannotated.
   *
   * @param storage             optional memory storage to memory map data
   * @param row                 row that was matched
   * @param msmsScan            only used for scan metadata - data is provided through dps
   *                            dataPoints
   * @param match               the match to export
   * @param dps                 data points
   * @param chimeric            chimeric result of msmsScan
   * @param score               fragmentation pattern score
   * @param allMatchedCompounds filtered list of all matched compounds (one adduct per compound
   *                            name). also contains match which is currently exported.
   * @return the new spectral library entry
   */
  @NotNull
  public SpectralLibraryEntry createAnnotated(final @Nullable MemoryMapStorage storage,
      final @Nullable FeatureListRow row, final @Nullable MassSpectrum msmsScan,
      final @NotNull FeatureAnnotation match, @NotNull final DataPoint[] dps,
      @Nullable final ChimericPrecursorResults chimeric, final @Nullable MSMSScore score,
      final @Nullable List<FeatureAnnotation> allMatchedCompounds,
      final @Nullable Map<DBEntryField, Object> metadataMap) {
    // add instrument type etc by parameter
    SpectralLibraryEntry entry = create(storage, row, null, msmsScan, match, dps, metadataMap);

    // only add fields here that are specific to library generation
    // all other fields should be added in {@link SpectralLibraryEntryFactory}

    // matched against mutiple compounds in the same sample?
    // usually metadata is filtered so that raw data files only contain specific compounds without interference
    addOtherMatchedCompounds(entry, match, allMatchedCompounds);

    // score might be successful without having a formula - so check if we actually have scores
    addMsMsScore(entry, score);

    addChimericMs1PrecursorResults(entry, chimeric); // done after all so that name may be changed

    return entry;
  }

  public void addScanSpecificFields(@NotNull final SpectralLibraryEntry entry,
      final @Nullable MassSpectrum spec) {
    if (spec == null) {
      return;
    }
    if (spec instanceof Scan scan) {
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
    }

    List<Float> energies = ScanUtils.extractCollisionEnergies(spec);
    if (!energies.isEmpty()) {
      FloatArrayList list = new FloatArrayList(energies);
      entry.putIfNotNull(DBEntryField.COLLISION_ENERGY, list);
    }

    addUniversalSpectrumIdentifiers(entry, spec);

    final MergingType mergingType;
    if (spec instanceof MergedMassSpectrum merged) {
      entry.putIfNotNull(DBEntryField.MS_LEVEL, merged.getMSLevel());
      entry.putIfNotNull(DBEntryField.SCAN_NUMBER,
          ScanUtils.extractScanNumbers(merged).boxed().toList());
      mergingType = merged.getMergingType();
    } else {
      mergingType = MergingType.SINGLE_SCAN;
      entry.putIfNotNull(DBEntryField.SCAN_NUMBER, ScanUtils.extractScanNumber(spec));
    }
    if (mergingType != null) {
      entry.putIfNotNull(DBEntryField.MERGED_SPEC_TYPE, mergingType.getUniqueID());
    }
  }


  /**
   * Flag chimeric precursor isolation in name
   */
  public void setFlagChimerics(final boolean flagChimerics) {
    this.flagChimerics = flagChimerics;
  }

  public void addChimericMs1PrecursorResults(final @NotNull SpectralLibraryEntry entry,
      final @Nullable ChimericPrecursorResults chimeric) {
    if (!flagChimerics || chimeric == null) {
      return;
    }

    entry.putIfNotNull(DBEntryField.QUALITY_PRECURSOR_PURITY, chimeric.purity());
    entry.putIfNotNull(DBEntryField.QUALITY_CHIMERIC, chimeric.flag());
    if (ChimericPrecursorFlag.CHIMERIC.equals(chimeric.flag())) {
      entry.putIfNotNull(DBEntryField.NAME,
          entry.getField(DBEntryField.NAME).orElse("") + " (Chimeric precursor selection)");
    }
  }

  public void addUniversalSpectrumIdentifiers(final @NotNull SpectralLibraryEntry entry,
      final @NotNull MassSpectrum scan) {
    // merged scans are derived from multiple source scans - add all USI here
    String datasetID = entry.getOrElse(DBEntryField.DATASET_ID, null);
    final List<String> usis;
    if (compactUSI) {
      usis = ScanUtils.extractCompressedUSIRanges(scan, datasetID).toList();
    } else {
      usis = ScanUtils.extractUSI(scan, datasetID).toList();
    }
    entry.putIfNotNull(DBEntryField.SOURCE_SCAN_USI, usis);
  }

  /**
   * Add metadata to spectral library entry from feature annotation.
   */
  public void addFeatureAnnotationFields(@NotNull final SpectralLibraryEntry entry,
      @Nullable FeatureAnnotation match) {
    if (!addAnnotation || match == null) {
      return;
    }
    switch (match) {
      case CompoundDBAnnotation dbmatch -> addAnnotationFields(entry, dbmatch);
      case SpectralLibraryEntry dbmatch -> addAnnotationFields(entry, dbmatch);
      case FeatureAnnotation _ -> {
        entry.putIfNotNull(DBEntryField.ION_TYPE, match.getAdductType());
        entry.putIfNotNull(DBEntryField.CCS, match.getCCS());
        entry.putIfNotNull(DBEntryField.NAME, match.getCompoundName());
        entry.putIfNotNull(DBEntryField.FORMULA, match.getFormula());
        entry.putIfNotNull(DBEntryField.INCHI, match.getInChI());
        entry.putIfNotNull(DBEntryField.INCHIKEY, match.getInChIKey());
        entry.putIfNotNull(DBEntryField.SMILES, match.getSmiles());
      }
    }
  }

  public void addAnnotationFields(@NotNull final SpectralLibraryEntry entry,
      @Nullable CompoundDBAnnotation match) {
    if (!addAnnotation || match == null) {
      return;
    }
    for (var dbentry : match.getReadOnlyMap().entrySet()) {
      DBEntryField field = DBEntryField.fromDataType(dbentry.getKey());
      if (field == DBEntryField.UNSPECIFIED) {
        continue;
      }
      try {
        entry.putIfNotNull(field, dbentry.getValue());
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Types were not converted from DB match to DB entry " + ex.getMessage(), ex);
      }
    }
  }

  public void addAnnotationFields(@NotNull final SpectralLibraryEntry entry,
      @Nullable SpectralLibraryEntry match) {
    if (!addAnnotation || match == null) {
      return;
    }
    for (var dbentry : match.getFields().entrySet()) {
      switch (dbentry.getKey()) {
        case RT, NAME, FORMULA, SMILES, INCHI, INCHIKEY, EXACT_MASS, ION_TYPE, SYNONYMS, CAS,
             PUBCHEM, PUBMED, MOLWEIGHT -> entry.putIfNotNull(dbentry.getKey(), dbentry.getValue());
      }
    }
  }

  public void addMsMsScore(@NotNull final SpectralLibraryEntry entry,
      @Nullable final MSMSScore score) {
    if (score != null && score.explainedSignals() > 0) {
      entry.putIfNotNull(DBEntryField.QUALITY_EXPLAINED_INTENSITY, score.explainedIntensity());
      entry.putIfNotNull(DBEntryField.QUALITY_EXPLAINED_SIGNALS, score.explainedSignals());
    }
  }

  /**
   * Adds count of other matched compounds with different compound name
   *
   * @param match               the match to export
   * @param allMatchedCompounds filtered list of all matched compounds (one adduct per compound
   *                            name). also contains match which is currently exported.
   */
  public void addOtherMatchedCompounds(@NotNull final SpectralLibraryEntry entry,
      final @NotNull FeatureAnnotation match,
      @Nullable final List<FeatureAnnotation> allMatchedCompounds) {
    if (allMatchedCompounds == null) {
      return;
    }
    // matched against mutiple compounds in the same sample?
    // usually metadata is filtered so that raw data files only contain specific compounds without interference
    if (allMatchedCompounds.size() > 1) {
      // 1 would be the match itself
      entry.putIfNotNull(DBEntryField.OTHER_MATCHED_COMPOUNDS_N, allMatchedCompounds.size() - 1);
      entry.putIfNotNull(DBEntryField.OTHER_MATCHED_COMPOUNDS_NAMES, allMatchedCompounds.stream()
          .filter(m -> !Objects.equals(match.getCompoundName(), m.getCompoundName()))
          .map(FeatureAnnotation::toString).collect(Collectors.joining("; ")));
    }
  }

  /**
   * Put experimental results from feature to entry. Prefer feature over row
   */
  public void addExperimentalFeatureResults(@NotNull SpectralLibraryEntry entry,
      final @Nullable FeatureListRow row, @Nullable Feature f) {
    if (!addExperimentalResults) {
      return;
    }

    if (row != null) {
      entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, row.getAverageMZ());
      entry.putIfNotNull(DBEntryField.RT, row.getAverageRT());
      entry.putIfNotNull(DBEntryField.CCS, row.getAverageCCS());
      entry.putIfNotNull(DBEntryField.FEATURE_MS1_HEIGHT, row.getMaxHeight());
    }
    if (f != null) {
      entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, f.getMZ());
      entry.putIfNotNull(DBEntryField.RT, f.getRT());
      entry.putIfNotNull(DBEntryField.CCS, f.getCCS());
      entry.putIfNotNull(DBEntryField.FEATURE_MS1_HEIGHT, f.getHeight());
    }
  }

  public void setAddOnlineReactivityFlags(final boolean addOnlineReactivityFlags) {
    this.addOnlineReactivityFlags = addOnlineReactivityFlags;
  }

  /**
   * Add online reactivity fields if activated by flag
   */
  public void addOnlineReactivityFlags(@NotNull final SpectralLibraryEntry entry,
      @Nullable final FeatureListRow row) {
    if (row == null || !addOnlineReactivityFlags) {
      return;
    }

    // reactivity only for MS1
    if (entry.getMsLevel().stream().anyMatch(msLevel -> msLevel == 1)) {
      String reactivityString = reactionJsonWriter.createReactivityString(row,
          row.getOnlineReactionMatches());

      if (reactivityString != null) {
        entry.putIfNotNull(DBEntryField.ONLINE_REACTIVITY, reactivityString);
      }
    }
  }
}
