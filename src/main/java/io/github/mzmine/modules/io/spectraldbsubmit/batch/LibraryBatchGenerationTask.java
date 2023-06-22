/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import static io.github.mzmine.util.scans.ScanUtils.extractDataPoints;
import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorChecker;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorFlag;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorResults;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters.ChimericMsOption;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MSPEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MZmineJsonGenerator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.FragmentScanSelection.IncludeInputSpectra;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class LibraryBatchGenerationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LibraryBatchGenerationTask.class.getName());
  private final SpectralLibrary library;
  private final ModularFeatureList[] flists;
  private final File outFile;
  private final SpectralLibraryExportFormats format;
  private final ParameterSet parameters;
  private final Map<DBEntryField, Object> metadataMap;
  private final boolean handleChimerics;
  private final FragmentScanSelection selection;
  private final MsMsQualityChecker msMsQualityChecker;
  private final MZTolerance mzTolMerging;
  private final boolean enableMsnMerge;
  private final MsLevelFilter postMergingMsLevelFilter;
  public long totalRows = 0;
  public AtomicInteger finishedRows = new AtomicInteger(0);
  public AtomicInteger exported = new AtomicInteger(0);
  private double minimumPrecursorPurity = 0d;
  private MZTolerance chimericsMainIonMzTol;
  private MZTolerance chimericsIsolationMzTol;
  private ChimericMsOption handleChimericsOption;
  private String description = "Batch exporting spectral library";

  public LibraryBatchGenerationTask(final ParameterSet parameters, final Instant moduleCallDate) {
    super(null, moduleCallDate);
    flists = parameters.getValue(LibraryBatchGenerationParameters.flists).getMatchingFeatureLists();
    format = parameters.getValue(LibraryBatchGenerationParameters.exportFormat);
    this.parameters = parameters;
    String exportFormat = format.getExtension();
    File file = parameters.getValue(LibraryBatchGenerationParameters.file);
    outFile = FileAndPathUtil.getRealFilePath(file, exportFormat);

    library = new SpectralLibrary(MemoryMapStorage.forMassList(), outFile.getName() + "_batch",
        outFile);
    // metadata as a map
    LibraryBatchMetadataParameters meta = parameters.getParameter(
        LibraryBatchGenerationParameters.metadata).getEmbeddedParameters();
    metadataMap = meta.asMap();

    msMsQualityChecker = parameters.getParameter(LibraryBatchGenerationParameters.quality)
        .getEmbeddedParameters().toQualityChecker();

    postMergingMsLevelFilter = parameters.getValue(
        LibraryBatchGenerationParameters.postMergingMsLevelFilter);

    enableMsnMerge = parameters.getValue(LibraryBatchGenerationParameters.mergeMzTolerance);
    mzTolMerging = parameters.getEmbeddedParameterValue(
        LibraryBatchGenerationParameters.mergeMzTolerance);
    //
    handleChimerics = parameters.getValue(LibraryBatchGenerationParameters.handleChimerics);
    if (handleChimerics) {
      HandleChimericMsMsParameters param = parameters.getParameter(
          LibraryBatchGenerationParameters.handleChimerics).getEmbeddedParameters();
      minimumPrecursorPurity = param.getValue(HandleChimericMsMsParameters.minimumPrecursorPurity);
      chimericsIsolationMzTol = param.getValue(HandleChimericMsMsParameters.isolationWindow);
      chimericsMainIonMzTol = param.getValue(HandleChimericMsMsParameters.mainMassWindow);
      handleChimericsOption = param.getValue(HandleChimericMsMsParameters.option);
    }

    // used to extract and merge spectra
    selection = new FragmentScanSelection(mzTolMerging, true,
        IncludeInputSpectra.HIGHEST_TIC_PER_ENERGY, IntensityMergingType.MAXIMUM,
        postMergingMsLevelFilter);
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : (double) finishedRows.get() / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalRows = Arrays.stream(flists).mapToLong(ModularFeatureList::getNumberOfRows).sum();

    try (var writer = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
      for (ModularFeatureList flist : flists) {
        description = "Exporting entries for feature list " + flist.getName();
        for (var row : flist.getRows()) {
          processRow(writer, row);
          finishedRows.incrementAndGet();
        }
        flist.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(LibraryBatchGenerationModule.class, parameters,
                getModuleCallDate()));
      }
      //
      logger.info(String.format("Exported %d new library entries to file %s", exported.get(),
          outFile.getAbsolutePath()));
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + outFile + " for writing.");
      logger.log(Level.WARNING,
          String.format("Error writing library file: %s. Message: %s", outFile.getAbsolutePath(),
              e.getMessage()), e);
      return;
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not export library to " + outFile + " because of internal exception:"
                      + e.getMessage());
      logger.log(Level.WARNING,
          String.format("Error writing library file: %s. Message: %s", outFile.getAbsolutePath(),
              e.getMessage()), e);
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void processRow(final BufferedWriter writer, final FeatureListRow row)
      throws IOException {
    List<Scan> scans = row.getAllFragmentScans();

    var featureList = row.getFeatureList();
    // might filter matches for compound name in feature list name
    // only if this option is active
    List<CompoundDBAnnotation> matches = row.getCompoundAnnotations().stream()
        .filter(match -> msMsQualityChecker.matchesName(match, featureList)).toList();

    if (scans.isEmpty() || matches.isEmpty()) {
      return;
    }

    // first entry for the same molecule reflect the most common ion type, usually M+H
    // if multiple compounds match, they are sorted by score descending
    var filteredMatches = CompoundAnnotationUtils.getBestMatchesPerCompoundName(matches);

    if (filteredMatches.stream()
        .noneMatch(match -> msMsQualityChecker.matchesName(match, featureList))) {
      return;
    }

    // handle chimerics
    final var chimericMap = handleChimericsAndFilterScansIfSelected(row, scans);

    scans = selectMergeAndFilterScans(scans);
    // export
    exportAllMatches(writer, row, scans, filteredMatches, chimericMap);
  }

  /**
   * Selects scans from the list, merges them if active, and filters for MS2 if selected
   *
   * @param scans input list of scans usually from a row
   * @return list of selected scans
   */
  private List<Scan> selectMergeAndFilterScans(List<Scan> scans) {
    if (enableMsnMerge) {
      // merge spectra, find best spectrum for each MSn node in the tree and each energy
      // filter after merging scans to also generate PSEUDO MS2 from MSn spectra
      scans = selection.getAllFragmentSpectra(scans);
    } else {
      // filter scans if selection is only MS2
      if (postMergingMsLevelFilter.isFilter()) {
        scans.removeIf(postMergingMsLevelFilter::notMatch);
      }
    }
    return scans;
  }

  /**
   * Exports all matches
   *
   * @param scans           filtered scans
   * @param filteredMatches filtered annotations, each will be exported
   * @param chimericMap     flags chimeric spectra
   */
  private void exportAllMatches(final BufferedWriter writer, final FeatureListRow row,
      final List<Scan> scans, final List<CompoundDBAnnotation> filteredMatches,
      final Map<Scan, ChimericPrecursorResults> chimericMap) throws IOException {
    // filtered matches contain one match per compound name sorted by the least complex first
    // M+H better than 2M+H2+2
    for (final CompoundDBAnnotation match : filteredMatches) {
      // cache all formulas
      IMolecularFormula formula = FormulaUtils.getIonizedFormula(match);
      FormulaWithExactMz[] sortedFormulas = FormulaUtils.getAllFormulas(formula, 1, 15);

      boolean explainedSignalsOnly = msMsQualityChecker.exportExplainedSignalsOnly();
      // filter matches
      for (final Scan msmsScan : scans) {
        final MSMSScore score = msMsQualityChecker.match(msmsScan, match, sortedFormulas);
        if (score.isFailed(false)) {
          continue;
        }

        DataPoint[] dps = explainedSignalsOnly ? score.getAnnotatedDataPoints()
            : extractDataPoints(msmsScan, true);

        SpectralLibraryEntry entry = createEntry(row, match, chimericMap, msmsScan, score, dps,
            filteredMatches);
        exportEntry(writer, entry);
        exported.incrementAndGet();
      }
    }
  }


  /**
   * @param row                 row that was matched
   * @param match               the match to export
   * @param chimericMap         mpas scans to their chimeric results. might be empty if scoring was
   *                            off
   * @param msmsScan            scan to export
   * @param score               fragmentation pattern score
   * @param dps                 data points
   * @param allMatchedCompounds filtered list of all matched compounds (one adduct per compound
   *                            name). also contains match which is currently exported.
   * @return the new spectral library entry
   */
  @NotNull
  private SpectralLibraryEntry createEntry(final FeatureListRow row,
      final CompoundDBAnnotation match, final Map<Scan, ChimericPrecursorResults> chimericMap,
      final Scan msmsScan, final MSMSScore score, final DataPoint[] dps,
      final List<CompoundDBAnnotation> allMatchedCompounds) {
    // add instrument type etc by parameter
    SpectralLibraryEntry entry = SpectralLibraryEntry.create(library.getStorage(), msmsScan, match,
        dps);
    entry.putAll(metadataMap);

    // matched against mutiple compounds in the same sample?
    // usually metadata is filtered so that raw data files only contain specific compounds without interference
    if (allMatchedCompounds.size() > 1) {
      // 1 would be the match itself
      entry.putIfNotNull(DBEntryField.OTHER_MATCHED_COMPOUNDS_N, allMatchedCompounds.size() - 1);
      entry.putIfNotNull(DBEntryField.OTHER_MATCHED_COMPOUNDS_NAMES, allMatchedCompounds.stream()
          .filter(m -> !Objects.equals(match.getCompoundName(), m.getCompoundName()))
          .map(CompoundDBAnnotation::toString).collect(Collectors.joining("; ")));
    }
    // score might be successful without having a formula - so check if we actually have scores
    if (score.explainedSignals() > 0) {
      entry.putIfNotNull(DBEntryField.QUALITY_EXPLAINED_INTENSITY, score.explainedIntensity());
      entry.putIfNotNull(DBEntryField.QUALITY_EXPLAINED_SIGNALS, score.explainedSignals());
    }
    if (ChimericMsOption.FLAG.equals(handleChimericsOption)) {
      // default is passed
      ChimericPrecursorResults chimeric = chimericMap.getOrDefault(msmsScan,
          ChimericPrecursorResults.PASSED);
      entry.putIfNotNull(DBEntryField.QUALITY_PRECURSOR_PURITY, chimeric.purity());
      entry.putIfNotNull(DBEntryField.QUALITY_CHIMERIC, chimeric.flag());
      if (ChimericPrecursorFlag.CHIMERIC.equals(chimeric.flag())) {
        entry.putIfNotNull(DBEntryField.NAME,
            entry.getField(DBEntryField.NAME).orElse("") + " (Chimeric precursor selection)");
      }
    }
    // add file info
    int scanNumber = msmsScan.getScanNumber();
    final String fileUSI = Path.of(requireNonNullElse(msmsScan.getDataFile().getAbsolutePath(),
        msmsScan.getDataFile().getName())).getFileName().toString() + ":" + scanNumber;

    entry.putIfNotNull(DBEntryField.SCAN_NUMBER, scanNumber);
    entry.getField(DBEntryField.DATASET_ID).ifPresent(
        dataID -> entry.putIfNotNull(DBEntryField.USI, "mzspec:" + dataID + ":" + fileUSI));

    // add experimental data
    if (entry.getField(DBEntryField.RT).isEmpty()) {
      entry.putIfNotNull(DBEntryField.RT, row.getAverageRT());
    }
    if (entry.getField(DBEntryField.CCS).isEmpty()) {
      entry.putIfNotNull(DBEntryField.CCS, row.getAverageCCS());
    }
    return entry;
  }

  /**
   * @param scans might be filtered if the chimeric handling is set to SKIP
   * @return a map that flags spectra as chimeric or passed - or an empty map if handeChimerics is
   * off
   */
  private Map<Scan, ChimericPrecursorResults> handleChimericsAndFilterScansIfSelected(
      final FeatureListRow row, final List<Scan> scans) {
    if (handleChimerics) {
      var chimericMap = ChimericPrecursorChecker.checkChimericPrecursorIsolation(row.getAverageMZ(),
          scans, chimericsMainIonMzTol, chimericsIsolationMzTol, minimumPrecursorPurity);
      if (ChimericMsOption.SKIP.equals(handleChimericsOption)) {
        scans.removeIf(scan -> ChimericPrecursorFlag.PASSED != chimericMap.get(scan).flag());
      }
      return chimericMap;
    }
    return Map.of();
  }

  private void exportEntry(final BufferedWriter writer, final SpectralLibraryEntry entry)
      throws IOException {
    String stringEntry = switch (format) {
      case msp -> MSPEntryGenerator.createMSPEntry(entry);
      case json -> MZmineJsonGenerator.generateJSON(entry);
      case mgf -> MGFEntryGenerator.createMGFEntry(entry);
    };
    writer.append(stringEntry).append("\n");
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

}
